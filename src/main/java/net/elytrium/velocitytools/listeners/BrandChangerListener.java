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

package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
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
    ServerPing.Builder builder = event.getPing().asBuilder();

    builder.version(
        new ServerPing.Version(
            this.plugin.getConfig().getBoolean("tools.brandchanger.show-always") ? -1 : event.getPing().getVersion().getProtocol(),
            this.plugin.getConfig().getString("tools.brandchanger.ping-brand")
        )
    );

    event.setPing(builder.build());
  }

  @Subscribe
  public void onMessage(PluginMessageEvent event) {
    if (!(event.getSource() instanceof Player)) {
      return;
    }

    if (event.getIdentifier().getId().equals("MC|Brand") || event.getIdentifier().getId().equals("minecraft:brand")) {
      ConnectedPlayer player = (ConnectedPlayer) event.getSource();
      player.getConnection().write(
          this.rewriteMinecraftBrand(event, this.plugin.getConfig().getString("tools.brandchanger.ingame-brand"), player.getProtocolVersion())
      );
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
