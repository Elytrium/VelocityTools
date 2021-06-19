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

package net.elytrium.elytraproxy_addon.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import net.elytrium.elytraproxy.config.Config;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  public String WDL_KICK = "Не качай пжпжпжпжпж";

  public String MASTER_KEY = "вставь ключ, еблан";
  public String TYPE = "укажи тип, еблан";
  public String URL = "ссылку тип, еблан";

  public String NOT_ENOUGH_ARGUMENTS = "Укажите ключ.";
  public String NOT_VALID = "Введите правильный ключ.";
  public String COMPLETED = "Вы успешно привязали свой аккаунт.";
  public String ALREADY_COMPLETED = "Ваш аккаунт уже привязан.";
  public String NOT_COMPLETED = "Не удалось привязать ваш аккаунт.";
  public List<String> DISALLOWED_SERVERS = Arrays.asList("auth");

  @Create
  public SQL SQL;

  public static class SQL {
    public String HOSTNAME = "127.0.0.1:3306";
    public String USER = "user";
    public String PASSWORD = "password";
    public String DATABASE = "database";
  }

  public void reload(File file) {
    load(file);
    save(file);
  }
}
