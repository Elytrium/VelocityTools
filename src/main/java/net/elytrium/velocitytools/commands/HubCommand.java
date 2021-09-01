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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HubCommand implements SimpleCommand {

  private final VelocityTools plugin;
  private final ProxyServer server;
  private final List<String> disabledServers;

  public HubCommand(VelocityTools plugin, ProxyServer server) {
    this.plugin = plugin;
    this.server = server;

    this.disabledServers = this.plugin.getConfig().getList("commands.hub.disabled-servers")
        .stream()
        .map(object -> Objects.toString(object, null))
        .collect(Collectors.toList());
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();

    if (!(source instanceof Player)) {
      source.sendMessage(CommandMessages.PLAYERS_ONLY);
      return;
    }

    Player player = (Player) source;
    String serverName = this.plugin.getConfig().getString("commands.hub.server");

    RegisteredServer toConnect = this.server.getServer(serverName).orElse(null);

    if (toConnect == null) {
      source.sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST.args(Component.text(serverName)));
      return;
    }

    player.getCurrentServer().ifPresent(serverConnection -> {
      String servername = serverConnection.getServer().getServerInfo().getName();

      if (this.disabledServers.stream().anyMatch(servername::equals)
          && !player.hasPermission("velocitytools.command.hub.bypass." + servername)) {
        player.sendMessage(
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(this.plugin.getConfig().getString("commands.hub.disabled-server-message")));
      } else {
        player.createConnectionRequest(toConnect).connectWithIndication()
            .thenAccept(isSuccessful -> {
              if (isSuccessful && !this.plugin.getConfig().getString("commands.hub.you-got-moved").isEmpty()) {
                player.sendMessage(
                    LegacyComponentSerializer
                        .legacyAmpersand()
                        .deserialize(this.plugin.getConfig().getString("commands.hub.you-got-moved")));
              }
            });
      }
    });
  }

  @Override
  public boolean hasPermission(final SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.hub");
  }
}
