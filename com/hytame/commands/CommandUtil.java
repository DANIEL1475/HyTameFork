package com.hytame.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hytame.util.EcsReflectionUtil;

public final class CommandUtil {
   private CommandUtil() {
   }

   public static Player player(CommandContext ctx) {
      if (ctx != null) {
         CommandSender var2 = ctx.sender();
         if (var2 instanceof PlayerRef) {
            PlayerRef pref = (PlayerRef)var2;

            try {
               return (Player)pref.getComponent(EcsReflectionUtil.PLAYER_TYPE);
            } catch (Exception var3) {
               return null;
            }
         }
      }

      return null;
   }
}
