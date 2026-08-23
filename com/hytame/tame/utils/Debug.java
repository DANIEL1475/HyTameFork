package com.hytame.tame.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hytame.HyTamePlugin;
import java.util.logging.Level;

public final class Debug {
   public static HytaleLogger LOGGER = HyTamePlugin.getInstance().getLogger();

   private Debug() {
   }

   public static Boolean isNullLog(Object obj, String message) {
      if (obj == null) {
         ((HytaleLogger.Api)LOGGER.atSevere()).log(message);
         return true;
      } else {
         return false;
      }
   }

   public static Boolean isNullMsg(PlayerRef player, Object obj, String message) {
      if (obj == null) {
         log(message, Level.SEVERE);
         msg(player, message, Level.SEVERE);
         return true;
      } else {
         return false;
      }
   }

   public static void log(String message, Level level) {
      if (level != Level.INFO || HyTamePlugin.isVerboseLogging()) {
         LOGGER.at(level).log(message);
      }
   }

   public static void msg(PlayerRef player, String message, Level level) {
      log(message, level);
      if (player != null) {
         Message var10000;
         switch (level.getName()) {
            case "SEVERE" -> var10000 = Message.raw(message).color("#FF5555").bold(true);
            case "WARNING" -> var10000 = Message.raw(message).color("#FFFF55");
            default -> var10000 = Message.raw(message).color("#FFFFFF");
         }

         Message msg = var10000;
         player.sendMessage(msg);
      }
   }
}
