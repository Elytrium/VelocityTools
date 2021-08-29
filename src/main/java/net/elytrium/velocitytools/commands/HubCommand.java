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
import java.text.MessageFormat;
import java.util.Optional;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HubCommand implements SimpleCommand {

  private final VelocityTools plugin;
  private final ProxyServer server;

  public HubCommand(VelocityTools plugin, ProxyServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();

    if (!(source instanceof Player)) {
      source.sendMessage(
          LegacyComponentSerializer
              .legacyAmpersand()
              .deserialize(this.plugin.getConfig().getString("commands.hub.players-only")));
      return;
    }

    Player player = (Player) source;
    String serverName = this.plugin.getConfig().getString("commands.hub.server");
    Optional<RegisteredServer> toConnect = this.server.getServer(serverName);

    if (!toConnect.isPresent()) {
      player.sendMessage(
          LegacyComponentSerializer
              .legacyAmpersand()
              .deserialize(MessageFormat.format(
                  this.plugin.getConfig().getString("commands.hub.not-enough-arguments"),
                  serverName)
              ));
      return;
    }

    player.getCurrentServer().ifPresent(serverConnection -> {
      if (this.plugin.getConfig().getList("commands.hub.disabled-servers").stream().anyMatch(
          serverConnection.getServer().getServerInfo().getName()::equals)) {
        player.sendMessage(
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(this.plugin.getConfig().getString("commands.hub.disabled-server-message")));
      } else {
        player.createConnectionRequest(toConnect.get()).fireAndForget();
      }
    });
  }
}
