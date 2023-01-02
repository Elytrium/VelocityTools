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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.java.commons.mc.serialization.Serializer;
import net.elytrium.java.commons.mc.serialization.Serializers;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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

  @MonotonicNonNull
  private static Logger LOGGER;
  @MonotonicNonNull
  private static Serializer SERIALIZER;

  private final ProxyServer server;
  private final Path dataDirectory;
  private final Metrics.Factory metricsFactory;
  private final PreparedPacketFactory packetFactory;

  @Inject
  public VelocityTools(ProxyServer server, @DataDirectory Path dataDirectory, Logger logger, Metrics.Factory metricsFactory) {
    setLogger(logger);

    this.server = server;
    this.dataDirectory = dataDirectory;
    this.metricsFactory = metricsFactory;

    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));
    this.packetFactory = new PreparedPacketFactory(
        PreparedPacket::new,
        StateRegistry.LOGIN,
        false,
        1,
        1,
        Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS
    );

    try {
      Class.forName("com.velocitypowered.proxy.connection.client.LoginInboundConnection");
    } catch (ClassNotFoundException e) {
      LOGGER.error("Please update your Velocity binary to 3.1.0+ version", e);
      this.server.shutdown();
    }
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();

    HooksInitializer.init(this.server);

    if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/VelocityTools/master/VERSION", Settings.IMP.VERSION)) {
      LOGGER.error("****************************************");
      LOGGER.warn("The new VelocityTools update was found, please update.");
      LOGGER.error("https://github.com/Elytrium/VelocityTools/releases/");
      LOGGER.error("****************************************");
    }
    this.metricsFactory.make(this, 12708);
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "LEGACY_AMPERSAND can't be null in velocity.")
  public void reload() {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Settings.IMP.SERIALIZER.toUpperCase(Locale.ROOT)).getSerializer();
    if (serializer == null) {
      LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    List<String> aliases = Settings.IMP.COMMANDS.HUB.ALIASES;
    aliases.forEach(alias -> this.server.getCommandManager().unregister(alias));
    if (Settings.IMP.COMMANDS.HUB.ENABLED && !Settings.IMP.COMMANDS.HUB.ALIASES.isEmpty()) {
      this.server.getCommandManager().register(aliases.get(0), new HubCommand(this.server), aliases.toArray(new String[0]));
    }

    this.server.getCommandManager().unregister("alert");
    this.server.getCommandManager().unregister("find");
    this.server.getCommandManager().unregister("send");
    this.server.getCommandManager().unregister("velocitytools");

    if (Settings.IMP.COMMANDS.ALERT.ENABLED) {
      this.server.getCommandManager().register("alert", new AlertCommand(this.server));
    }

    if (Settings.IMP.COMMANDS.FIND.ENABLED) {
      this.server.getCommandManager().register("find", new FindCommand(this.server));
    }

    if (Settings.IMP.COMMANDS.SEND.ENABLED) {
      this.server.getCommandManager().register("send", new SendCommand(this.server));
    }

    this.server.getCommandManager().register("velocitytools", new VelocityToolsCommand(this), "vtools");

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

    HandshakeHook.reload(this.packetFactory);
  }


  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public static Logger getLogger() {
    return LOGGER;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }
}
