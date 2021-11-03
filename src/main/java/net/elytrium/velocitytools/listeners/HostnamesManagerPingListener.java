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
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.handlers.HostnamesManagerHandler;
import net.elytrium.velocitytools.hooks.InitialInboundConnectionHook;

public class HostnamesManagerPingListener {

  private final HostnamesManagerHandler handler;

  public HostnamesManagerPingListener(VelocityTools plugin) {
    this.handler = new HostnamesManagerHandler(plugin);
  }

  @Subscribe
  public void onPing(ProxyPingEvent event) {
    if (event.getConnection().getClass().getName().endsWith("HandshakeSessionHandler$LegacyInboundConnection")) {
      return;
    }

    try {
      MinecraftConnection connection = InitialInboundConnectionHook.get(event.getConnection());
      connection.eventLoop().execute(() ->
          event.getConnection().getVirtualHost().ifPresent(inet -> {
            String remoteAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
            String log = event.getConnection().getRemoteAddress() + " is pinging the server using: " + inet.getHostName();
            if (this.handler.checkAddress(inet.getHostName(), remoteAddress, log)) {
              connection.close();
            }
          })
      );
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
