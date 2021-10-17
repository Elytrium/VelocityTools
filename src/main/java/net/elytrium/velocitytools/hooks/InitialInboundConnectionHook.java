package net.elytrium.velocitytools.hooks;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import java.lang.reflect.Field;

public class InitialInboundConnectionHook {

  private static Field mcConnectionField;

  static {
    try {
      mcConnectionField = InitialInboundConnection.class.getDeclaredField("connection");
      mcConnectionField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public static MinecraftConnection get(InboundConnection connection) throws IllegalAccessException {
    return (MinecraftConnection) mcConnectionField.get(connection);
  }
}
