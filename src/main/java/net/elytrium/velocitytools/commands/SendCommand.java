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

package net.elytrium.velocitytools.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class SendCommand implements SimpleCommand {

  private final ProxyServer server;
  private final Component notEnoughArguments;
  private final String youGotSummoned;
  private final String console;
  private final String playerNotOnline;
  private final String callback;

  public SendCommand(ProxyServer server) {
    this.server = server;
    this.notEnoughArguments = LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.COMMANDS.SEND.NOT_ENOUGH_ARGUMENTS);
    this.youGotSummoned = Settings.IMP.COMMANDS.SEND.YOU_GOT_SUMMONED;
    this.console = Settings.IMP.COMMANDS.SEND.CONSOLE;
    this.playerNotOnline = Settings.IMP.COMMANDS.SEND.PLAYER_NOT_ONLINE;
    this.callback = Settings.IMP.COMMANDS.SEND.CALLBACK;
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

    Component summoned = LegacyComponentSerializer.legacyAmpersand().deserialize(
        MessageFormat.format(
            this.youGotSummoned, target.getServerInfo().getName(),
            ((source instanceof Player) ? ((Player) source).getUsername() : this.console)
        )
    );

    AtomicInteger sentPlayers = new AtomicInteger();
    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "all": {
        this.server.getAllPlayers().forEach(p ->
            p.createConnectionRequest(target).connectWithIndication()
                .thenAccept(isSuccessful -> {
                  sentPlayers.incrementAndGet();
                  if (isSuccessful && !this.youGotSummoned.isEmpty()) {
                    p.sendMessage(summoned);
                  }
                })
        );
        break;
      }
      case "current": {
        if (!(source instanceof Player)) {
          source.sendMessage(CommandMessages.PLAYERS_ONLY);
          break;
        }

        ((Player) source).getCurrentServer().ifPresent(serverConnection ->
            serverConnection.getServer().getPlayersConnected().forEach(p ->
                p.createConnectionRequest(target).connectWithIndication()
                    .thenAccept(isSuccessful -> {
                      sentPlayers.incrementAndGet();
                      if (isSuccessful && !this.youGotSummoned.isEmpty()) {
                        p.sendMessage(summoned);
                      }
                    })
            )
        );
        break;
      }
      default: {
        RegisteredServer serverTarget = this.server.getServer(args[0]).orElse(null);
        if (serverTarget != null) {
          serverTarget.getPlayersConnected().forEach(p ->
              p.createConnectionRequest(target).connectWithIndication()
                  .thenAccept(isSuccessful -> {
                    sentPlayers.incrementAndGet();
                    if (isSuccessful && !this.youGotSummoned.isEmpty()) {
                      p.sendMessage(summoned);
                    }
                  })
          );
        } else {
          Player player = this.server.getPlayer(args[0]).orElse(null);
          if (player != null) {
            player.createConnectionRequest(target).connectWithIndication()
                .thenAccept(isSuccessful -> {
                  sentPlayers.incrementAndGet();
                  if (isSuccessful && !this.youGotSummoned.isEmpty()) {
                    player.sendMessage(summoned);
                  }
                });
          } else {
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(MessageFormat.format(this.playerNotOnline, args[0])));
          }
        }
        break;
      }
    }

    source.sendMessage(
        LegacyComponentSerializer.legacyAmpersand().deserialize(MessageFormat.format(this.callback, sentPlayers, target.getServerInfo().getName()))
    );
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.send");
  }
}
