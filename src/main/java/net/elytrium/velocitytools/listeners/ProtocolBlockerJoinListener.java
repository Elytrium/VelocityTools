/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.utils.ProtocolUtil;
import net.elytrium.velocitytools.utils.WhitelistUtil;
import net.kyori.adventure.text.Component;

public class ProtocolBlockerJoinListener {

  private static final MethodHandle MH_delegate;

  private final boolean whitelist;
  private final List<Integer> protocolNumbers;
  private final List<ProtocolVersion> protocolVersions;
  private final ProtocolVersion minimumProtocolVersion;
  private final ProtocolVersion maximumProtocolVersion;
  private final Component kickReason;

  public ProtocolBlockerJoinListener() {
    this.whitelist = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.WHITELIST;
    this.protocolNumbers = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.PROTOCOLS;
    this.protocolVersions = Settings.IMP.TOOLS.PROTOCOL_BLOCKER.VERSIONS.stream()
        .map(ProtocolUtil::protocolVersionFromString)
        .collect(Collectors.toList());
    this.minimumProtocolVersion = ProtocolUtil.protocolVersionFromString(Settings.IMP.TOOLS.PROTOCOL_BLOCKER.MINIMUM_VERSION);
    this.maximumProtocolVersion = ProtocolUtil.protocolVersionFromString(Settings.IMP.TOOLS.PROTOCOL_BLOCKER.MAXIMUM_VERSION);
    this.kickReason = VelocityTools.getSerializer().deserialize(Settings.IMP.TOOLS.PROTOCOL_BLOCKER.KICK_REASON);
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) {
    try {
      InitialInboundConnection inbound = (InitialInboundConnection) MH_delegate.invoke(event.getConnection());

      inbound.getConnection().eventLoop().execute(() -> {
        ProtocolVersion playerProtocol = event.getConnection().getProtocolVersion();
        if (this.minimumProtocolVersion.compareTo(playerProtocol) > 0 || this.maximumProtocolVersion.compareTo(playerProtocol) < 0
            || WhitelistUtil.checkForWhitelist(this.whitelist,
            this.protocolNumbers.contains(playerProtocol.getProtocol())
                || this.protocolVersions.contains(playerProtocol))) {
          event.getConnection().getVirtualHost().ifPresent(conn -> inbound.disconnect(this.kickReason));
        }
      });
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }
  }

  static {
    try {
      MH_delegate = MethodHandles.privateLookupIn(LoginInboundConnection.class, MethodHandles.lookup())
          .findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
