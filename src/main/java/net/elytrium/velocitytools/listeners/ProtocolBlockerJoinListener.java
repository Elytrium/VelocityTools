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
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import java.lang.reflect.Field;
import java.util.List;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.utils.WhitelistUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ProtocolBlockerJoinListener {

  private final Field delegate;
  private final boolean whitelist;
  private final List<Integer> protocols;
  private final Component kickReason;

  public ProtocolBlockerJoinListener() {
    try {
      this.delegate = LoginInboundConnection.class.getDeclaredField("delegate");
      this.delegate.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }

    this.whitelist = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.WHITELIST;
    this.protocols = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.PROTOCOLS;
    this.kickReason = LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.TOOLS.PROTOCOL_BLOCKER.KICK_REASON);
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) {
    try {
      InitialInboundConnection inbound = (InitialInboundConnection) this.delegate.get(event.getConnection());

      inbound.getConnection().eventLoop().execute(() -> {
        if (WhitelistUtil.checkForWhitelist(this.whitelist, this.protocols.contains(event.getConnection().getProtocolVersion().getProtocol()))) {
          event.getConnection().getVirtualHost().ifPresent(conn -> inbound.disconnect(this.kickReason));
        }
      });
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
