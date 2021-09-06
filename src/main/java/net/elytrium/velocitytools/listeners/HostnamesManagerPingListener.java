package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;

public class HostnamesManagerPingListener {

  private static Field mcConnectionField;

  private final boolean debug;
  private final boolean showBlocked;
  private final boolean whitelist;
  private final List<String> whitelistedIps;
  private final List<String> hostnames;

  public HostnamesManagerPingListener(VelocityTools plugin) {
    this.debug = plugin.getConfig().getBoolean("tools.hostnamesmanager.debug");
    this.showBlocked = plugin.getConfig().getBoolean("tools.hostnamesmanager.show-blocked");
    this.whitelist = plugin.getConfig().getBoolean("tools.hostnamesmanager.whitelist");
    this.hostnames = plugin.getConfig().getList("tools.hostnamesmanager.hostnames")
        .stream()
        .map(object -> Objects.toString(object, null))
        .collect(Collectors.toList());
    this.whitelistedIps = plugin.getConfig().getList("tools.hostnamesmanager.ignored-ips")
        .stream()
        .map(object -> Objects.toString(object, null))
        .collect(Collectors.toList());
  }

  static {
    try {
      mcConnectionField = InitialInboundConnection.class.getDeclaredField("connection");
      mcConnectionField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  @Subscribe
  public void onPing(ProxyPingEvent event) {
    MinecraftConnection connection;
    try {
      connection = (MinecraftConnection) mcConnectionField.get(event.getConnection());
    } catch (IllegalArgumentException | IllegalAccessException ignored) {
      return;
    }
    String remoteAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

    event.getConnection().getVirtualHost().ifPresent(inet -> {
      String log = event.getConnection().getRemoteAddress() + " is pinging the server using: " + inet.getHostName();
      if (this.whitelist) {
        if (!this.hostnames.contains(inet.getHostName()) && !this.whitelistedIps.contains(remoteAddress)) {
          connection.close();
          log += " §c(blocked)";
        }
      } else {
        if (this.hostnames.contains(inet.getHostName()) && !this.whitelistedIps.contains(remoteAddress)) {
          connection.close();
          log += " §c(blocked)";
        }
      }
      if (this.debug) {
        if (!this.showBlocked && log.endsWith(" §c(blocked)")) {
          return;
        }
        VelocityTools.getInstance().getLogger().info(log);
      }
    });
  }
}
