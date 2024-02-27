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

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import java.lang.invoke.MethodHandle;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.utils.Reflection;
import org.slf4j.Logger;

public class HostnamesManagerHandler {

  private static final MethodHandle CLEAN_V_HOST = Reflection.findStatic(HandshakeSessionHandler.class, "cleanVhost", String.class, String.class);

  private static List<ThreadLocal<Matcher>> IP_WHITELIST;

  public static boolean checkAddress(Logger logger, StateRegistry type, MinecraftConnection connection, String initialServerAddress) throws Throwable {
    if ((type == StateRegistry.STATUS && !Settings.HOSTNAMES_MANAGER.blockPing) || (type == StateRegistry.LOGIN && !Settings.HOSTNAMES_MANAGER.blockJoin)) {
      return false;
    }

    InetSocketAddress remoteAddress = (InetSocketAddress) connection.getRemoteAddress();

    String serverAddress = (String) HostnamesManagerHandler.CLEAN_V_HOST.invokeExact(initialServerAddress);
    if (Settings.HOSTNAMES_MANAGER.blockLocalAddresses && (serverAddress.startsWith("127.") || serverAddress.equalsIgnoreCase("localhost"))
        || (Settings.HOSTNAMES_MANAGER.whitelist != Settings.HOSTNAMES_MANAGER.hostnames.stream().anyMatch(
            hostname -> Settings.HOSTNAMES_MANAGER.ignoreCase ? hostname.equalsIgnoreCase(serverAddress) : hostname.equals(serverAddress)
            ) && HostnamesManagerHandler.IP_WHITELIST.stream().noneMatch(pattern -> pattern.get().reset(remoteAddress.getAddress().getHostAddress()).matches()))) {
      if (Settings.HOSTNAMES_MANAGER.debug) {
        // Stupid checkstyle can't handle new switch statements properly
        // CHECKSTYLE.OFF: WhitespaceAround
        logger.warn(switch (type) {
          case LOGIN -> "{} is joining the server using: {} (blocked)";
          case STATUS -> "{} is pinging the server using: {} (blocked)";
          default -> throw new IllegalStateException("Unexpected value: " + type);
        }, remoteAddress, serverAddress);
        // CHECKSTYLE.ON: WhitespaceAround
      }

      return true;
    }

    if (Settings.HOSTNAMES_MANAGER.debug && !Settings.HOSTNAMES_MANAGER.showBlockedOnly) {
      // CHECKSTYLE.OFF: WhitespaceAround
      logger.info(switch (type) {
        case LOGIN -> "{} is joining the server using: {}";
        case STATUS -> "{} is pinging the server using: {}";
        default -> throw new IllegalStateException("Unexpected value: " + type);
      }, remoteAddress, serverAddress);
      // CHECKSTYLE.ON: WhitespaceAround
    }

    return false;
  }

  public static void reload() {
    HostnamesManagerHandler.IP_WHITELIST = Settings.HOSTNAMES_MANAGER.ignoredIps
        .stream()
        .map(address -> Pattern.compile("^" + address.replace(".", "\\.").replace("*", ".*") + "$"))
        .map(pattern -> ThreadLocal.withInitial(() -> pattern.matcher("")))
        .toList();
  }
}
