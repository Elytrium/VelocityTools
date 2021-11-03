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

package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.handlers.HostnamesManagerHandler;
import net.elytrium.velocitytools.hooks.InitialInboundConnectionHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HostnamesManagerJoinListener {

  private final Component kickReason;
  private final HostnamesManagerHandler handler;

  public HostnamesManagerJoinListener(VelocityTools plugin) {
    if (plugin.getConfig().getString("tools.hostnamesmanager.kick-reason").equals("{DISCONNECTED}")) {
      this.kickReason = Component.translatable("multiplayer.disconnect.generic");
    } else {
      this.kickReason = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("tools.hostnamesmanager.kick-reason"));
    }

    this.handler = new HostnamesManagerHandler(plugin);
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) {
    try {
      InitialInboundConnection conn = (InitialInboundConnection) event.getConnection();
      InitialInboundConnectionHook.get(conn).eventLoop().execute(() ->
          conn.getVirtualHost().ifPresent(inet -> {
            String remoteAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
            String log = event.getConnection().getRemoteAddress() + " is joining the server using: " + inet.getHostName();
            if (this.handler.checkAddress(inet.getHostName(), remoteAddress, log)) {
              conn.disconnect(this.kickReason);
            }
          })
      );
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
