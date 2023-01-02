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

package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.List;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.utils.WhitelistUtil;
import net.kyori.adventure.text.Component;

public class ProtocolBlockerPingListener {

  private final boolean whitelist;
  private final List<Integer> protocols;
  private final String brand;
  private final String motd;
  private final Component motdComponent;

  public ProtocolBlockerPingListener() {
    this.whitelist = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.WHITELIST;
    this.protocols = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.PROTOCOLS;
    this.brand = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.BRAND;
    this.motd = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.MOTD;
    this.motdComponent = VelocityTools.getSerializer().deserialize(this.motd);
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
