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

import com.velocitypowered.proxy.network.Connections;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import java.lang.reflect.Method;
import net.elytrium.velocitytools.Settings;

public class ChannelInitializerHook extends ChannelInitializer<Channel> {

  private final Method initChannel;
  private final ChannelInitializer<Channel> originalInitializer;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ChannelInitializerHook(ChannelInitializer<Channel> originalInitializer) {
    try {
      this.initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
      this.initChannel.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    this.originalInitializer = originalInitializer;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
    this.initChannel.invoke(this.originalInitializer, channel);
    if (Settings.IMP.TOOLS.DISABLE_LEGACY_PING && channel.pipeline().names().contains(Connections.LEGACY_PING_DECODER)) {
      channel.pipeline().remove(Connections.LEGACY_PING_DECODER);
    }
  }
}
