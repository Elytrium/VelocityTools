/*
 * Copyright (C) 2021 Elytrium
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
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.elytrium.velocitytools.commands.AlertCommand;
import net.elytrium.velocitytools.commands.FindCommand;
import net.elytrium.velocitytools.commands.HubCommand;
import net.elytrium.velocitytools.commands.SendCommand;
import net.elytrium.velocitytools.commands.VelocityToolsCommand;
import net.elytrium.velocitytools.hooks.PluginMessageHook;
import net.elytrium.velocitytools.listeners.BrandChangerListener;
import net.elytrium.velocitytools.listeners.HostnamesManagerJoinListener;
import net.elytrium.velocitytools.listeners.HostnamesManagerPingListener;
import net.elytrium.velocitytools.listeners.ProtocolBlockerJoinListener;
import net.elytrium.velocitytools.listeners.ProtocolBlockerPingListener;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "velocity_tools",
    name = "Velocity Tools",
    version = BuildConstants.VERSION,
    url = "https://elytrium.net",
    authors = {"mdxd44", "hevav"}
)
public class VelocityTools {

  private static VelocityTools instance;

  private final ProxyServer server;
  private final Path dataDirectory;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;

  private Toml config;

  @Inject
  public VelocityTools(ProxyServer server, @DataDirectory Path dataDirectory, Logger logger, Metrics.Factory metricsFactory) {
    setInstance(this);

    this.server = server;
    this.dataDirectory = dataDirectory;
    this.logger = logger;
    this.metricsFactory = metricsFactory;
  }

  private static void setInstance(VelocityTools thisInst) {
    instance = thisInst;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();

    PluginMessageHook.init();

    this.checkForUpdates();
    this.metricsFactory.make(this, 12708);
  }

  public static VelocityTools getInstance() {
    return instance;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public void reload() {
    try {
      if (!this.dataDirectory.toFile().exists()) {
        this.dataDirectory.toFile().mkdir();
      }

      File configFile = new File(this.dataDirectory.toFile(), "config.toml");
      if (!configFile.exists()) {
        Files.copy(Objects.requireNonNull(VelocityTools.class.getResourceAsStream("/config.toml")), configFile.toPath()
        );
      }
      this.config = new Toml().read(new File(this.dataDirectory.toFile(), "config.toml"));
    } catch (IOException e) {
      this.logger.error("Unable to load configuration!", e);
    }

    // Commands /////////////////////////
    if (this.config.getBoolean("commands.hub.enabled") && !this.config.getList("commands.hub.aliases").isEmpty()) {
      List<String> aliases = this.config.getList("commands.hub.aliases");
      this.server.getCommandManager().unregister(aliases.get(0));
      this.server.getCommandManager().register(aliases.get(0), new HubCommand(this, this.server), aliases.toArray(new String[0]));
    }

    if (this.config.getBoolean("commands.alert.enabled")) {
      this.server.getCommandManager().unregister("alert");
      this.server.getCommandManager().register("alert", new AlertCommand(this, this.server));
    }

    if (this.config.getBoolean("commands.find.enabled")) {
      this.server.getCommandManager().unregister("find");
      this.server.getCommandManager().register("find", new FindCommand(this, this.server));
    }

    if (this.config.getBoolean("commands.send.enabled")) {
      this.server.getCommandManager().unregister("send");
      this.server.getCommandManager().register("send", new SendCommand(this, this.server));
    }

    this.server.getCommandManager().unregister("velocitytools");
    this.server.getCommandManager().register("velocitytools", new VelocityToolsCommand(this), "vtools");
    ///////////////////////////////////

    // Tools /////////////////////////
    this.server.getEventManager().unregisterListeners(this);

    if (this.config.getBoolean("tools.brandchanger.enabled")) {
      this.server.getEventManager().register(this, new BrandChangerListener(this));
    }

    if (this.config.getBoolean("tools.protocolblocker.block-ping")) {
      this.server.getEventManager().register(this, new ProtocolBlockerPingListener(this));
    }

    if (this.config.getBoolean("tools.protocolblocker.block-joining")) {
      this.server.getEventManager().register(this, new ProtocolBlockerJoinListener(this));
    }

    if (this.config.getBoolean("tools.hostnamesmanager.block-ping")) {
      this.server.getEventManager().register(this, new HostnamesManagerPingListener(this));
    }

    if (this.config.getBoolean("tools.hostnamesmanager.block-joining")) {
      this.server.getEventManager().register(this, new HostnamesManagerJoinListener(this));
    }
    ///////////////////////////////////
  }

  @SuppressWarnings({"ConstantConditions", "MismatchedStringCase"})
  private void checkForUpdates() {
    if (!BuildConstants.VERSION.contains("-DEV")) {
      try {
        URL url = new URL("https://raw.githubusercontent.com/Elytrium/LimboAPI/master/VERSION");
        URLConnection conn = url.openConnection();
        int timeout = (int) TimeUnit.SECONDS.toMillis(4);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          String version = in.readLine();
          if (version != null && !version.trim().equalsIgnoreCase(BuildConstants.VERSION)) {
            this.logger.error("****************************************");
            this.logger.warn("The new VelocityTools update was found, please update.");
            this.logger.error("https://github.com/Elytrium/VelocityTools/releases/");
            this.logger.error("****************************************");
          }
        }
      } catch (IOException ex) {
        this.logger.warn("Unable to check for updates.", ex);
      }
    }
  }

  public Toml getConfig() {
    return this.config;
  }

  public ProxyServer getServer() {
    return this.server;
  }

  public Logger getLogger() {
    return this.logger;
  }
}
