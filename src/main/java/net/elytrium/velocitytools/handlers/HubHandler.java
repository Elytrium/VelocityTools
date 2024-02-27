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

package net.elytrium.velocitytools.handlers;

import java.util.concurrent.atomic.AtomicInteger;
import net.elytrium.velocitytools.Settings;

public class HubHandler {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  public static String nextServer(String currentServer) {
    int amount = Settings.HUB_COMMAND.servers.size();
    if (amount > 1) {
      int counter = HubHandler.COUNTER.getAndIncrement();
      if (counter >= amount) {
        HubHandler.COUNTER.set(1);
        counter = 0;
      }

      String next = Settings.HUB_COMMAND.servers.get(counter);
      if (currentServer != null && currentServer.equalsIgnoreCase(next)) {
        if (++counter >= amount) {
          counter = 0;
        }

        return Settings.HUB_COMMAND.servers.get(counter);
      }

      return next;
    } else {
      return Settings.HUB_COMMAND.servers.get(0);
    }
  }
}
