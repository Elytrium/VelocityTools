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

package net.elytrium.velocitytools;

import java.io.File;
import java.util.List;
import net.elytrium.velocitytools.config.Config;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = BuildConstants.VERSION;

  @Create
  public COMMANDS COMMANDS;
  @Create
  public TOOLS TOOLS;

  @Comment({
      "Don't use \\n, use {NL} for new line. Ampersand (&) color codes are supported too.\n",
      "",
      "Permissions:",
      "  │",
      "  └── Commands:",
      "      │",
      "      ├── /velocitytools reload",
      "      │   └── velocitytools.admin.reload",
      "      ├── /alert",
      "      │   └── velocitytools.command.alert",
      "      ├── /find",
      "      │   └── velocitytools.command.find",
      "      ├── /send",
      "      │   └── velocitytools.command.send",
      "      └── /hub",
      "          ├── velocitytools.command.hub",
      "          └── velocitytools.command.hub.bypass.<servername> (disabled-servers bypass permission)",
      ""
  })
  public static class COMMANDS {

    @Create
    public ALERT ALERT;
    @Create
    public FIND FIND;
    @Create
    public SEND SEND;
    @Create
    public HUB HUB;

    public static class ALERT {

      public boolean ENABLED = true;
      public String PREFIX = "&8[&4Alert&8] &r{0}";
      public String MESSAGE_NEEDED = "&cYou must supply the message.";
      public String EMPTY_PROXY = "&cNo one is connected to this proxy!";
    }

    public static class FIND {

      public boolean ENABLED = true;
      public String USERNAME_NEEDED = "&cYou must supply the username.";
      public String PLAYER_ONLINE_AT = "&6{0} &fis online at &6{1}";
      public String PLAYER_NOT_ONLINE = "&6{0} &fis not online.";
    }

    public static class SEND {

      public boolean ENABLED = true;
      public String CONSOLE = "CONSOLE";
      public String NOT_ENOUGH_ARGUMENTS = "&fNot enough arguments. Usage: &6/send <server|player|all|current> <target>";
      @Comment("Set to \"\" to disable.")
      public String YOU_GOT_SUMMONED = "&fSummoned to &6{0} &fby &6{1}";
      public String PLAYER_NOT_ONLINE = "&6{0} &fis not online.";
      public String CALLBACK = "&aAttempting to send {0} players to {1}";
    }

    public static class HUB {

      public boolean ENABLED = true;
      public List<String> SERVERS = List.of("hub-1", "hub-2");
      @Comment("Set to \"\" to disable.")
      public String YOU_GOT_MOVED = "&aYou have been moved to a hub!";
      public String DISABLED_SERVER = "&cYou cannot use this command here.";
      public List<String> DISABLED_SERVERS = List.of("foo", "bar");
      public List<String> ALIASES = List.of("hub", "lobby");
    }
  }

  public static class TOOLS {

    @Comment("Hides the Legacy Ping message.")
    public boolean DISABLE_LEGACY_PING = true;
    @Comment("Hides the \"... provided invalid protocol ...\" message. Helps with some types of attacks. (https://media.discordapp.net/attachments/868930650537857024/921383075454259300/unknown.png)")
    public boolean DISABLE_INVALID_PROTOCOL = true;

    @Create
    public PROTOCOL_BLOCKER PROTOCOL_BLOCKER;
    @Create
    public BRAND_CHANGER BRAND_CHANGER;
    @Create
    public HOSTNAMES_MANAGER HOSTNAMES_MANAGER;

    public static class PROTOCOL_BLOCKER {

      public boolean BLOCK_JOIN = true;
      public boolean BLOCK_PING = false;
      @Comment("If true, all protocols except those listed below will be blocked.")
      public boolean WHITELIST = false;
      // TODO: Ну либо какой то сайт чтобы генерировать либо что то типа того
      @Comment("You can find a list of protocols here: https://wiki.vg/Protocol_version_numbers")
      public List<Integer> PROTOCOLS = List.of(9999, 9998);
      @Comment("For \"block-ping\" option.")
      public String BRAND = "Version is not supported!";
      @Comment("For \"block-ping\", set to \"\" to disable.")
      public String MOTD = "&cVersion is not supported!{NL}&ePlease, join with Minecraft 1.12.2 or newer.";
      @Comment("For \"block-joining\" option.")
      public String KICK_REASON = "&cYour version is unsupported!";
    }

    public static class BRAND_CHANGER {

      public boolean REWRITE_IN_PING = true;
      public boolean REWRITE_IN_GAME = true;
      public String PING_BRAND = "YourServer 1.12.2-1.17.1";
      @Comment("For ping.")
      public boolean SHOW_ALWAYS = false;
      @Comment("{0} - Original server brand (e.g. Paper).")
      public String IN_GAME_BRAND = "YourServer ({0})";
    }

    @Comment("Doesn't work with srv records.")
    public static class HOSTNAMES_MANAGER {

      public boolean BLOCK_JOIN = true;
      public boolean BLOCK_PING = true;
      @Comment("Connections IP logging.")
      public boolean DEBUG = false;
      @Comment("For \"debug\" option.")
      public boolean SHOW_BLOCKED_ONLY = false;
      @Comment("For \"block-joining\" option, set to \"\" to show the default reason.")
      public String KICK_REASON = "&cPlease, don't connect to the direct ip!{NL}Use example.com";
      public boolean WHITELIST = true;
      @Comment("IP Addresses starting with \"127.\" or equal to \"localhost\" will be blocked.")
      public boolean BLOCK_LOCAL_ADDRESSES = true;
      @Comment("DoMaIn.net will be similar to domain.net.")
      public boolean IGNORE_CASE = true;
      public List<String> HOSTNAMES = List.of("your-domain.net", "your-domain.com");
      @Comment("List of IP addresses that will bypass this check.")
      public List<String> IGNORED_IPS = List.of("79.555.*", "228.1337.*");
    }
  }

  public void reload(File file) {
    if (this.load(file)) {
      this.save(file);
    } else {
      this.save(file);
      this.load(file);
    }
  }
}
