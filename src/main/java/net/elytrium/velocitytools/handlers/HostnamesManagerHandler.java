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

package net.elytrium.velocitytools.handlers;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.elytrium.velocitytools.VelocityTools;
import net.elytrium.velocitytools.utils.WhitelistUtil;

public class HostnamesManagerHandler {

  private final boolean debug;
  private final boolean showBlockedOnly;
  private final boolean whitelist;
  private final boolean blockLocal;
  private final List<Pattern> hostnames;
  private final List<Pattern> whitelistedIps;

  public HostnamesManagerHandler(VelocityTools plugin) {
    this.debug = plugin.getConfig().getBoolean("tools.hostnamesmanager.debug");
    this.showBlockedOnly = plugin.getConfig().getBoolean("tools.hostnamesmanager.show-blocked-only");
    this.whitelist = plugin.getConfig().getBoolean("tools.hostnamesmanager.whitelist");
    this.blockLocal = plugin.getConfig().getBoolean("tools.hostnamesmanager.block-local-addresses");
    this.hostnames = plugin.getConfig().getList("tools.hostnamesmanager.hostnames")
        .stream()
        .map(object -> Pattern.compile("^" + object.toString().replace(".", "\\.").replace("*", ".*") + "$"))
        .collect(Collectors.toList());
    this.whitelistedIps = plugin.getConfig().getList("tools.hostnamesmanager.ignored-ips")
        .stream()
        .map(object -> Pattern.compile("^" + object.toString().replace(".", "\\.").replace("*", ".*") + "$"))
        .collect(Collectors.toList());
  }

  public boolean checkAddress(String addr, String remoteAddr, String log) {
    if (WhitelistUtil.checkForWhitelist(this.whitelist,
        (this.blockLocal && (addr.startsWith("127.") || addr.equalsIgnoreCase("localhost")))
        || this.hostnames.stream().anyMatch(pattern -> pattern.matcher(addr).matches())
    ) && this.whitelistedIps.stream().noneMatch(pattern -> pattern.matcher(remoteAddr).matches())) {
      this.debugInfo(log + " §c(blocked)", true);
      return true;
    } else {
      this.debugInfo(log, false);
      return false;
    }
  }

  private void debugInfo(String msg, boolean blocked) {
    if (this.debug) {
      if (this.showBlockedOnly && !blocked) {
        return;
      }

      VelocityTools.getInstance().getLogger().info(msg);
    }
  }
}
