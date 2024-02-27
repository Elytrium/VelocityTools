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

package net.elytrium.velocitytools;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.List;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.RegisterPlaceholders;
import net.elytrium.serializer.language.object.YamlSerializable;
import net.elytrium.velocitytools.serialization.replacers.ComponentReplacer;
import net.elytrium.velocitytools.serialization.serializers.ComponentSerializer;
import net.elytrium.velocitytools.serialization.serializers.ProtocolVersionSerializer;
import net.kyori.adventure.text.Component;

public class Settings extends YamlSerializable { // TODO customizable logger + tab complete logger

  public static final Settings HEAD = new Settings(new SerializerConfig.Builder()
      .registerReplacer(new ComponentReplacer())
      .registerSerializer(new ComponentSerializer())
      .registerSerializer(new ProtocolVersionSerializer())
      .setCommentValueIndent(1)
      .build()
  );

  public static final Settings.Main MAIN = Settings.HEAD.main;

  public static final Settings.Commands.AlertCommand ALERT_COMMAND = Settings.HEAD.commands.alert;
  public static final Settings.Commands.FindCommand FIND_COMMAND = Settings.HEAD.commands.find;
  public static final Settings.Commands.HubCommand HUB_COMMAND = Settings.HEAD.commands.hub;

  public static final Settings.Tools TOOLS = Settings.HEAD.tools;
  public static final Settings.Tools.ProtocolBlockerModule PROTOCOL_BLOCKER = Settings.TOOLS.protocolBlocker;
  public static final Settings.Tools.BrandChangerModule BRAND_CHANGER = Settings.TOOLS.brandChanger;
  public static final Settings.Tools.HostnamesManagerModule HOSTNAMES_MANAGER = Settings.TOOLS.hostnamesManager;

  public final String version = BuildConfig.VERSION;

  @Comment({
      @CommentValue("Available serializers:"),
      @CommentValue("LEGACY_AMPERSAND - \"&c&lExample &c&9Text\"."),
      @CommentValue("LEGACY_SECTION - \"§c§lExample §c§9Text\"."),
      @CommentValue("MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)"),
      @CommentValue("GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)"),
      @CommentValue("PLAIN - Any plain text.")
  })
  net.elytrium.velocitytools.serialization.ComponentSerializer serializer = net.elytrium.velocitytools.serialization.ComponentSerializer.LEGACY_AMPERSAND;

  Main main = new Main();
  Commands commands = new Commands();
  Tools tools = new Tools();

  private Settings(SerializerConfig config) {
    super(config);
  }

  public static net.elytrium.velocitytools.serialization.ComponentSerializer serializer() {
    return Settings.HEAD == null ? net.elytrium.velocitytools.serialization.ComponentSerializer.LEGACY_AMPERSAND : Settings.HEAD.serializer;
  }

  public static class Main {

    @Comment({
        @CommentValue("VelocityTools will consume more RAM if this option is enabled, but compatibility with other plugins will be better"),
        @CommentValue("Enable it if you have a plugin installed that bypasses compression (e.g. Geyser)")
    })
    public boolean saveUncompressedPackets = true;
  }

  @Comment({
      @CommentValue("Permissions:"),
      @CommentValue(" │"),
      @CommentValue(" └── Commands:"),
      @CommentValue("      │"),
      @CommentValue("      ├── /velocitytools reload"),
      @CommentValue("      │    └── velocitytools.admin.reload"),
      @CommentValue("      ├── /alert"),
      @CommentValue("      │    └── velocitytools.command.alert"),
      @CommentValue("      ├── /find"),
      @CommentValue("      │    └── velocitytools.command.find"),
      @CommentValue("      ├── /send"),
      @CommentValue("      │    └── velocitytools.command.send"),
      @CommentValue("      └── /hub"),
      @CommentValue("           ├── velocitytools.command.hub"),
      @CommentValue("           └── velocitytools.command.hub.bypass.<servername> (disabled-servers bypass permission)"),
      @CommentValue()
  })
  public static class Commands {

    AlertCommand alert = new AlertCommand();
    FindCommand find = new FindCommand();
    HubCommand hub = new HubCommand();

    public static class AlertCommand {

      public boolean enabled = true;
      @RegisterPlaceholders("0")
      public Component prefix = Settings.serializer().deserialize("&8[&4Alert&8] &r{0}"); // TODO rename to format
      public Component messageNeeded = Settings.serializer().deserialize("&cYou must supply the message.");
      public Component emptyProxy = Settings.serializer().deserialize("&cNo one is connected to this proxy!");
    }

    public static class FindCommand {

      public boolean enabled = true;
      public Component usernameNeeded = Settings.serializer().deserialize("&cYou must supply the username.");
      @RegisterPlaceholders({"0", "1"})
      public Component playerOnlineAt = Settings.serializer().deserialize("&6{0} &fis online at &6{1}");
      @RegisterPlaceholders("0")
      public Component playerNotOnServer = Settings.serializer().deserialize("&6{0} &fis not on any of servers.");
      @RegisterPlaceholders("0")
      public Component playerNotOnline = Settings.serializer().deserialize("&6{0} &fis not online.");
    }

    public static class HubCommand {

      public boolean enabled = true;
      @Comment(@CommentValue("Balances players between hub servers"))
      public boolean spreadOnJoin = false;
      public boolean spreadOnKick = false;
      public List<String> servers = List.of("hub-1", "hub-2");
      @Comment(@CommentValue("Set to \"\" to disable."))
      public Component youGotMoved = Settings.serializer().deserialize("&aYou have been moved to a hub!");
      public Component disabledServer = Settings.serializer().deserialize("&cYou cannot use this command here.");
      public List<String> disabledServers = List.of("foo", "bar");
      public List<String> aliases = List.of("hub", "lobby");
    }
  }

  public static class Tools {

    @Comment(@CommentValue("Hides the Legacy Ping message."))
    public boolean disableLegacyPing = true; // TODO rename to "disable legacy ping"
    @Comment(@CommentValue("Hides the \"... provided invalid protocol ...\" message. Helps with some types of attacks. (https://media.discordapp.net/attachments/868930650537857024/921383075454259300/unknown.png)"))
    public boolean disableInvalidProtocol = true; // TODO rename to "omit invalid protocol message"

    ProtocolBlockerModule protocolBlocker = new ProtocolBlockerModule();
    BrandChangerModule brandChanger = new BrandChangerModule();
    HostnamesManagerModule hostnamesManager = new HostnamesManagerModule();

    public static class ProtocolBlockerModule {

      public boolean blockJoin = true;
      public boolean blockPing = false;
      @Comment(@CommentValue("If true, all protocols except those listed below will be blocked."))
      public boolean whitelist = false;
      @Comment({
          @CommentValue("You can set either a protocol number here (e.g. '340' for 1.12.2) or a Minecraft version below (e.g. '1.12.2')"),
          @CommentValue("You can find a list of protocols here: https://wiki.vg/Protocol_version_numbers"),
      })
      public List<Integer> protocols = List.of(9999, 9998);
      @Comment({
          @CommentValue("<min>-<max> means that versions from <min> to <max> have the same protocol version,"),
          @CommentValue("so these versions will be considered as one version"),
          @CommentValue(),
          @CommentValue("List of versions:"),
          @CommentValue("1.7.2-1.7.5, 1.7.6-1.7.10, 1.8-1.8.9, 1.9, 1.9.1, 1.9.2-1.9.3, 1.9.4,"),
          @CommentValue("1.10-1.10.2, 1.11, 1.11.1-1.11.2, 1.12, 1.12.1, 1.12.2,"),
          @CommentValue("1.13, 1.13.1, 1.13.2, 1.14, 1.14.1, 1.14.2, 1.14.3, 1.14.4,"),
          @CommentValue("1.15, 1.15.1, 1.15.2, 1.16, 1.16.1, 1.16.2, 1.16.3, 1.16.4-1.16.5,"),
          @CommentValue("1.17, 1.17.1, 1.18-1.18.1, 1.18.2, 1.19, 1.19.1-1.19.2, 1.19.3, 1.19.4, 1.20-1.20.1, 1.20.2, 1.20.3-1.20.4, LATEST")
      })
      public List<ProtocolVersion> versions = List.of(ProtocolVersion.MINECRAFT_1_7_2); // TODO fix primitive serializers for colections
      public ProtocolVersion minimumVersion = ProtocolVersion.MINECRAFT_1_7_2;
      public ProtocolVersion maximumVersion = ProtocolVersion.MAXIMUM_VERSION;
      @Comment(@CommentValue("For \"block-ping\" option."))
      public String brand = "Version is not supported!";
      @Comment(@CommentValue("For \"block-ping\", set to \"\" to disable."))
      public Component motd = Settings.serializer().deserialize("&cVersion is not supported!\n&ePlease, join with Minecraft 1.12.2 or newer.");
      @Comment(@CommentValue("For \"block-joining\" option."))
      public Component kickReason = Settings.serializer().deserialize("&cYour version is unsupported!");
    }

    public static class BrandChangerModule {

      public boolean rewriteInPing = true;
      public boolean rewriteInGame = true;
      public String pingBrand = "YourServer 1.12.2-1.20.4";
      @Comment(@CommentValue("For ping."))
      public boolean showAlways = false;
      @Comment(@CommentValue("{0} - Original server brand (e.g. Paper)."))
      @RegisterPlaceholders("0")
      public String inGameBrand = "YourServer ({0})";
    }

    @Comment(@CommentValue("Doesn't work with srv records."))
    public static class HostnamesManagerModule {

      public boolean blockJoin = false;
      public boolean blockPing = false;
      @Comment(@CommentValue("Connections IP logging."))
      public boolean debug = false;
      @Comment(@CommentValue("For \"debug\" option."))
      public boolean showBlockedOnly = false;
      @Comment(@CommentValue("For \"block-joining\" option, set to \"\" to show the default reason."))
      public Component kickReason = Settings.serializer().deserialize("&cPlease, don't connect to the direct ip!\nUse example.com");
      public boolean whitelist = true;
      @Comment(@CommentValue("IP Addresses starting with \"127.\" or equal to \"localhost\" will be blocked."))
      public boolean blockLocalAddresses = false; // TODO remove
      @Comment(@CommentValue("DoMaIn.net will be similar to domain.net."))
      public boolean ignoreCase = true;
      public List<String> hostnames = List.of("your-domain.net", "your-domain.com");
      @Comment(@CommentValue("List of IP addresses that will bypass this check."))
      public List<String> ignoredIps = List.of("79.555.*", "228.1337.*");
    }
  }
}
