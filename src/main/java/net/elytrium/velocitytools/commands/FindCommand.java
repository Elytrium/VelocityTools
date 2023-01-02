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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;

public class FindCommand implements SimpleCommand {

  private final ProxyServer server;
  private final Component usernameNeeded;
  private final String playerOnlineAt;

  public FindCommand(ProxyServer server) {
    this.server = server;
    this.usernameNeeded = VelocityTools.getSerializer().deserialize(Settings.IMP.COMMANDS.FIND.USERNAME_NEEDED);
    this.playerOnlineAt = Settings.IMP.COMMANDS.FIND.PLAYER_ONLINE_AT;
  }

  @Override
  public List<String> suggest(SimpleCommand.Invocation invocation) {
    String[] args = invocation.arguments();

    if (args.length == 0) {
      return this.server.getAllPlayers().stream()
          .map(Player::getUsername)
          .collect(Collectors.toList());
    } else if (args.length == 1) {
      return this.server.getAllPlayers().stream()
          .map(Player::getUsername)
          .filter(name -> name.regionMatches(true, 0, args[0], 0, args[0].length()))
          .collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0) {
      source.sendMessage(this.usernameNeeded);
    } else {
      Optional<Player> player = this.server.getPlayer(args[0]);
      if (player.isPresent()) {
        Player player0 = player.get();
        Optional<ServerConnection> server = player0.getCurrentServer();
        server.ifPresent(srv ->
            source.sendMessage(
                VelocityTools.getSerializer().deserialize(
                    MessageFormat.format(this.playerOnlineAt, player0.getUsername(), srv.getServerInfo().getName())
                )
            )
        );
      } else {
        source.sendMessage(
            VelocityTools.getSerializer().deserialize(MessageFormat.format(Settings.IMP.COMMANDS.FIND.PLAYER_NOT_ONLINE, args[0]))
        );
      }
    }
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.find");
  }
}
