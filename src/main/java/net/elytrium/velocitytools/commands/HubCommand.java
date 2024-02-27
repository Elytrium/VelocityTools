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

package net.elytrium.velocitytools.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.handlers.HubHandler;
import net.kyori.adventure.text.Component;

public class HubCommand implements SimpleCommand {

  private final ProxyServer server;

  public HubCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    if (source instanceof Player player) {
      ServerConnection currentServer = player.getCurrentServer().orElse(null);
      String server = currentServer == null ? null : currentServer.getServerInfo().getName();
      if (server != null && Settings.HUB_COMMAND.disabledServers != null && Settings.HUB_COMMAND.disabledServers.contains(server) && !player.hasPermission("velocitytools.command.hub.bypass." + server)) {
        player.sendMessage(Settings.HUB_COMMAND.disabledServer);
        return;
      }

      String nextServerName = HubHandler.nextServer(server);
      RegisteredServer nextServer = this.server.getServer(nextServerName).orElse(null);
      if (nextServer == null) {
        player.sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(nextServerName)));
        return;
      }

      if (Settings.HUB_COMMAND.youGotMoved == null) {
        player.createConnectionRequest(nextServer).fireAndForget();
      } else {
        player.createConnectionRequest(nextServer).connectWithIndication().thenAccept(success -> {
          if (success) {
            player.sendMessage(Settings.HUB_COMMAND.youGotMoved);
          }
        });
      }
    } else {
      source.sendMessage(CommandMessages.PLAYERS_ONLY);
    }
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.hub");
  }
}
