package com.hytame.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hytame.HyTamePlugin;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BreedCommand extends AbstractCommand {
   private static final Message DEPRECATION_WARNING = Message.raw("[Deprecated] Use /hytame instead. /breed will be removed in a future version.").color("#FFAA00");
   private static final Set<UUID> hytalorWarningShown = ConcurrentHashMap.newKeySet();

   public BreedCommand() {
      super("breed", "[Deprecated] Use /hytame - Animal Breeding & Taming");
      this.addSubCommand(new DeprecatedHelpSubCommand());
      this.addSubCommand(new DeprecatedStatusSubCommand());
      this.addSubCommand(new DeprecatedInfoSubCommand());
      this.addSubCommand(new DeprecatedTameSubCommand());
      this.addSubCommand(new DeprecatedUntameSubCommand());
      this.addSubCommand(new DeprecatedSettingsSubCommand());
      this.addSubCommand(new DeprecatedScanSubCommand());
      this.addSubCommand(new DeprecatedFoodsSubCommand());
      this.addSubCommand(new DeprecatedConfigSubCommand());
      this.addSubCommand(new DeprecatedGrowthSubCommand());
      this.addSubCommand(new DeprecatedCustomSubCommand());
      this.addSubCommand(new DeprecatedDebugSubCommand());
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      if (showHytalorWarningIfNeeded(ctx)) {
         return CompletableFuture.completedFuture((Object)null);
      } else {
         ctx.sendMessage(DEPRECATION_WARNING);
         ctx.sendMessage(Message.raw(""));
         showDeprecatedHelp(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static boolean showHytalorWarningIfNeeded(CommandContext ctx) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null && !plugin.isHytalorInstalled()) {
         boolean isAdmin = !(CommandUtil.player(ctx) instanceof Player) || HytamePermissions.hasAdminAccess(CommandUtil.player(ctx));
         if (isAdmin) {
            UUID playerUuid = null;
            Player var5 = CommandUtil.player(ctx);
            if (var5 instanceof Player) {
               Player player = var5;

               try {
                  playerUuid = player.getUuid();
               } catch (Exception var6) {
               }
            }

            if (playerUuid == null || !hytalorWarningShown.contains(playerUuid)) {
               ctx.sendMessage(Message.raw("WARNING: HYTALOR NOT DETECTED - HyTame requires Hytalor!").color("#FF5555"));
               ctx.sendMessage(Message.raw("Install from: curseforge.com/hytale/mods/hytalor").color("#AAAAAA"));
               ctx.sendMessage(Message.raw(""));
               if (playerUuid != null) {
                  hytalorWarningShown.add(playerUuid);
               }
            }

            return true;
         } else {
            ctx.sendMessage(Message.raw("[HyTame] This feature is currently unavailable.").color("#FF5555"));
            ctx.sendMessage(Message.raw("Server needs Hytalor installed.").color("#AAAAAA"));
            return true;
         }
      } else {
         return false;
      }
   }

   private static void showDeprecatedHelp(CommandContext ctx) {
      ctx.sendMessage(Message.raw("=== /breed is deprecated - Use /hytame ===").color("#FFAA00"));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("The following commands have been renamed:").color("#AAAAAA"));
      ctx.sendMessage(Message.raw("  /breed help    ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame help").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed status  ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame status").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed config  ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame config").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed growth  ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame growth").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed tame    ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame tame").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed untame  ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame untame").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed info    ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame info").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed settings").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame settings").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed custom  ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame custom").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed scan    ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame scan").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("  /breed debug   ").color("#888888").insert(Message.raw("->").color("#555555")).insert(Message.raw(" /hytame debug").color("#FFFFFF")));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("For full help, run: ").color("#AAAAAA").insert(Message.raw("/hytame help").color("#55FF55")));
   }

   private abstract static class DeprecatedSubCommand extends AbstractCommand {
      public DeprecatedSubCommand(String name, String description) {
         super(name, "[Deprecated] " + description);
      }

      protected final CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedCommand.showHytalorWarningIfNeeded(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(BreedCommand.DEPRECATION_WARNING);
            ctx.sendMessage(Message.raw(""));
            return this.executeDeprecated(ctx);
         }
      }

      protected abstract CompletableFuture<Void> executeDeprecated(CommandContext var1);
   }

   private static class DeprecatedHelpSubCommand extends DeprecatedSubCommand {
      public DeprecatedHelpSubCommand() {
         super("help", "Show help information");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameHelpSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedStatusSubCommand extends DeprecatedSubCommand {
      public DeprecatedStatusSubCommand() {
         super("status", "View tracked animals");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameStatusSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedInfoSubCommand extends DeprecatedSubCommand {
      public DeprecatedInfoSubCommand() {
         super("info", "Show taming info");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameInfoSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedTameSubCommand extends DeprecatedSubCommand {
      public DeprecatedTameSubCommand() {
         super("tame", "Tame an animal");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameTameSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedUntameSubCommand extends DeprecatedSubCommand {
      public DeprecatedUntameSubCommand() {
         super("untame", "Release a tamed animal");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         ctx.sendMessage(Message.raw("Use /hytame untame instead (look at the animal).").color("#FFAA00"));
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedSettingsSubCommand extends DeprecatedSubCommand {
      public DeprecatedSettingsSubCommand() {
         super("settings", "Taming settings");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameSettingsSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedScanSubCommand extends DeprecatedSubCommand {
      public DeprecatedScanSubCommand() {
         super("scan", "Scan for untracked babies");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameScanSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedFoodsSubCommand extends DeprecatedSubCommand {
      public DeprecatedFoodsSubCommand() {
         super("foods", "Quick food reference");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameFoodsSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedConfigSubCommand extends DeprecatedSubCommand {
      public DeprecatedConfigSubCommand() {
         super("config", "Configuration commands");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameConfigSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedGrowthSubCommand extends DeprecatedSubCommand {
      public DeprecatedGrowthSubCommand() {
         super("growth", "Toggle baby growth");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameGrowthSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedCustomSubCommand extends DeprecatedSubCommand {
      public DeprecatedCustomSubCommand() {
         super("custom", "Manage custom animals");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameCustomSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static class DeprecatedDebugSubCommand extends DeprecatedSubCommand {
      public DeprecatedDebugSubCommand() {
         super("debug", "Debug commands");
      }

      protected CompletableFuture<Void> executeDeprecated(CommandContext ctx) {
         (new HytameCommand.HytameDebugSubCommand()).executeFromDeprecated(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }
}
