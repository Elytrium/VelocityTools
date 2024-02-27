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

package net.elytrium.velocitytools.serialization.serializers;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.serializer.custom.ClassSerializer;

public class ProtocolVersionSerializer extends ClassSerializer<ProtocolVersion, String> {

  @Override
  public String serialize(ProtocolVersion from) {
    if (from == null || from == ProtocolVersion.MAXIMUM_VERSION) {
      return "LATEST";
    }

    String versionIntroducedIn = from.getVersionIntroducedIn();
    String mostRecentSupportedVersion = from.getMostRecentSupportedVersion();
    return versionIntroducedIn.equals(mostRecentSupportedVersion) ? versionIntroducedIn : versionIntroducedIn + '-' + mostRecentSupportedVersion;
  }

  @Override
  public ProtocolVersion deserialize(String from) {
    if (from == null || from.equalsIgnoreCase("LATEST")) {
      return ProtocolVersion.MAXIMUM_VERSION;
    }

    String[] versions = from.split("-");
    for (String version : versions) {
      version = version.strip();
      for (ProtocolVersion protocolVersion : ProtocolVersion.values()) {
        if (protocolVersion.name().equalsIgnoreCase(version) || protocolVersion.getVersionsSupportedBy().contains(version)) {
          return protocolVersion;
        }
      }
    }

    throw new UnsupportedOperationException("Unsupported version: " + from);
  }
}
