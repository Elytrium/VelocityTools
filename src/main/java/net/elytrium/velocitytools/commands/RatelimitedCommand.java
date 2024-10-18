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
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;

public abstract class RatelimitedCommand implements SimpleCommand {

  private final Component ratelimited;

  public RatelimitedCommand() {
    this.ratelimited = VelocityTools.getSerializer().deserialize(Settings.IMP.COMMANDS.RATELIMITED);
  }

  @Override
  public final void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    if (source instanceof Player player && !VelocityTools.getRatelimiter().attempt(player.getRemoteAddress().getAddress())) {
      source.sendMessage(this.ratelimited);
      return;
    }

    this.execute(source, invocation.arguments());
  }

  protected abstract void execute(CommandSource source, String[] args);
}
