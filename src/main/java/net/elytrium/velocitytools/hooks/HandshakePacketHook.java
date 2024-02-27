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

package net.elytrium.velocitytools.hooks;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import java.lang.invoke.MethodHandle;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.handlers.HostnamesManagerHandler;
import net.elytrium.velocitytools.utils.Reflection;
import org.slf4j.Logger;

public class HandshakePacketHook extends HandshakePacket {

  private static final MethodHandle CONNECTION_GETTER = Reflection.findGetter(HandshakeSessionHandler.class, "connection", MinecraftConnection.class);
  private static final MethodHandle PROTOCOL_VERSION_SETTER = Reflection.findSetter(MinecraftConnection.class, "protocolVersion", ProtocolVersion.class);
  private static final PreparedPacketFactory FACTORY = new PreparedPacketFactory(PreparedPacket::new, StateRegistry.LOGIN, false, 0, 0, Settings.MAIN.saveUncompressedPackets, true);
  private static PreparedPacket DISCONNECT_PACKET;

  private final Logger logger;

  public HandshakePacketHook(Logger logger) {
    this.logger = logger;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    try {
      MinecraftConnection connection = (MinecraftConnection) HandshakePacketHook.CONNECTION_GETTER.invokeExact((HandshakeSessionHandler) handler);
      switch (this.getNextStatus()) {
        case StateRegistry.STATUS_ID -> {
          if (HostnamesManagerHandler.checkAddress(this.logger, StateRegistry.STATUS, connection, this.getServerAddress())) {
            // TODO custom motd
            connection.close();
            return true;
          }
        }
        case StateRegistry.LOGIN_ID -> {
          if (HostnamesManagerHandler.checkAddress(this.logger, StateRegistry.LOGIN, connection, this.getServerAddress())) {
            connection.getChannel().pipeline().remove(Connections.FRAME_ENCODER);
            ProtocolVersion protocolVersion = this.getProtocolVersion();
            HandshakePacketHook.PROTOCOL_VERSION_SETTER.invokeExact(connection, protocolVersion);
            connection.closeWith(HandshakePacketHook.DISCONNECT_PACKET.getPackets(protocolVersion).retain());
            return true;
          }
        }
        default -> {
          if (Settings.TOOLS.disableInvalidProtocol) {
            connection.close();
            return true;
          }
        }
      }
    } catch (Throwable t) {
      throw new ReflectionException(t);
    }

    return super.handle(handler);
  }

  public static void reload() {
    if (HandshakePacketHook.DISCONNECT_PACKET != null) {
      HandshakePacketHook.DISCONNECT_PACKET.release();
    }

    HandshakePacketHook.DISCONNECT_PACKET = HandshakePacketHook.FACTORY.createPreparedPacket(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION)
        .prepare(version -> DisconnectPacket.create(Settings.HOSTNAMES_MANAGER.kickReason, version, StateRegistry.LOGIN));
  }

  static boolean enabled() {
    return Settings.TOOLS.disableInvalidProtocol || Settings.HOSTNAMES_MANAGER.blockPing || Settings.HOSTNAMES_MANAGER.blockJoin;
  }
}
