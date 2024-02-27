/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.velocitytools;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.elytrium.commons.utils.updates.UpdatesChecker;
import net.elytrium.velocitytools.commands.AlertCommand;
import net.elytrium.velocitytools.commands.FindCommand;
import net.elytrium.velocitytools.commands.HubCommand;
import net.elytrium.velocitytools.commands.VelocityToolsCommand;
import net.elytrium.velocitytools.handlers.HostnamesManagerHandler;
import net.elytrium.velocitytools.hooks.HandshakePacketHook;
import net.elytrium.velocitytools.hooks.Hooks;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "velocity-tools",
    name = "VelocityTools",
    version = BuildConfig.VERSION,
    url = "https://elytrium.net/",
    authors = {
        "Elytrium (https://elytrium.net/)",
    }
)
public class VelocityTools {

  private final Logger logger;
  private final Path dataDirectory;
  private final ProxyServer server;
  private final Metrics.Factory metricsFactory;

  @Inject
  public VelocityTools(Logger logger, @DataDirectory Path dataDirectory, ProxyServer server, Metrics.Factory metricsFactory) {
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.server = server;
    this.metricsFactory = metricsFactory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    // Pre 1.2.0 migration.
    Path oldDataDirectory;
    Path parent;
    if (Files.notExists(this.dataDirectory) && (parent = this.dataDirectory.getParent()) != null && Files.exists(oldDataDirectory = parent.resolve("velocity_tools"))) {
      try {
        Files.move(oldDataDirectory, this.dataDirectory);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    this.reload();

    Hooks.init(this.logger, this.server);

    CommandManager commandManager = this.server.getCommandManager();
    if (Settings.HUB_COMMAND.enabled) {
      List<String> aliases = Settings.HUB_COMMAND.aliases;
      if (!aliases.isEmpty()) {
        int amount = aliases.size();
        commandManager.register(aliases.get(0), new HubCommand(this.server), amount == 1 ? new String[0] : aliases.subList(1, amount).toArray(new String[amount - 1]));
      }
    }

    if (Settings.ALERT_COMMAND.enabled) {
      commandManager.register("alert", new AlertCommand(this.server));
    }

    if (Settings.FIND_COMMAND.enabled) {
      commandManager.register("find", new FindCommand(this.server));
    }

    commandManager.register("velocitytools", new VelocityToolsCommand(this), "vtools");

    EventManager eventManager = this.server.getEventManager();
    if (Settings.BRAND_CHANGER.rewriteInPing
        || Settings.PROTOCOL_BLOCKER.blockPing || Settings.PROTOCOL_BLOCKER.blockJoin
        || (Settings.HUB_COMMAND.enabled && (Settings.HUB_COMMAND.spreadOnJoin || Settings.HUB_COMMAND.spreadOnKick))) {
      eventManager.register(this, new VelocityToolsListener(this.logger, this.server));
    }

    try {
      this.metricsFactory.make(this, 12708);
      if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/VelocityTools/master/VERSION", Settings.HEAD.version)) {
        this.logger.error("****************************************");
        this.logger.warn("The new VelocityTools update was found, please update.");
        this.logger.error("https://github.com/Elytrium/VelocityTools/releases/");
        this.logger.error("****************************************");
      }
    } catch (Exception e) {
      this.logger.error("An exception occurred during updates checking", e);
    }
  }

  public void reload() {
    Settings.HEAD.reload(this.dataDirectory.resolve("config.yml"));

    if (Settings.HOSTNAMES_MANAGER.blockJoin) {
      HandshakePacketHook.reload();
      HostnamesManagerHandler.reload();
    } else if (Settings.HOSTNAMES_MANAGER.blockPing) {
      HostnamesManagerHandler.reload();
    }
  }
}
