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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.List;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.utils.WhitelistUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ProtocolBlockerPingListener {

  private final List<Integer> protocols;
  private final boolean whitelist;
  private final String motd;
  private final Component motdComponent;
  private final String brand;

  public ProtocolBlockerPingListener(VelocityTools plugin) {
    this.protocols = plugin.getConfig().getList("tools.protocolblocker.protocols")
        .stream()
        .map(object -> Integer.parseInt((String) object))
        .collect(Collectors.toList());
    this.whitelist = plugin.getConfig().getBoolean("tools.protocolblocker.whitelist");
    this.motd = plugin.getConfig().getString("tools.protocolblocker.motd");
    this.motdComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(this.motd);
    this.brand = plugin.getConfig().getString("tools.protocolblocker.brand");
  }

  @Subscribe(order = PostOrder.LAST)
  public void onPing(ProxyPingEvent event) {
    ServerPing.Builder builder = event.getPing().asBuilder();

    int playerProtocol = event.getConnection().getProtocolVersion().getProtocol();
    if (WhitelistUtil.checkForWhitelist(this.whitelist, this.protocols.contains(playerProtocol))) {
      builder.version(new ServerPing.Version(playerProtocol + 1, this.brand));
      if (!this.motd.isEmpty()) {
        builder.description(this.motdComponent);
      }
    }

    event.setPing(builder.build());
  }
}
