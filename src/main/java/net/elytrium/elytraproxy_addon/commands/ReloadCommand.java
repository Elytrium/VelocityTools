package net.elytrium.elytraproxy_addon.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.elytrium.elytraproxy_addon.Main;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ReloadCommand implements SimpleCommand {

  public final Main plugin;

  public ReloadCommand(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    if (source.hasPermission("elytraproxy_addon.reload")) {
      try {
        plugin.reload();
        source.sendMessage(LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize("Вроде норм")
        );
      } catch (Exception e) {
        source.sendMessage(LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize("Пизда бляяя")
        );
        e.printStackTrace();
      }
    }
  }
}
