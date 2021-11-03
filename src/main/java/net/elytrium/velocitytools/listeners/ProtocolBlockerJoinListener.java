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
import java.util.List;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.hooks.InitialInboundConnectionHook;
import net.elytrium.velocitytools.utils.WhitelistUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ProtocolBlockerJoinListener {

  private final List<Integer> protocols;
  private final boolean whitelist;
  private final Component kickReason;

  public ProtocolBlockerJoinListener(VelocityTools plugin) {
    this.protocols = plugin.getConfig().getList("tools.protocolblocker.protocols")
        .stream()
        .map(object -> Integer.parseInt(object.toString()))
        .collect(Collectors.toList());
    this.whitelist = plugin.getConfig().getBoolean("tools.protocolblocker.whitelist");
    this.kickReason = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("tools.protocolblocker.kick-reason"));
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) {
    try {
      InitialInboundConnection inboundConnection = (InitialInboundConnection) event.getConnection();

      InitialInboundConnectionHook.get(inboundConnection).eventLoop().execute(() -> {
        if (WhitelistUtil.checkForWhitelist(this.whitelist, this.protocols.contains(event.getConnection().getProtocolVersion().getProtocol()))) {
          event.getConnection().getVirtualHost().ifPresent(conn -> inboundConnection.disconnect(this.kickReason));
        }
      });
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
