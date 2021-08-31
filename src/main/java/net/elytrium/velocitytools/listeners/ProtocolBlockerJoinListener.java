package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ProtocolBlockerJoinListener {

  private final List<Integer> protocols;
  private final boolean whitelist;
  private final Component kickReason;

  public ProtocolBlockerJoinListener(VelocityTools plugin) {
    this.protocols = plugin.getConfig().getList("tools.protocolblocker.protocols")
        .stream()
        .map(object -> Integer.parseInt(Objects.toString(object, null)))
        .collect(Collectors.toList());
    this.whitelist = plugin.getConfig().getBoolean("tools.protocolblocker.whitelist");
    this.kickReason = LegacyComponentSerializer
        .legacyAmpersand()
        .deserialize(plugin.getConfig().getString("tools.protocolblocker.kick_reason"));
  }

  @Subscribe
  public void onJoin(PreLoginEvent event) {
    int playerProtocol = event.getConnection().getProtocolVersion().getProtocol();

    if (this.whitelist) {
      if (!this.protocols.contains(playerProtocol)) {
        event.getConnection().getVirtualHost().ifPresent(conn ->
            ((InitialInboundConnection) event.getConnection()).disconnect(this.kickReason));
      }
    } else {
      if (this.protocols.contains(playerProtocol)) {
        event.getConnection().getVirtualHost().ifPresent(conn ->
            ((InitialInboundConnection) event.getConnection()).disconnect(this.kickReason));
      }
    }
  }
}
