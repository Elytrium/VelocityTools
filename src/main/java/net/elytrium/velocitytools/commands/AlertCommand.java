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
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Collection;
import net.elytrium.serializer.placeholders.Placeholders;
import net.elytrium.velocitytools.Settings;
import net.kyori.adventure.text.Component;

public class AlertCommand implements RawCommand {

  private final ProxyServer server;

  public AlertCommand(ProxyServer server) {
    this.server = server;
  }

  @Override
  public void execute(RawCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String args = invocation.arguments();
    if (args.isEmpty()) {
      source.sendMessage(Settings.ALERT_COMMAND.messageNeeded);
    } else {
      Collection<Player> players = this.server.getAllPlayers();
      if (players.isEmpty()) {
        source.sendMessage(Settings.ALERT_COMMAND.emptyProxy);
      } else {
        Component component = Placeholders.replace(Settings.ALERT_COMMAND.prefix, Settings.serializer().deserialize(args));
        if (!(source instanceof Player)) {
          source.sendMessage(component);
        }

        players.forEach(player -> player.sendMessage(component));
      }
    }
  }

  @Override
  public boolean hasPermission(RawCommand.Invocation invocation) {
    return invocation.source().hasPermission("velocitytools.command.alert");
  }
}
