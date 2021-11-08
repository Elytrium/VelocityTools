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

package net.elytrium.velocitytools.hooks;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.handlers.HostnamesManagerHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

class HandshakeHook extends Handshake implements Hook {

  private final HostnamesManagerHandler handler = new HostnamesManagerHandler();
  private final String kickReason = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.KICK_REASON;
  private final Component kickReasonComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(this.kickReason);

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    int nextStatus = this.getNextStatus();
    String serverAddress = this.getServerAddress();

    try {
      Field connectionField = HandshakeSessionHandler.class.getDeclaredField("connection");
      connectionField.setAccessible(true);
      MinecraftConnection connection = (MinecraftConnection) connectionField.get(handler);

      if (nextStatus == StateRegistry.STATUS_ID) {
        if (this.handler.checkAddress(StateRegistry.STATUS, connection, serverAddress)) {
          connection.close();
          return true;
        }
      }

      if (nextStatus == StateRegistry.LOGIN_ID) {
        StateRegistry login = StateRegistry.LOGIN;
        if (this.handler.checkAddress(login, connection, serverAddress)) {
          ProtocolVersion protocolVersion = this.getProtocolVersion();
          if (!this.kickReason.isEmpty()) {
            connection.setState(login);
            connection.setProtocolVersion(protocolVersion);
          }
          connection.closeWith(Disconnect.create(this.kickReasonComponent, protocolVersion));
          return true;
        }
      }
    } catch (IllegalAccessException | NoSuchFieldException e) {
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
}
