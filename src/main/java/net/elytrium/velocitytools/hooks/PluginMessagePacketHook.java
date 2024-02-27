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

package net.elytrium.velocitytools.hooks;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.ConfigSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.serializer.placeholders.Placeholders;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.utils.Reflection;

class PluginMessagePacketHook extends PluginMessagePacket {

  private static final MethodHandle SERVER_CONN_GETTER0 = Reflection.findGetter(BackendPlaySessionHandler.class, "serverConn", VelocityServerConnection.class);
  private static final MethodHandle SERVER_CONN_GETTER1 = Reflection.findGetter(ConfigSessionHandler.class, "serverConn", VelocityServerConnection.class);

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (PluginMessageUtil.isMcBrand(this)) {
      try {
        if (handler instanceof BackendPlaySessionHandler) {
          ConnectedPlayer player = ((VelocityServerConnection) PluginMessagePacketHook.SERVER_CONN_GETTER0.invokeExact((BackendPlaySessionHandler) handler)).getPlayer();
          player.getConnection().write(this.rewriteMinecraftBrand(this, player.getProtocolVersion()));
          return true;
        } else if (handler instanceof ConfigSessionHandler) {
          ConnectedPlayer player = ((VelocityServerConnection) PluginMessagePacketHook.SERVER_CONN_GETTER1.invokeExact((ConfigSessionHandler) handler)).getPlayer();
          player.getConnection().write(this.rewriteMinecraftBrand(this, player.getProtocolVersion()));
          return true;
        }
      } catch (Throwable t) {
        throw new ReflectionException(t);
      }
    }

    return super.handle(handler);
  }

  private PluginMessagePacket rewriteMinecraftBrand(PluginMessagePacket message, ProtocolVersion protocolVersion) {
    String rewrittenBrand = Placeholders.replace(Settings.BRAND_CHANGER.inGameBrand, PluginMessageUtil.readBrandMessage(message.content()));
    ByteBuf rewrittenBuf = Unpooled.buffer();
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeString(rewrittenBuf, rewrittenBrand);
    } else {
      rewrittenBuf.writeCharSequence(rewrittenBrand, StandardCharsets.UTF_8);
    }

    return new PluginMessagePacket(message.getChannel(), rewrittenBuf);
  }

  static boolean enabled() {
    return Settings.BRAND_CHANGER.rewriteInGame;
  }
}
