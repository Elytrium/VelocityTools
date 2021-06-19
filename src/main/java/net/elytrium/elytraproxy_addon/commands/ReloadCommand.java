package net.elytrium.elytraproxy_addon.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import java.io.File;
import net.elytrium.elytraproxy_addon.Main;
import net.elytrium.elytraproxy_addon.config.Settings;

public class ReloadCommand implements SimpleCommand {

  public final Main plugin;

  public ReloadCommand(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    if (source.hasPermission("elytraproxy_addon.reload")) {
      Settings.IMP.reload(new File(plugin.dataDirectory.toFile(), "config.yml"));
    }
  }
}
