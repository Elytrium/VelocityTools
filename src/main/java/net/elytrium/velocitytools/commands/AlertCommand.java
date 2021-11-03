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
import java.text.MessageFormat;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class AlertCommand implements SimpleCommand {

  private final VelocityTools plugin;
  private final ProxyServer server;

  public AlertCommand(VelocityTools plugin, ProxyServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0) {
      source.sendMessage(
          LegacyComponentSerializer.legacyAmpersand().deserialize(this.plugin.getConfig().getString("commands.alert.message-needed"))
      );
    } else {
      Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(
          MessageFormat.format(this.plugin.getConfig().getString("commands.alert.prefix"), String.join(" ", args))
      );
      if (this.server.getAllPlayers().size() == 0) {
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(this.plugin.getConfig().getString("commands.alert.empty-proxy")));
      } else if (this.server.getAllPlayers().size() >= 1) {
        if (!(source instanceof Player)) {
          source.sendMessage(component);
        }
        for (Player player : this.server.getAllPlayers()) {
          player.sendMessage(player, component);
        }
      }
    }
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.alert");
  }
}
