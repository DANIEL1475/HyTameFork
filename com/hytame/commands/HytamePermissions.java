package com.hytame.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HytamePermissions {
   public static final String BASE = "hytame.";
   public static final String ADMIN = "hytame.admin.";
   public static final String HELP = "hytame.help";
   public static final String STATUS = "hytame.status";
   public static final String INFO = "hytame.info";
   public static final String TAME = "hytame.tame";
   public static final String SCAN = "hytame.scan";
   public static final String SETTINGS = "hytame.settings";
   public static final String CONFIG = "hytame.admin.config";
   public static final String GROWTH = "hytame.admin.growth";
   public static final String CUSTOM = "hytame.admin.custom";
   public static final String DEBUG = "hytame.admin.debug";

   public static boolean hasAdminAccess(Player player) {
      if (player == null) {
         return false;
      } else {
         if (Constants.SINGLEPLAYER) {
            String threadName = Thread.currentThread().getName();
            boolean onWorldThread = threadName.contains("WorldThread");
            if (!onWorldThread) {
               if (!player.getPlayerRef().hasPermission("hytame.admin.*") && !player.getPlayerRef().hasPermission("hytame.admin")) {
                  return true;
               }

               return true;
            }

            try {
               Ref<EntityStore> ref = player.getReference();
               if (ref != null && ref.isValid()) {
                  PlayerRef playerRef = (PlayerRef)ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                  if (playerRef != null && SingleplayerModule.isOwner(playerRef)) {
                     return true;
                  }
               }
            } catch (Throwable var5) {
            }
         }

         return player.getPlayerRef().hasPermission("hytame.admin.*") || player.getPlayerRef().hasPermission("hytame.admin");
      }
   }

   public static boolean hasPermission(Player player, String permission) {
      if (player == null) {
         return false;
      } else {
         return permission.startsWith("hytame.admin.") ? hasAdminAccess(player) : player.getPlayerRef().hasPermission(permission);
      }
   }

   public static boolean hasAccess(Player player, boolean requiresAdmin) {
      return !requiresAdmin ? true : hasAdminAccess(player);
   }

   private HytamePermissions() {
   }
}
