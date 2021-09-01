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
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;

public class HostnamesManagerJoinListener {

  private static Field mcConnectionField;

  static {
    try {
      mcConnectionField = InitialInboundConnection.class.getDeclaredField("connection");
      mcConnectionField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) throws IllegalAccessException {
    MinecraftConnection connection = (MinecraftConnection) mcConnectionField.get(event.getConnection());
    InitialInboundConnection conn = (InitialInboundConnection) event.getConnection();
    if (conn.getVirtualHost().isPresent()) {
      InetSocketAddress addr = conn.getVirtualHost().get();
      if (VelocityTools.getInstance().getConfig().getBoolean("debug")) {
        System.out.println(addr.getHostName()); // hostname
        System.out.println(((InetSocketAddress) connection.getChannel().localAddress()).getAddress().getHostAddress()); // чистый цифровой айпи
        System.out.println(addr.getHostString()); // если есть hostname то его, иначе цифровой айпи
        System.out.println(((InetSocketAddress) connection.getChannel().localAddress()).getHostString()); // если есть hostname то его, иначе цифровой айпи
      }
    }
    //((InitialInboundConnection) event.getConnection()).disconnect(Component.empty());
  }
}
