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
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.velocitytools.utils.Reflection;
import org.slf4j.Logger;

public class Hooks {

  @SuppressWarnings("unchecked")
  public static void init(Logger logger, ProxyServer server) {
    if (ChannelInitializerHook.enabled()) {
      try {
        ConnectionManager connectionManager = (ConnectionManager) Reflection.findGetter(VelocityServer.class, "cm", ConnectionManager.class).invokeExact((VelocityServer) server);
        ServerChannelInitializerHolder holder = connectionManager.getServerChannelInitializer();
        VarHandle initializer = Reflection.findVarHandle(ServerChannelInitializerHolder.class, "initializer", ChannelInitializer.class);
        initializer.set(holder, new ChannelInitializerHook((ChannelInitializer<Channel>) initializer.get(holder)));
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    boolean pluginMessageHookEnabled = PluginMessagePacketHook.enabled();
    boolean handshakeHookEnabled = HandshakePacketHook.enabled();
    if (pluginMessageHookEnabled || handshakeHookEnabled) {
      try {
        MethodHandle packetIdToSupplierGetter = Reflection.findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier", IntObjectMap.class);
        MethodHandle packetClassToIdGetter = Reflection.findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId", Object2IntMap.class);
        BiConsumer<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> processor = (version, registry) -> {
          try {
            var packetIdToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplierGetter.invokeExact(registry);
            var packetClassToId = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToIdGetter.invokeExact(registry);
            BiConsumer<Class<? extends MinecraftPacket>, Supplier<? extends MinecraftPacket>> apply = (clazz, supplier) -> {
              int packetId = packetClassToId.getInt(clazz.getSuperclass());
              if (packetId != Integer.MIN_VALUE) {
                packetClassToId.put(clazz, packetId);
                packetIdToSupplier.put(packetId, supplier);
              }
            };

            if (pluginMessageHookEnabled) {
              apply.accept(PluginMessagePacketHook.class, PluginMessagePacketHook::new);
            }

            if (handshakeHookEnabled) {
              apply.accept(HandshakePacketHook.class, () -> new HandshakePacketHook(logger));
            }
          } catch (Throwable t) {
            throw new ReflectionException(t);
          }
        };

        MethodHandle versions = Reflection.findGetter(StateRegistry.PacketRegistry.class, "versions", Map.class);
        MethodHandle serverbound = Reflection.findGetter(StateRegistry.class, "serverbound", StateRegistry.PacketRegistry.class);
        MethodHandle clientbound = Reflection.findGetter(StateRegistry.class, "clientbound", StateRegistry.PacketRegistry.class);
        ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versions.invokeExact((StateRegistry.PacketRegistry) serverbound.invokeExact(StateRegistry.HANDSHAKE))).forEach(processor);
        ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versions.invokeExact((StateRegistry.PacketRegistry) clientbound.invokeExact(StateRegistry.CONFIG))).forEach(processor);
        ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versions.invokeExact((StateRegistry.PacketRegistry) clientbound.invokeExact(StateRegistry.PLAY))).forEach(processor);
      } catch (Throwable t) {
        throw new ReflectionException(t);
      }
    }
  }
}
