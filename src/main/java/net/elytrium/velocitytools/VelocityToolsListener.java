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

package net.elytrium.velocitytools;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.velocitytools.handlers.HubHandler;
import net.elytrium.velocitytools.utils.Reflection;
import org.slf4j.Logger;

public class VelocityToolsListener {

  private static final MethodHandle DELEGATE_GETTER = Reflection.findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);
  private static final VarHandle TRY_INDEX_VARHANDLE = Reflection.findVarHandle(ConnectedPlayer.class, "tryIndex", int.class);

  private final Logger logger;
  private final ProxyServer server;

  public VelocityToolsListener(Logger logger, ProxyServer server) {
    this.logger = logger;
    this.server = server;
  }

  @Subscribe(order = PostOrder.EARLY)
  public void onPreLogin(PreLoginEvent event) {
    if (Settings.PROTOCOL_BLOCKER.blockJoin) {
      try {
        InboundConnection connection = event.getConnection();
        InitialInboundConnection inbound = (InitialInboundConnection) VelocityToolsListener.DELEGATE_GETTER.invokeExact((LoginInboundConnection) connection);
        inbound.getConnection().eventLoop().execute(() -> {
          ProtocolVersion playerProtocol = connection.getProtocolVersion();
          if (Settings.PROTOCOL_BLOCKER.minimumVersion.greaterThan(playerProtocol) || Settings.PROTOCOL_BLOCKER.maximumVersion.lessThan(playerProtocol)
              || Settings.PROTOCOL_BLOCKER.whitelist != (Settings.PROTOCOL_BLOCKER.protocols.contains(playerProtocol.getProtocol()) || Settings.PROTOCOL_BLOCKER.versions.contains(playerProtocol))) {
            inbound.disconnect(Settings.PROTOCOL_BLOCKER.kickReason);
          }
        });
      } catch (Throwable t) {
        throw new ReflectionException(t);
      }
    }
  }

  @Subscribe(order = PostOrder.LATE)
  public void onProxyPing(ProxyPingEvent event) {
    if (Settings.PROTOCOL_BLOCKER.blockPing) {
      ProtocolVersion protocolVersion = event.getConnection().getProtocolVersion();
      int playerProtocolNumber = protocolVersion.getProtocol();
      if (Settings.PROTOCOL_BLOCKER.minimumVersion.greaterThan(protocolVersion) || Settings.PROTOCOL_BLOCKER.maximumVersion.lessThan(protocolVersion)
          || Settings.PROTOCOL_BLOCKER.whitelist != (Settings.PROTOCOL_BLOCKER.protocols.contains(playerProtocolNumber) || Settings.PROTOCOL_BLOCKER.versions.contains(protocolVersion))) {
        ServerPing.Builder ping = event.getPing().asBuilder();
        ping.version(new ServerPing.Version(playerProtocolNumber + 1, Settings.PROTOCOL_BLOCKER.brand));
        if (Settings.PROTOCOL_BLOCKER.motd != null) {
          ping.description(Settings.PROTOCOL_BLOCKER.motd);
        }

        event.setPing(ping.build());
        return;
      }
    }

    if (Settings.BRAND_CHANGER.rewriteInPing) {
      ServerPing.Builder ping = event.getPing().asBuilder();
      event.setPing(ping.version(new ServerPing.Version(Settings.BRAND_CHANGER.showAlways ? -1 : ping.getVersion().getProtocol(), Settings.BRAND_CHANGER.pingBrand)).build());
    }
  }

  @Subscribe(order = PostOrder.LATE)
  public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
    if (Settings.HUB_COMMAND.enabled && Settings.HUB_COMMAND.spreadOnJoin) {
      String serverName = HubHandler.nextServer(null);
      RegisteredServer nextServer = this.server.getServer(serverName).orElse(null);
      if (nextServer == null) {
        this.logger.error("The specified server {} does not exist.", serverName);
        return;
      }

      event.setInitialServer(nextServer);
    }
  }

  @Subscribe(order = PostOrder.LATE)
  public void onKickedFromServer(KickedFromServerEvent event) {
    if (!event.kickedDuringServerConnect() && Settings.HUB_COMMAND.enabled && Settings.HUB_COMMAND.spreadOnKick) {
      String from = event.getServer().getServerInfo().getName();
      String serverName = HubHandler.nextServer(from);
      if (!from.equalsIgnoreCase(serverName)) {
        RegisteredServer nextServer = this.server.getServer(serverName).orElse(null);
        if (nextServer == null) {
          this.logger.error("The specified server {} does not exist.", serverName);
          return;
        }

        if (Settings.HUB_COMMAND.servers.size() > 1) { // TODO check kick reason
          for (String next : Settings.HUB_COMMAND.servers) {
            if (from.equalsIgnoreCase(next)) {
              int attempt = (int) VelocityToolsListener.TRY_INDEX_VARHANDLE.getAndAdd((ConnectedPlayer) event.getPlayer(), 1); // TODO figure out case when it initially equal 0 instead of 1
              if (attempt >= Settings.HUB_COMMAND.servers.size()/*TODO let the user decide the max attempts*/) {
                return;
              }

              break;
            }
          }
        }

        event.setResult(KickedFromServerEvent.RedirectPlayer.create(nextServer));
      }
    }
  }
}
