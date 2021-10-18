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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;

public class VelocityToolsCommand implements SimpleCommand {

  public final VelocityTools plugin;

  public VelocityToolsCommand(VelocityTools plugin) {
    this.plugin = plugin;
  }

  @Override
  public List<String> suggest(final SimpleCommand.Invocation invocation) {
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      return Stream.of("reload").collect(Collectors.toList());
    } else if (args.length == 1) {
      return Stream.of("reload").filter(str -> str.regionMatches(true, 0, args[0], 0, args[0].length())).collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      this.showHelp(source);
    } else if (args.length == 1) {
      if (args[0].equalsIgnoreCase("reload") && source.hasPermission("velocitytools.command.reload")) {
        try {
          this.plugin.reload();

          source.sendMessage(Component.text("§aСonfig reloaded successfully!"));
        } catch (Exception e) {
          e.printStackTrace();

          source.sendMessage(Component.text("§cAn internal error has occurred!"));
        }
      } else {
        this.showHelp(source);
      }
    }
  }

  private void showHelp(CommandSource source) {
    source.sendMessage(Component.text("§eThis server is using VelocityTools"));
    source.sendMessage(Component.text("§e(c) 2021 Elytrium"));
    source.sendMessage(Component.text("§ahttps://ely.su/github/"));
    if (source.hasPermission("velocitytools.command.reload")) {
      source.sendMessage(Component.text("§r"));
      source.sendMessage(Component.text("§fSubcommands:"));
      source.sendMessage(Component.text("    §a/vtools reload §8- §eReload config"));
    }
  }
}
