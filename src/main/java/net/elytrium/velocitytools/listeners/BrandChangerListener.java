package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import net.elytrium.velocitytools.VelocityTools;

public class BrandChangerListener {

  private final VelocityTools plugin;

  public BrandChangerListener(VelocityTools plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onPing(ProxyPingEvent event) {
    ServerPing.Builder pong = event.getPing().asBuilder();
    pong.version(
        new ServerPing.Version(
        this.plugin.getConfig().getBoolean("tools.brandchanger.show-always")
            ? -1
            : event.getPing().getVersion().getProtocol(),
        this.plugin.getConfig().getString("tools.brandchanger.ping_brand"))
    );
    event.setPing(pong.build());
  }

  @Subscribe
  public void onMessage(PluginMessageEvent event) {
    ConnectedPlayer player = (ConnectedPlayer) event.getSource();
    if (event.getIdentifier().getId().equals("MC|BRAND") || event.getIdentifier().getId().equals("minecraft:brand")) {
      player.getConnection().write(this.rewriteMinecraftBrand(
          event,
          this.plugin.getConfig().getString("tools.brandchanger.ingame_brand"),
          player.getProtocolVersion()
      ));
    }
  }

  private PluginMessage rewriteMinecraftBrand(PluginMessageEvent event, String version, ProtocolVersion protocolVersion) {
    String currentBrand = PluginMessageUtil.readBrandMessage(Unpooled.wrappedBuffer(event.getData()));
    String rewrittenBrand = MessageFormat.format(version, currentBrand);
    ByteBuf rewrittenBuf = Unpooled.buffer();
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      ProtocolUtils.writeString(rewrittenBuf, rewrittenBrand);
    } else {
      rewrittenBuf.writeCharSequence(rewrittenBrand, StandardCharsets.UTF_8);
    }

    return new PluginMessage(event.getIdentifier().getId(), rewrittenBuf);
  }
}
