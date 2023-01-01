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
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.java.commons.updates.UpdatesChecker;
import net.elytrium.velocitytools.commands.AlertCommand;
import net.elytrium.velocitytools.commands.FindCommand;
import net.elytrium.velocitytools.commands.HubCommand;
import net.elytrium.velocitytools.commands.SendCommand;
import net.elytrium.velocitytools.commands.VelocityToolsCommand;
import net.elytrium.velocitytools.hooks.HandshakeHook;
import net.elytrium.velocitytools.hooks.HooksInitializer;
import net.elytrium.velocitytools.listeners.BrandChangerPingListener;
import net.elytrium.velocitytools.listeners.ProtocolBlockerJoinListener;
import net.elytrium.velocitytools.listeners.ProtocolBlockerPingListener;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "velocity_tools",
    name = "VelocityTools",
    version = BuildConstants.VERSION,
    url = "https://elytrium.net/",
    authors = {
        "Elytrium (https://elytrium.net/)",
    }
)
public class VelocityTools {

  private static VelocityTools INSTANCE;

  private final ProxyServer server;
  private final Path dataDirectory;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final PreparedPacketFactory packetFactory;

  @Inject
  public VelocityTools(ProxyServer server, @DataDirectory Path dataDirectory, Logger logger, Metrics.Factory metricsFactory) {
    setInstance(this);

    this.server = server;
    this.dataDirectory = dataDirectory;
    this.logger = logger;
    this.metricsFactory = metricsFactory;
    this.packetFactory = new PreparedPacketFactory(PreparedPacket::new, StateRegistry.LOGIN, false, 1, 1);

    try {
      Class.forName("com.velocitypowered.proxy.connection.client.LoginInboundConnection");
    } catch (ClassNotFoundException e) {
      this.getLogger().error("Please update your Velocity binary to 3.1.x", e);
      this.server.shutdown();
    }
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();

    HooksInitializer.init(this.server);

    if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/VelocityTools/master/VERSION", Settings.IMP.VERSION)) {
      this.logger.error("****************************************");
      this.logger.warn("The new VelocityTools update was found, please update.");
      this.logger.error("https://github.com/Elytrium/VelocityTools/releases/");
      this.logger.error("****************************************");
    }
    this.metricsFactory.make(this, 12708);
  }

  public void reload() {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    // Commands /////////////////////////
    List<String> aliases = Settings.IMP.COMMANDS.HUB.ALIASES;
    aliases.forEach(alias -> this.server.getCommandManager().unregister(alias));
    if (Settings.IMP.COMMANDS.HUB.ENABLED && !Settings.IMP.COMMANDS.HUB.ALIASES.isEmpty()) {
      this.server.getCommandManager().register(aliases.get(0), new HubCommand(this.server), aliases.toArray(new String[0]));
    }

    if (Settings.IMP.COMMANDS.ALERT.ENABLED) {
      this.server.getCommandManager().unregister("alert");
      this.server.getCommandManager().register("alert", new AlertCommand(this.server));
    }

    if (Settings.IMP.COMMANDS.FIND.ENABLED) {
      this.server.getCommandManager().unregister("find");
      this.server.getCommandManager().register("find", new FindCommand(this.server));
    }

    if (Settings.IMP.COMMANDS.SEND.ENABLED) {
      this.server.getCommandManager().unregister("send");
      this.server.getCommandManager().register("send", new SendCommand(this.server));
    }

    this.server.getCommandManager().unregister("velocitytools");
    this.server.getCommandManager().register("velocitytools", new VelocityToolsCommand(this), "vtools");
    ///////////////////////////////////

    // Tools /////////////////////////
    this.server.getEventManager().unregisterListeners(this);

    if (Settings.IMP.TOOLS.BRAND_CHANGER.REWRITE_IN_PING) {
      this.server.getEventManager().register(this, new BrandChangerPingListener());
    }

    if (Settings.IMP.TOOLS.PROTOCOL_BLOCKER.BLOCK_PING) {
      this.server.getEventManager().register(this, new ProtocolBlockerPingListener());
    }

    if (Settings.IMP.TOOLS.PROTOCOL_BLOCKER.BLOCK_JOIN) {
      this.server.getEventManager().register(this, new ProtocolBlockerJoinListener());
    }
    ///////////////////////////////////

    HandshakeHook.reload(this.packetFactory);
  }

  private static void setInstance(VelocityTools instance) {
    VelocityTools.INSTANCE = instance;
  }

  public static VelocityTools getInstance() {
    return INSTANCE;
  }

  public Logger getLogger() {
    return this.logger;
  }
}
