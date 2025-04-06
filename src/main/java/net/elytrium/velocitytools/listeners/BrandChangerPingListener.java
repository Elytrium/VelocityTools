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

package net.elytrium.velocitytools.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.elytrium.velocitytools.Settings;

public class BrandChangerPingListener {

  private final boolean showAlways;
  private final String pingBrand;

  public BrandChangerPingListener() {
    this.showAlways = Settings.IMP.TOOLS.BRAND_CHANGER.SHOW_ALWAYS;
    this.pingBrand = Settings.IMP.TOOLS.BRAND_CHANGER.PING_BRAND;
  }

  @Subscribe
  public void onPing(ProxyPingEvent event) {
    ServerPing.Builder builder = event.getPing().asBuilder();

    builder.version(new ServerPing.Version(this.showAlways ? -1 : event.getPing().getVersion().getProtocol(), this.pingBrand));

    event.setPing(builder.build());
  }
}
