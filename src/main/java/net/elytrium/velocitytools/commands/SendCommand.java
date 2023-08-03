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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;

public class SendCommand implements SimpleCommand {

  private final ProxyServer server;
  private final Component notEnoughArguments;
  private final String youGotSummoned;
  private final String console;
  private final String playerNotOnline;
  private final String callback;
  private final Component notOnServer;

  public SendCommand(ProxyServer server) {
    this.server = server;
    this.notEnoughArguments = VelocityTools.getSerializer().deserialize(Settings.IMP.COMMANDS.SEND.NOT_ENOUGH_ARGUMENTS);
    this.youGotSummoned = Settings.IMP.COMMANDS.SEND.YOU_GOT_SUMMONED;
    this.console = Settings.IMP.COMMANDS.SEND.CONSOLE;
    this.playerNotOnline = Settings.IMP.COMMANDS.SEND.PLAYER_NOT_ONLINE;
    this.callback = Settings.IMP.COMMANDS.SEND.CALLBACK;
    this.notOnServer = VelocityTools.getSerializer().deserialize(Settings.IMP.COMMANDS.SEND.NOT_ON_SERVER);
  }

  @Override
  public List<String> suggest(SimpleCommand.Invocation invocation) {
    String[] args = invocation.arguments();

    if (args.length < 2) {
      List<String> players = this.server.getAllPlayers().stream()
          .map(Player::getUsername)
          .collect(Collectors.toList());

      List<String> serversNames = this.server.getAllServers().stream()
          .map(RegisteredServer::getServerInfo)
          .map(ServerInfo::getName)
          .collect(Collectors.toList());
      serversNames.add("ALL");
      serversNames.add("CURRENT");

      players.addAll(serversNames);

      if (args.length == 0) {
        return players;
      } else {
        return players.stream()
            .filter(name -> name.regionMatches(true, 0, args[0], 0, args[0].length()))
            .collect(Collectors.toList());
      }
    } else if (args.length == 2) {
      return this.server.getAllServers().stream()
          .map(RegisteredServer::getServerInfo)
          .map(ServerInfo::getName)
          .filter(name -> name.regionMatches(true, 0, args[1], 0, args[1].length()))
          .collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length != 2) {
      source.sendMessage(this.notEnoughArguments);
      return;
    }

    RegisteredServer target = this.server.getServer(args[1]).orElse(null);
    if (target == null) {
      source.sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST.args(Component.text(args[1])));
      return;
    }

    Component summoned = VelocityTools.getSerializer().deserialize(MessageFormat.format(
        this.youGotSummoned,
        target.getServerInfo().getName(),
        ((source instanceof Player) ? ((Player) source).getUsername() : this.console)
    ));

    int playersToProccess;
    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "all" -> {
        Collection<Player> players = this.server.getAllPlayers();
        playersToProccess = players.size();
        players.forEach(player -> player.createConnectionRequest(target).connectWithIndication().thenAccept(isSuccessful -> {
          if (isSuccessful && !this.youGotSummoned.isEmpty()) {
            player.sendMessage(summoned);
          }
        }));
      }
      case "current" -> {
        if (source instanceof Player player) {
          Optional<ServerConnection> currentServer = player.getCurrentServer();
          if (currentServer.isPresent()) {
            Collection<Player> players = currentServer.get().getServer().getPlayersConnected();
            playersToProccess = players.size();
            players.forEach(next -> next.createConnectionRequest(target).connectWithIndication().thenAccept(isSuccessful -> {
              if (isSuccessful && !this.youGotSummoned.isEmpty()) {
                next.sendMessage(summoned);
              }
            }));
          } else {
            source.sendMessage(this.notOnServer);
            return;
          }
        } else {
          source.sendMessage(CommandMessages.PLAYERS_ONLY);
          return;
        }
      }
      default -> {
        RegisteredServer server = this.server.getServer(args[0]).orElse(null);
        if (server == null) {
          Player player = this.server.getPlayer(args[0]).orElse(null);
          if (player == null) {
            source.sendMessage(VelocityTools.getSerializer().deserialize(MessageFormat.format(this.playerNotOnline, args[0])));
            return;
          } else {
            playersToProccess = 1;
            player.createConnectionRequest(target).connectWithIndication().thenAccept(isSuccessful -> {
              if (isSuccessful && !this.youGotSummoned.isEmpty()) {
                player.sendMessage(summoned);
              }
            });
          }
        } else {
          Collection<Player> players = server.getPlayersConnected();
          playersToProccess = players.size();
          players.forEach(player -> player.createConnectionRequest(target).connectWithIndication().thenAccept(isSuccessful -> {
            if (isSuccessful && !this.youGotSummoned.isEmpty()) {
              player.sendMessage(summoned);
            }
          }));
        }
      }
    }

    source.sendMessage(VelocityTools.getSerializer().deserialize(MessageFormat.format(
        this.callback,
        playersToProccess,
        target.getServerInfo().getName()
    )));
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.send");
  }
}
