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
import java.util.Objects;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HostnamesManagerJoinListener {

  private final boolean debug;
  private final boolean showBlocked;
  private final boolean whitelist;
  private final List<String> whitelistedIps;
  private final List<String> hostnames;
  private final Component kickReason;

  public HostnamesManagerJoinListener(VelocityTools plugin) {
    this.debug = plugin.getConfig().getBoolean("tools.hostnamesmanager.debug");
    this.showBlocked = plugin.getConfig().getBoolean("tools.hostnamesmanager.show-blocked");
    this.whitelist = plugin.getConfig().getBoolean("tools.hostnamesmanager.whitelist");
    this.hostnames = plugin.getConfig().getList("tools.hostnamesmanager.hostnames")
        .stream()
        .map(object -> Objects.toString(object, null))
        .collect(Collectors.toList());
    this.whitelistedIps = plugin.getConfig().getList("tools.hostnamesmanager.ignored-ips")
        .stream()
        .map(object -> Objects.toString(object, null))
        .collect(Collectors.toList());
    if (plugin.getConfig().getString("tools.hostnamesmanager.kick-reason").equals("{DISCONNECTED}")) {
      this.kickReason = Component.translatable("multiplayer.disconnect.generic");
    } else {
      this.kickReason = LegacyComponentSerializer
          .legacyAmpersand()
          .deserialize(plugin.getConfig().getString("tools.hostnamesmanager.kick-reason"));
    }
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) {
    InitialInboundConnection conn = (InitialInboundConnection) event.getConnection();
    String remoteAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

    conn.getVirtualHost().ifPresent(inet -> {
      String log = event.getConnection().getRemoteAddress() + " is joining the server using: " + inet.getHostName();
      if (this.whitelist) {
        if (!this.hostnames.contains(inet.getHostName()) && !this.whitelistedIps.contains(remoteAddress)) {
          conn.disconnect(this.kickReason);
          log += " §c(blocked)";
        }
      } else {
        if (this.hostnames.contains(inet.getHostName()) && !this.whitelistedIps.contains(remoteAddress)) {
          conn.disconnect(this.kickReason);
          log += " §c(blocked)";
        }
      }
      if (this.debug) {
        if (!this.showBlocked && log.endsWith(" §c(blocked)")) {
          return;
        }
        VelocityTools.getInstance().getLogger().info(log);
      }
    });
  }
}
