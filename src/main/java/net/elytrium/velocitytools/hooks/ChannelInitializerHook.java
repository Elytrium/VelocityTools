/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.elytrium.velocitytools.Settings;
import org.jetbrains.annotations.NotNull;

public class ChannelInitializerHook extends ChannelInitializer<Channel> {

  private static final MethodHandle MH_initChannel;

  private final ChannelInitializer<Channel> originalInitializer;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ChannelInitializerHook(ChannelInitializer<Channel> originalInitializer) {
    this.originalInitializer = originalInitializer;
  }

  @Override
  protected void initChannel(@NotNull Channel channel) throws Exception {
    try {
      MH_initChannel.invokeExact(this.originalInitializer, channel);
    } catch (Throwable e) {
      throw new Exception("failed to initialize channel", e);
    }
    ChannelPipeline pipeline = channel.pipeline();
    if (Settings.IMP.TOOLS.DISABLE_LEGACY_PING && pipeline.names().contains(Connections.LEGACY_PING_DECODER)) {
      pipeline.remove(Connections.LEGACY_PING_DECODER);
    }
  }

  static {
    try {
      MH_initChannel = MethodHandles.privateLookupIn(ChannelInitializer.class, MethodHandles.lookup())
          .findVirtual(ChannelInitializer.class, "initChannel", MethodType.methodType(void.class, Channel.class));
    } catch (Throwable e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
