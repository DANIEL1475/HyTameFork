package com.hytame.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hytame.HyTamePlugin;
import com.hytame.interactions.InteractionStateCache;
import com.hytame.listeners.NewAnimalSpawnDetector;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.GrowthManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.GrowthStage;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LegacyCommands {
   private static final Set<UUID> hytalorWarningShown = ConcurrentHashMap.newKeySet();
   private static final String[] HINT_FORMATS = new String[]{"Feed", "Press [F] to Feed", "Press [Use] to Feed", "[F] Feed", "§ePress §f[F]§e to Feed", "server.interactionHints.generic", "Press [{key}] to Feed", "@server.interactionHints.generic"};
   private static int currentHintFormatIndex = 0;

   private static boolean checkHytalorWarning(CommandContext ctx) {
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
               ctx.sendMessage(Message.raw("Install from: https://www.curseforge.com/hytale/mods/hytalor").color("#AAAAAA"));
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

   public static String getCurrentHintFormat() {
      return HINT_FORMATS[currentHintFormatIndex];
   }

   public static int cycleHintFormat() {
      currentHintFormatIndex = (currentHintFormatIndex + 1) % HINT_FORMATS.length;
      return currentHintFormatIndex;
   }

   public static class TamingInfoCommand extends AbstractCommand {
      public TamingInfoCommand() {
         super("taminginfo", "[Deprecated] Show taming information - Use /hytame info instead");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (LegacyCommands.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("[Deprecated] Use /hytame info instead").color("#FFAA00"));
            ctx.sendMessage(Message.raw(""));
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getTamingManager() != null) {
               TamingManager taming = plugin.getTamingManager();
               ctx.sendMessage(Message.raw("=== Taming Status ===").color("#FF9900"));
               ctx.sendMessage(Message.raw("Total tamed: ").color("#AAAAAA").insert(Message.raw(String.valueOf(taming.getTamedCount())).color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Awaiting respawn: ").color("#AAAAAA").insert(Message.raw(String.valueOf(taming.getDespawnedCount())).color("#FFFF55")));
               ctx.sendMessage(Message.raw("Use /hytame status for detailed info").color("#AAAAAA"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               ctx.sendMessage(Message.raw("Taming system not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class TamingSettingsCommand extends AbstractCommand {
      public TamingSettingsCommand() {
         super("tamingsettings", "[Deprecated] Toggle taming settings - Use /hytame settings instead");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ctx.sendMessage(Message.raw("[Deprecated] Use /hytame settings instead").color("#FFAA00"));
         ctx.sendMessage(Message.raw(""));
         ctx.sendMessage(Message.raw("This command is not yet available.").color("#FFFF55"));
         ctx.sendMessage(Message.raw("By default, others CAN interact with your tamed animals.").color("#AAAAAA"));
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class BreedingHelpCommand extends AbstractCommand {
      public BreedingHelpCommand() {
         super("laitsbreeding", "[Deprecated] Show breeding plugin help - Use /hytame help instead");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (LegacyCommands.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("[Deprecated] Use /hytame instead").color("#FFAA00"));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("=== Lait's Animal Breeding v1.5.3 ===").color("#FF9900"));
            ctx.sendMessage(Message.raw("Main Command: ").color("#AAAAAA").insert(Message.raw("/hytame").color("#FFFFFF")));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("Commands:").color("#FFAA00"));
            ctx.sendMessage(Message.raw("/hytame").color("#FFFFFF").insert(Message.raw(" - Main command").color("#55FF55")));
            ctx.sendMessage(Message.raw("/hytame status").color("#FFFFFF").insert(Message.raw(" - View tracked animals").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("/hytame config").color("#FFFFFF").insert(Message.raw(" - Configuration commands").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("/hytame growth").color("#FFFFFF").insert(Message.raw(" - Toggle baby growth").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("/hytame info").color("#FFFFFF").insert(Message.raw(" - View tamed animals").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("/hytame custom").color("#FFFFFF").insert(Message.raw(" - Manage custom animals").color("#AAAAAA")));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("Feed animals their favorite food to breed!").color("#55FF55"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class BreedingStatusCommand extends AbstractCommand {
      public BreedingStatusCommand() {
         super("breedstatus", "[Deprecated] Show breeding status - Use /hytame status instead");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (LegacyCommands.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("[Deprecated] Use /hytame status instead").color("#FFAA00"));
            ctx.sendMessage(Message.raw(""));
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               BreedingManager breeding = plugin.getBreedingManager();
               GrowthManager growth = plugin.getGrowthManager();
               TamingManager taming = plugin.getTamingManager();
               ctx.sendMessage(Message.raw("=== Breeding Status ===").color("#FF9900"));
               ctx.sendMessage(Message.raw("Version: ").color("#AAAAAA").insert(Message.raw("1.5.3").color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Animals tracked: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breeding.getTrackedCount())).color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Pregnant: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breeding.getPregnantCount())).color("#FFFF55")));
               ctx.sendMessage(Message.raw("In love: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breeding.getInLoveCount())).color("#FF55FF")));
               ctx.sendMessage(Message.raw(""));
               Map<GrowthStage, Integer> stageCounts = growth.getGrowthStageCounts();
               ctx.sendMessage(Message.raw("Growth stages:").color("#FFFF55"));
               ctx.sendMessage(Message.raw("  Babies: ").color("#AAAAAA").insert(Message.raw(String.valueOf(stageCounts.get(GrowthStage.BABY))).color("#55FFFF")));
               ctx.sendMessage(Message.raw("  Juveniles: ").color("#AAAAAA").insert(Message.raw(String.valueOf(stageCounts.get(GrowthStage.JUVENILE))).color("#55FFFF")));
               ctx.sendMessage(Message.raw("  Adults: ").color("#AAAAAA").insert(Message.raw(String.valueOf(stageCounts.get(GrowthStage.ADULT))).color("#55FF55")));
               ctx.sendMessage(Message.raw(""));
               ctx.sendMessage(Message.raw("Spawn Detection:").color("#FFFF55"));
               int detectedCount = NewAnimalSpawnDetector.getDetectedCount();
               long lastDetection = NewAnimalSpawnDetector.getLastDetectionTime();
               String lastAnimal = NewAnimalSpawnDetector.getLastDetectedAnimal();
               ctx.sendMessage(Message.raw("  Detected spawns: ").color("#AAAAAA").insert(Message.raw(String.valueOf(detectedCount)).color("#FFFFFF")));
               if (lastDetection > 0L) {
                  long secondsAgo = (System.currentTimeMillis() - lastDetection) / 1000L;
                  ctx.sendMessage(Message.raw("  Last detection: ").color("#AAAAAA").insert(Message.raw(lastAnimal + " (" + secondsAgo + "s ago)").color("#55FFFF")));
               } else {
                  ctx.sendMessage(Message.raw("  Last detection: ").color("#AAAAAA").insert(Message.raw("none").color("#777777")));
               }

               if (breeding.getTrackedCount() == 0) {
                  ctx.sendMessage(Message.raw(""));
                  ctx.sendMessage(Message.raw("No animals tracked yet. Feed some animals!").color("#AAAAAA"));
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class BreedingLogsCommand extends AbstractCommand {
      public BreedingLogsCommand() {
         super("breedlogs", "Toggle breeding plugin verbose logs");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         boolean newState = !HyTamePlugin.isVerboseLogging();
         HyTamePlugin.setVerboseLogging(newState);
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[HyTame] Verbose logging " + (newState ? "enabled" : "disabled"));
         }

         String statusColor = newState ? "#55FF55" : "#FF5555";
         String statusText = newState ? "ENABLED" : "DISABLED";
         ctx.sendMessage(Message.raw("Verbose logging ").color("#AAAAAA").insert(Message.raw(statusText).color(statusColor)));
         if (newState) {
            ctx.sendMessage(Message.raw("Debug information will now appear in server logs.").color("#AAAAAA"));
         }

         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class NoClipCommand extends AbstractCommand {
      private static final Set<String> noclipPlayers = ConcurrentHashMap.newKeySet();

      public NoClipCommand() {
         super("noclip", "Toggle noclip (invulnerable + fly camera)");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         Player player = CommandUtil.player(ctx);
         String playerName = player.getLegacyDisplayName();
         boolean enabling = !noclipPlayers.contains(playerName);
         if (enabling) {
            noclipPlayers.add(playerName);
         } else {
            noclipPlayers.remove(playerName);
         }

         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class BreedingGrowthCommand extends AbstractCommand {
      public BreedingGrowthCommand() {
         super("breedgrowth", "[Deprecated] Toggle baby animal growth - Use /hytame growth instead");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ctx.sendMessage(Message.raw("[Deprecated] Use /hytame growth instead").color("#FFAA00"));
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getConfigManager() != null) {
            boolean newState = !plugin.getConfigManager().isGrowthEnabled();
            plugin.getConfigManager().setGrowthEnabled(newState);
            String statusColor = newState ? "#55FF55" : "#FF5555";
            String statusText = newState ? "ENABLED" : "DISABLED";
            ctx.sendMessage(Message.raw("Baby growth ").color("#AAAAAA").insert(Message.raw(statusText).color(statusColor)));
            if (!newState) {
               ctx.sendMessage(Message.raw("Babies will not grow into adults until re-enabled.").color("#AAAAAA"));
            }

            plugin.getConfigManager().saveToFile();
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("Plugin not initialized").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class BreedingScanCommand extends AbstractCommand {
      public BreedingScanCommand() {
         super("breedscan", "Manually trigger animal scan for breeding interactions");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            boolean wasVerbose = HyTamePlugin.isVerboseLogging();
            HyTamePlugin.setVerboseLogging(true);
            ctx.sendMessage(Message.raw("Starting manual animal scan...").color("#FFFF55"));
            ctx.sendMessage(Message.raw("Check server logs for details (verbose logging enabled)").color("#AAAAAA"));

            try {
               plugin.autoSetupNearbyAnimals();
               ctx.sendMessage(Message.raw("Scan triggered successfully").color("#55FF55"));
            } catch (Exception e) {
               ctx.sendMessage(Message.raw("Scan error: " + e.getMessage()).color("#FF5555"));
            }

            if (!wasVerbose) {
               plugin.getTickScheduler().schedule(() -> HyTamePlugin.setVerboseLogging(false), 5L, TimeUnit.SECONDS);
            }

            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class BreedingDevCommand extends AbstractCommand {
      public BreedingDevCommand() {
         super("breeddev", "Toggle in-game chat logging (shows all debug messages in chat)");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         boolean newState = !HyTamePlugin.isDevMode();
         HyTamePlugin.setDevMode(newState);
         HyTamePlugin.setVerboseLogging(newState);
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[HyTame] Chat logging " + (newState ? "enabled" : "disabled"));
         }

         String statusColor = newState ? "#55FF55" : "#FF5555";
         String statusText = newState ? "ENABLED" : "DISABLED";
         ctx.sendMessage(Message.raw("Chat logging ").color("#AAAAAA").insert(Message.raw(statusText).color(statusColor)));
         if (newState) {
            ctx.sendMessage(Message.raw("All debug messages will now appear in chat.").color("#FFAA00"));
            ctx.sendMessage(Message.raw("Use /breeddev again to disable").color("#AAAAAA"));
         }

         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class BreedingHintCommand extends AbstractCommand {
      public BreedingHintCommand() {
         super("breedhint", "Cycle through hint format options");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         int newIndex = LegacyCommands.cycleHintFormat();
         String newFormat = LegacyCommands.getCurrentHintFormat();
         ctx.sendMessage(Message.raw("Hint Test - Format #" + newIndex + ": ").color("#FFFF55").insert(Message.raw("\"" + newFormat + "\"").color("#FFFFFF")));
         ctx.sendMessage(Message.raw("Look at an animal to see the new hint format.").color("#AAAAAA"));
         ctx.sendMessage(Message.raw("Run ").color("#AAAAAA").insert(Message.raw("/breedhint").color("#FFFFFF")).insert(Message.raw(" again to try the next format.").color("#AAAAAA")));
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            plugin.attachInteractionsToAnimals();
         }

         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class BreedingCachesCommand extends AbstractCommand {
      public BreedingCachesCommand() {
         super("breedcaches", "Show cache sizes for debugging memory leaks");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            ctx.sendMessage(Message.raw("Plugin not available").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("=== Cache Status ===").color("#FF9900"));
            if (plugin.getSpawnDetector() != null) {
               int spawnCacheSize = plugin.getSpawnDetector().getProcessedCacheSize();
               ctx.sendMessage(Message.raw("  processedEntities: ").color("#AAAAAA").insert(Message.raw(String.valueOf(spawnCacheSize)).color("#FFFFFF")));
            } else {
               ctx.sendMessage(Message.raw("  processedEntities: ").color("#AAAAAA").insert(Message.raw("N/A (detector not running)").color("#FF5555")));
            }

            int interactionsCacheSize = InteractionStateCache.getInstance().getCacheSize();
            ctx.sendMessage(Message.raw("  originalStates: ").color("#AAAAAA").insert(Message.raw(String.valueOf(interactionsCacheSize)).color("#FFFFFF")));
            if (plugin.getBreedingManager() != null) {
               int breedingCacheSize = plugin.getBreedingManager().getTrackedCount();
               ctx.sendMessage(Message.raw("  breedingDataMap: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breedingCacheSize)).color("#FFFFFF")));
            } else {
               ctx.sendMessage(Message.raw("  breedingDataMap: ").color("#AAAAAA").insert(Message.raw("N/A").color("#FF5555")));
            }

            if (plugin.getTamingManager() != null) {
               int tamedCount = plugin.getTamingManager().getTamedCount();
               ctx.sendMessage(Message.raw("  tamedAnimals: ").color("#AAAAAA").insert(Message.raw(String.valueOf(tamedCount)).color("#FFFFFF")));
            } else {
               ctx.sendMessage(Message.raw("  tamedAnimals: ").color("#AAAAAA").insert(Message.raw("N/A").color("#FF5555")));
            }

            ctx.sendMessage(Message.raw("Caches are cleaned periodically (every 5-10 min).").color("#AAAAAA"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class NameTagCommand extends AbstractCommand {
      private final RequiredArg<String> nameArg;

      public NameTagCommand() {
         super("nametag", "[Deprecated] Use Name Tag item on animal instead");
         this.nameArg = this.withRequiredArg("name", "Name for the animal", ArgTypes.STRING);
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ctx.sendMessage(Message.raw("[Deprecated] This command has been replaced.").color("#FFAA00"));
         ctx.sendMessage(Message.raw("To tame an animal:").color("#AAAAAA"));
         ctx.sendMessage(Message.raw("  1. Hold a Name Tag item").color("#FFFFFF"));
         ctx.sendMessage(Message.raw("  2. Press F on an animal").color("#FFFFFF"));
         ctx.sendMessage(Message.raw("  3. Enter a name in the UI").color("#FFFFFF"));
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class UntameCommand extends AbstractCommand {
      public UntameCommand() {
         super("untame", "[Deprecated] Use /hytame untame <name> instead");
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ctx.sendMessage(Message.raw("[Deprecated] This command has been replaced.").color("#FFAA00"));
         ctx.sendMessage(Message.raw("To release a tamed animal:").color("#AAAAAA"));
         ctx.sendMessage(Message.raw("  Use: /hytame untame <animal-name>").color("#FFFFFF"));
         return CompletableFuture.completedFuture((Object)null);
      }
   }
}
