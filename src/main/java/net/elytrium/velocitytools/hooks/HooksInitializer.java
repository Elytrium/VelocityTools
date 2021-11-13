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

package net.elytrium.velocitytools.hooks;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author hevav
 */
public class HooksInitializer {

  @SuppressWarnings("unchecked")
  public static void init(ProxyServer server) {
    try {
      Field cm = server.getClass().getDeclaredField("cm");
      cm.setAccessible(true);

      ServerChannelInitializerHolder serverChannelInitializer = ((ConnectionManager) cm.get(server)).getServerChannelInitializer();
      Field initializerField = serverChannelInitializer.getClass().getDeclaredField("initializer");
      initializerField.setAccessible(true);
      ChannelInitializer<Channel> initializer = (ChannelInitializer<Channel>) initializerField.get(serverChannelInitializer);
      initializerField.set(serverChannelInitializer, new ChannelInitializerHook(initializer));

      Field serverConnField = BackendPlaySessionHandler.class.getDeclaredField("serverConn");
      serverConnField.setAccessible(true);
      PluginMessageHook.serverConnField = serverConnField;

      Field versionsField = StateRegistry.PacketRegistry.class.getDeclaredField("versions");
      versionsField.setAccessible(true);

      Field packetIdToSupplierField = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetIdToSupplier");
      packetIdToSupplierField.setAccessible(true);

      Field packetClassToIdField = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId");
      packetClassToIdField.setAccessible(true);

      List<PacketHook> hooks = new ArrayList<>();
      hooks.add(new PluginMessageHook());
      hooks.add(new HandshakeHook());

      BiConsumer<? super ProtocolVersion, ? super StateRegistry.PacketRegistry.ProtocolRegistry> consumer = (version, registry) -> {
        try {
          IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier
              = (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplierField.get(registry);

          Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId
              = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToIdField.get(registry);

          hooks.forEach(hook -> {
            int packetId = packetClassToId.getInt(hook.getType());
            packetClassToId.put(hook.getHookClass(), packetId);
            packetIdToSupplier.remove(packetId);
            packetIdToSupplier.put(packetId, hook.getHook());
          });
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      };

      ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.get(StateRegistry.PLAY.clientbound)).forEach(consumer);
      ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.get(StateRegistry.HANDSHAKE.serverbound)).forEach(consumer);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
