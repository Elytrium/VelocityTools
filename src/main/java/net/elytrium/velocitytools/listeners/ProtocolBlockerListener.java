package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.List;
import net.elytrium.velocitytools.VelocityTools;

public class ProtocolBlockerListener {

  private final VelocityTools plugin;

  public ProtocolBlockerListener(VelocityTools plugin) {
   this.plugin = plugin;
  }

  @Subscribe
  public void onPing(ProxyPingEvent event) {
    List<Integer> protocols = this.plugin.getConfig().getList("tools.protocolblocker.protocols");
    ServerPing.Builder pong = event.getPing().asBuilder();

    if (this.plugin.getConfig().getBoolean("tools.protocolblocker.block_ping") && !this.plugin.getConfig().getBoolean("tools.brandchanger.show-always")) {
      System.out.println("эйоу");
      if (protocols.contains(event.getConnection().getProtocolVersion().getProtocol()) && !this.plugin.getConfig().getBoolean("tools.protocolblocker.whitelist")) {
        System.out.println("прив");
        pong.version(
            new ServerPing.Version(
                -1,
                this.plugin.getConfig().getString("tools.protocolblocker.brand"))
        );
      } else {
        System.out.println("че каво бро");
        pong.version(
            new ServerPing.Version(
                -1,
                this.plugin.getConfig().getString("tools.protocolblocker.brand"))
        );
      }
      event.setPing(pong.build());
    }
  }
}
