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
import java.util.List;
import net.elytrium.serializer.placeholders.Placeholders;
import net.elytrium.velocitytools.Settings;

public class FindCommand implements SimpleCommand {

  private final ProxyServer server;

  public FindCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public List<String> suggest(SimpleCommand.Invocation invocation) {
    String[] args = invocation.arguments();
    return args.length == 0 ? this.server.getAllPlayers().stream().map(Player::getUsername).toList()
        : args.length == 1 ? this.server.getAllPlayers().stream().map(Player::getUsername).filter(name -> name.regionMatches(true, 0, args[0], 0, args[0].length())).toList()
        : List.of();
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();
    if (args.length == 0) {
      source.sendMessage(Settings.FIND_COMMAND.usernameNeeded);
    } else {
      Player player = this.server.getPlayer(args[0]).orElse(null);
      ServerConnection currentServer;
      source.sendMessage(player == null ? Placeholders.replace(Settings.FIND_COMMAND.playerNotOnline, args[0])
          : (currentServer = player.getCurrentServer().orElse(null)) == null ? Placeholders.replace(Settings.FIND_COMMAND.playerNotOnServer, player.getUsername())
          : Placeholders.replace(Settings.FIND_COMMAND.playerOnlineAt, player.getUsername(), currentServer.getServerInfo().getName())
      );
    }
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.find");
  }
}
