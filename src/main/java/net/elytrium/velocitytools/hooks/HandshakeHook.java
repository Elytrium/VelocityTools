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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.handlers.HostnamesManagerHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HandshakeHook extends Handshake implements PacketHook {

  private static Method GET_STATE_FOR_PROTOCOL;
  private static Field CONNECTION_FIELD;
  private static HostnamesManagerHandler HOSTNAMES_HANDLER;
  private static PreparedPacket DISCONNECT_PACKET;
  private static boolean DISABLE_INVALID_PROTOCOL;

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    try {
      int nextStatus = this.getNextStatus();
      if (DISABLE_INVALID_PROTOCOL && GET_STATE_FOR_PROTOCOL.invoke(handler, nextStatus) == null) {
        return true;
      }

      MinecraftConnection connection = (MinecraftConnection) CONNECTION_FIELD.get(handler);
      if (nextStatus == StateRegistry.STATUS_ID) {
        if (HOSTNAMES_HANDLER.checkAddress(StateRegistry.STATUS, connection, handler, this.getServerAddress())) {
          connection.close();
          return true;
        }
      } else if (nextStatus == StateRegistry.LOGIN_ID) {
        StateRegistry login = StateRegistry.LOGIN;
        if (HOSTNAMES_HANDLER.checkAddress(login, connection, handler, this.getServerAddress())) {
          connection.getChannel().pipeline().remove(Connections.FRAME_ENCODER);
          connection.closeWith(DISCONNECT_PACKET.getPackets(this.getProtocolVersion()));
          return true;
        }
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    return super.handle(handler);
  }

  @Override
  public Supplier<MinecraftPacket> getHook() {
    return HandshakeHook::new;
  }

  @Override
  public Class<? extends MinecraftPacket> getType() {
    return Handshake.class;
  }

  @Override
  public Class<? extends MinecraftPacket> getHookClass() {
    return this.getClass();
  }

  public static void reload(PreparedPacketFactory factory) {
    try {
      GET_STATE_FOR_PROTOCOL = HandshakeSessionHandler.class.getDeclaredMethod("getStateForProtocol", int.class);
      GET_STATE_FOR_PROTOCOL.setAccessible(true);

      CONNECTION_FIELD = HandshakeSessionHandler.class.getDeclaredField("connection");
      CONNECTION_FIELD.setAccessible(true);

      HOSTNAMES_HANDLER = new HostnamesManagerHandler();

      String kickReason = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.KICK_REASON;
      Component kickReasonComponent;
      if (kickReason.isEmpty()) {
        kickReasonComponent = Component.empty();
      } else {
        kickReasonComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(kickReason);
      }

      DISCONNECT_PACKET = factory
          .createPreparedPacket(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION)
          .prepare(version -> Disconnect.create(kickReasonComponent, version));

      DISABLE_INVALID_PROTOCOL = Settings.IMP.TOOLS.DISABLE_INVALID_PROTOCOL;
    } catch (NoSuchMethodException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }
}
