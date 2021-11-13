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

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import java.lang.reflect.Field;
import net.elytrium.velocitytools.VelocityTools;

// TODO: Remove class after velocity release.
public class InitialInboundConnectionHook {

  private static Field mcConnectionField;

  public static MinecraftConnection get(InboundConnection connection) throws IllegalAccessException {
    if (VelocityTools.getInstance().isVelocityOld()) {
      return (MinecraftConnection) mcConnectionField.get(connection);
    } else {
      return ((InitialInboundConnection) connection).getConnection();
    }
  }

  private static void init() {
    try {
      mcConnectionField = InitialInboundConnection.class.getDeclaredField("connection");
      mcConnectionField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  static {
    init();
  }
}
