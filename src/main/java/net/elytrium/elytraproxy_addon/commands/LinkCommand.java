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

package net.elytrium.elytraproxy_addon.commands;

import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import net.elytrium.elytraproxy_addon.Main;
import net.elytrium.elytraproxy_addon.config.Settings;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class LinkCommand implements SimpleCommand {

  private final Main plugin;

  public LinkCommand(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (!(source instanceof Player)) {
      source.sendMessage(LegacyComponentSerializer
          .legacyAmpersand()
          .deserialize("Пошёл нахуй пидор консольный"));
      return;
    }

    Player player = (Player) source;
    if (args.length != 1) {
      source.sendMessage(LegacyComponentSerializer
          .legacyAmpersand()
          .deserialize(Settings.IMP.NOT_ENOUGH_ARGUMENTS));
      return;
    }

    String uuidPattern =
        "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    if (!args[0].matches(uuidPattern)) {
      source.sendMessage(LegacyComponentSerializer
          .legacyAmpersand()
          .deserialize(Settings.IMP.NOT_VALID));
      return;
    }

    if (plugin.mySqlDatabase.getItem("users",
        ImmutableMap.of("uuid", player.getUniqueId()), LinkCommand.class) != null) {
      source.sendMessage(LegacyComponentSerializer
          .legacyAmpersand()
          .deserialize(Settings.IMP.ALREADY_COMPLETED));
      return;
    }

    List<NameValuePair> request = Arrays.asList(
        new BasicNameValuePair("masterKey", Settings.IMP.MASTER_KEY),
        new BasicNameValuePair("id", String.valueOf(player.getUniqueId())),
        new BasicNameValuePair("displayParam", player.getUsername()),
        new BasicNameValuePair("type", Settings.IMP.TYPE),
        new BasicNameValuePair("check", args[0])
    );

    try {
      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPost post = new HttpPost(Settings.IMP.URL);
      post.setEntity(new UrlEncodedFormEntity(request, StandardCharsets.UTF_8));
      CloseableHttpResponse httpResponse = httpClient.execute(post);

      boolean completed = EntityUtils.toString((HttpEntity) httpResponse.getEntity().getContent(),
          StandardCharsets.UTF_8).contains("OK");

      if (completed) {
        source.sendMessage(LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize(Settings.IMP.COMPLETED));
        plugin.mySqlDatabase.insertMap("users",
            ImmutableMap.of("uuid", player.getUniqueId()), true);
      } else {
        source.sendMessage(LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize(Settings.IMP.NOT_COMPLETED));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean hasPermission(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    if (!(source instanceof Player)) {
      return true;
    }
    Player player = (Player) source;
    return !Settings.IMP.DISALLOWED_SERVERS
        .contains(player.getCurrentServer().get().getServerInfo().getName());
  }
}
