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
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.velocitytools.Settings;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.utils.WhitelistUtil;

public class HostnamesManagerHandler {


  private static Method cleanVhost;
  private final boolean ignoreCase;
  private final boolean blockPing;
  private final boolean blockJoin;
  private final boolean blockLocal;
  private final boolean whitelist;
  private final List<Pattern> hostnames;
  private final List<Pattern> whitelistedIps;
  private final boolean debug;
  private final boolean showBlockedOnly;

  public HostnamesManagerHandler() {
    this.ignoreCase = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.IGNORE_CASE;
    this.blockPing = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.BLOCK_PING;
    this.blockJoin = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.BLOCK_JOIN;
    this.blockLocal = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.BLOCK_LOCAL_ADDRESSES;
    this.whitelist = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.WHITELIST;
    this.hostnames = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.HOSTNAMES
        .stream()
        .map(object -> Pattern.compile("^" + object.replace(".", "\\.").replace("*", ".*") + "$"))
        .collect(Collectors.toList());
    this.whitelistedIps = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.IGNORED_IPS
        .stream()
        .map(object -> Pattern.compile("^" + object.replace(".", "\\.").replace("*", ".*") + "$"))
        .collect(Collectors.toList());
    this.debug = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.DEBUG;
    this.showBlockedOnly = Settings.IMP.TOOLS.HOSTNAMES_MANAGER.SHOW_BLOCKED_ONLY;
  }

  public boolean checkAddress(StateRegistry type, MinecraftConnection connection, MinecraftSessionHandler handler,
                              String initialServerAddress) throws IllegalAccessException, InvocationTargetException {
    String log;

    switch (type) {
      case STATUS: {
        if (this.blockPing) {
          log = "{} is pinging the server using: {}";
        } else {
          return false;
        }
        break;
      }
      case LOGIN: {
        if (this.blockJoin) {
          log = "{} is joining the server using: {}";
        } else {
          return false;
        }
        break;
      }
      default: {
        throw new IllegalStateException("Unexpected value: " + type);
      }
    }

    InetSocketAddress remoteAddress = (InetSocketAddress) connection.getRemoteAddress();

    String originalServerAddress = (String) cleanVhost.invoke(handler, initialServerAddress);
    String serverAddress;
    if (this.ignoreCase) {
      serverAddress = originalServerAddress.toLowerCase(Locale.ROOT);
    } else {
      serverAddress = originalServerAddress;
    }

    if ((this.blockLocal && (originalServerAddress.startsWith("127.") || originalServerAddress.equalsIgnoreCase("localhost")))
        || (WhitelistUtil.checkForWhitelist(this.whitelist, this.hostnames.stream().anyMatch(pattern -> pattern.matcher(serverAddress).matches()))
        && this.whitelistedIps.stream().noneMatch(pattern -> pattern.matcher(remoteAddress.getAddress().getHostAddress()).matches()))) {
      this.debugInfo(log + " §c(blocked)", remoteAddress, originalServerAddress, true);
      return true;
    } else {
      this.debugInfo(log, remoteAddress, originalServerAddress, false);
      return false;
    }
  }

  private void debugInfo(String msg, InetSocketAddress remoteAddress, String hostname, boolean blocked) {
    if (this.debug) {
      if (this.showBlockedOnly && !blocked) {
        return;
      }

      VelocityTools.getLogger().info(msg, remoteAddress, hostname);
    }
  }

  private static void init() {
    try {
      cleanVhost = HandshakeSessionHandler.class.getDeclaredMethod("cleanVhost", String.class);
      cleanVhost.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new ReflectionException(e);
    }
  }

  static {
    init();
  }
}
