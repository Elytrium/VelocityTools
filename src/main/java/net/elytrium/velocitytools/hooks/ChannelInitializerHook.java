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

import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import java.lang.invoke.MethodHandle;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.utils.Reflection;

public class ChannelInitializerHook extends ChannelInitializer<Channel> {

  private static final MethodHandle INIT_CHANNEL_METHOD = Reflection.findVirtualVoid(ChannelInitializer.class, "initChannel", Channel.class);

  private final ChannelInitializer<Channel> originalInitializer;

  public ChannelInitializerHook(ChannelInitializer<Channel> originalInitializer) {
    this.originalInitializer = originalInitializer;
  }

  @Override
  protected void initChannel(Channel channel) {
    try {
      ChannelInitializerHook.INIT_CHANNEL_METHOD.invokeExact(this.originalInitializer, channel);
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }

    ChannelPipeline pipeline = channel.pipeline();
    if (pipeline.names().contains(Connections.LEGACY_PING_DECODER)) {
      pipeline.remove(Connections.LEGACY_PING_DECODER); // TODO custom decoder that will instantly close the connection on legacy ping or answer
    }
  }

  static boolean enabled() {
    return Settings.TOOLS.disableLegacyPing;
  }
}
