package com.hytame.commands;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.hypixel.hytale.server.spawning.world.WorldEnvironmentSpawnData;
import com.hypixel.hytale.server.spawning.world.WorldNPCSpawnStat;
import com.hypixel.hytale.server.spawning.world.component.WorldSpawnData;
import com.hytame.HyTamePlugin;
import com.hytame.listeners.DetectTamedDeath;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.PersistenceManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.TamedAnimalData;
import com.hytame.patch.PatchSyncService;
import com.hytame.tame.HyTameComponent;
import com.hytame.ui.ConfigPanelUIPage;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class HytameCommand extends AbstractCommand {
   public static final String PERM_ADMIN = "hytame.admin.";
   private static final Message DEPRECATION_WARNING = Message.raw("[Deprecated] Use /hytame instead. /breed will be removed in a future version.").color("#FFAA00");
   private static final Set<UUID> hytalorWarningShown = ConcurrentHashMap.newKeySet();

   public HytameCommand() {
      super("hytame", "Main command for HyTame");
      this.addSubCommand(new HytameHelpSubCommand());
      this.addSubCommand(new HytameStatusSubCommand());
      this.addSubCommand(new HytameInfoSubCommand());
      this.addSubCommand(new HytameTameSubCommand());
      this.addSubCommand(new HytameUntameSubCommand());
      this.addSubCommand(new HytameSettingsSubCommand());
      this.addSubCommand(new HytameScanSubCommand());
      this.addSubCommand(new HytameFoodsSubCommand());
      this.addSubCommand(new HytameConfigSubCommand());
      this.addSubCommand(new HytameGrowthSubCommand());
      this.addSubCommand(new HytameCustomSubCommand());
      this.addSubCommand(new HytameClearTamesSubCommand());
      this.addSubCommand(new HytameDebugSubCommand());
   }

   protected boolean canGeneratePermission() {
      return false;
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      if (checkHytalorWarning(ctx)) {
         return CompletableFuture.completedFuture((Object)null);
      } else {
         checkDeprecatedAlias(ctx);
         showHelp(ctx);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static void checkDeprecatedAlias(CommandContext ctx) {
      String input = ctx.getInputString();
      if (input != null && input.toLowerCase().startsWith("breed")) {
         ctx.sendMessage(DEPRECATION_WARNING);
         ctx.sendMessage(Message.raw(""));
      }

   }

   private static boolean checkAdminDenied(CommandContext ctx) {
      Player player = CommandUtil.player(ctx);
      if (player instanceof Player && !HytamePermissions.hasAdminAccess(player)) {
         ctx.sendMessage(Message.raw("This command requires admin permissions.").color("#FF5555"));
         return true;
      } else {
         return false;
      }
   }

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
               ctx.sendMessage(Message.raw(""));
               ctx.sendMessage(Message.raw("[WARNING] HYTALOR NOT DETECTED").color("#FF5555"));
               ctx.sendMessage(Message.raw("HyTame requires Hytalor to work.").color("#FFFFFF"));
               ctx.sendMessage(Message.raw("Without it, taming and breeding features will NOT work.").color("#AAAAAA"));
               ctx.sendMessage(Message.raw(""));
               ctx.sendMessage(Message.raw("Install Hytalor from:").color("#AAAAAA"));
               ctx.sendMessage(Message.raw("curseforge.com/hytale/mods/hytalor").color("#55FFFF"));
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

   private static void showHelp(CommandContext ctx) {
      ctx.sendMessage(Message.raw("=== HyTame - Animal Breeding & Taming ===").color("#FF9900"));
      ctx.sendMessage(Message.raw("Version: ").color("#AAAAAA").insert(Message.raw("1.5.3").color("#FFFFFF")));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("Player Commands:").color("#FFAA00"));
      ctx.sendMessage(Message.raw("/hytame help").color("#FFFFFF").insert(Message.raw(" - Show this help").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame status").color("#FFFFFF").insert(Message.raw(" - View tracked animals").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame info").color("#FFFFFF").insert(Message.raw(" - Show taming info").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame tame <name>").color("#FFFFFF").insert(Message.raw(" - Prepare to tame an animal").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame untame").color("#FFFFFF").insert(Message.raw(" - Release a tamed animal").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame list").color("#FFFFFF").insert(Message.raw(" - Browse taming & breeding settings").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame scan").color("#FFFFFF").insert(Message.raw(" - Scan for untracked babies").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame config info <animal>").color("#FFFFFF").insert(Message.raw(" - View animal details").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame config list").color("#FFFFFF").insert(Message.raw(" - List all animals").color("#AAAAAA")));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("Admin Commands ").color("#FFAA00").insert(Message.raw("(admin):").color("#888888")));
      ctx.sendMessage(Message.raw("/hytame config enable/disable").color("#FFFFFF").insert(Message.raw(" - Toggle breeding/taming").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame growth").color("#FFFFFF").insert(Message.raw(" - Toggle baby growth").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame custom ...").color("#FFFFFF").insert(Message.raw(" - Manage custom animals").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame clear [player]").color("#FFFFFF").insert(Message.raw(" - Remove all tames").color("#AAAAAA")));
      ctx.sendMessage(Message.raw("/hytame debug ...").color("#FFFFFF").insert(Message.raw(" - Debug commands").color("#AAAAAA")));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("Feed animals their favorite food to breed!").color("#55FF55"));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw(">>> ").color("#FFFF55").insert(Message.raw("/hytame foods").color("#55FFFF")).insert(Message.raw(" - Quick guide to all animal foods!").color("#FFFF55")));
   }

   public static class HytameHelpSubCommand extends AbstractCommand {
      public HytameHelpSubCommand() {
         super("help", "Show help information");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            HytameCommand.showHelp(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         HytameCommand.showHelp(ctx);
      }
   }

   public static class HytameStatusSubCommand extends AbstractCommand {
      public HytameStatusSubCommand() {
         super("status", "View tracked animals and breeding stats");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeStatusLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeStatusLogic(ctx);
      }

      private static void executeStatusLogic(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
         } else {
            BreedingManager breeding = plugin.getBreedingManager();
            TamingManager taming = plugin.getTamingManager();
            ctx.sendMessage(Message.raw("=== Breeding Status ===").color("#FF9900"));
            ctx.sendMessage(Message.raw("Animals tracked: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breeding.getTrackedCount())).color("#FFFFFF")));
            ctx.sendMessage(Message.raw("In love mode: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breeding.getInLoveCount())).color("#FF69B4")));
            ctx.sendMessage(Message.raw("Pregnant: ").color("#AAAAAA").insert(Message.raw(String.valueOf(breeding.getPregnantCount())).color("#FFFF55")));
            if (taming != null) {
               ctx.sendMessage(Message.raw("Tamed animals: ").color("#AAAAAA").insert(Message.raw(String.valueOf(taming.getTamedCount())).color("#55FF55")));
            }

         }
      }
   }

   public static class HytameInfoSubCommand extends AbstractCommand {
      public HytameInfoSubCommand() {
         super("info", "Show taming information");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeInfoLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeInfoLogic(ctx);
      }

      private static void executeInfoLogic(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getTamingManager() != null) {
            TamingManager taming = plugin.getTamingManager();
            ctx.sendMessage(Message.raw("=== Taming Status ===").color("#FF9900"));
            ctx.sendMessage(Message.raw("Total tamed: ").color("#AAAAAA").insert(Message.raw(String.valueOf(taming.getTamedCount())).color("#FFFFFF")));
            ctx.sendMessage(Message.raw("Awaiting respawn: ").color("#AAAAAA").insert(Message.raw(String.valueOf(taming.getDespawnedCount())).color("#FFFF55")));
         } else {
            ctx.sendMessage(Message.raw("Taming system not initialized!").color("#FF5555"));
         }
      }
   }

   public static class HytameTameSubCommand extends AbstractCommand {
      private final RequiredArg<String> nameArg;

      public HytameTameSubCommand() {
         super("tame", "[Deprecated] Use Name Tag item on animal instead");
         this.nameArg = this.withRequiredArg("name", "Name for the animal", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeTameLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeTameLogic(ctx);
      }

      private static void executeTameLogic(CommandContext ctx) {
         ctx.sendMessage(Message.raw("[Deprecated] This command has been replaced.").color("#FFAA00"));
         ctx.sendMessage(Message.raw("To tame an animal:").color("#AAAAAA"));
         ctx.sendMessage(Message.raw("  1. Hold a Name Tag item").color("#FFFFFF"));
         ctx.sendMessage(Message.raw("  2. Press Left Click on an animal").color("#FFFFFF"));
         ctx.sendMessage(Message.raw("  3. Enter a name in the UI").color("#FFFFFF"));
      }
   }

   public class HytameUntameSubCommand extends AbstractTargetEntityCommand {
      public HytameUntameSubCommand() {
         Objects.requireNonNull(HytameCommand.this);
         super("untame", "Release a tamed animal (look at it)");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected void execute(@Nonnull CommandContext ctx, @Nonnull List<Ref<EntityStore>> entities, @Nonnull World world, @Nonnull Store<EntityStore> store) {
         Player var6 = CommandUtil.player(ctx);
         if (!(var6 instanceof Player)) {
            ctx.sendMessage(Message.raw("This command can only be used by players.").color("#FF5555"));
         } else {
            Player player = var6;
            UUID var19 = null;

            try {
               Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();
               if (playerRef != null) {
                  UUIDComponent uuidComp = (UUIDComponent)store.getComponent(playerRef, UUIDComponent.getComponentType());
                  if (uuidComp != null) {
                     var19 = uuidComp.getUuid();
                  }
               }
            } catch (Exception var18) {
            }

            if (var19 == null) {
               ctx.sendMessage(Message.raw("Could not identify player.").color("#FF5555"));
            } else {
               boolean isAdmin = HytamePermissions.hasAdminAccess(player);
               HyTamePlugin plugin = HyTamePlugin.getInstance();
               TamingManager tamingManager = plugin != null ? plugin.getTamingManager() : null;
               if (tamingManager == null) {
                  ctx.sendMessage(Message.raw("TamingManager not available.").color("#FF5555"));
               } else {
                  for(Ref<EntityStore> entityRef : entities) {
                     HyTameComponent comp = (HyTameComponent)store.getComponent(entityRef, HyTameComponent.getComponentType());
                     if (comp != null && comp.isTamed()) {
                        UUID hytameId = comp.getHytameId();
                        if (hytameId == null) {
                           ctx.sendMessage(Message.raw("This animal has no HyTame ID.").color("#FF5555"));
                        } else {
                           UUID releaseAs = isAdmin ? comp.getTamerUUID() : var19;
                           if (releaseAs == null) {
                              releaseAs = var19;
                           }

                           boolean success = tamingManager.releaseAnimal(hytameId, releaseAs);
                           if (success) {
                              NPCEntity npcEntity = (NPCEntity)store.getComponent(entityRef, NPCEntity.getComponentType());
                              String roleName = npcEntity != null ? npcEntity.getRoleName() : "Animal";
                              ctx.sendMessage(Message.raw("Released " + roleName + ".").color("#55FF55"));
                           } else {
                              ctx.sendMessage(Message.raw("Cannot release: not the owner.").color("#FF5555"));
                           }
                        }
                     } else {
                        ctx.sendMessage(Message.raw("This animal is not tamed.").color("#FF5555"));
                     }
                  }

               }
            }
         }
      }
   }

   public static class HytameSettingsSubCommand extends AbstractCommand {
      public HytameSettingsSubCommand() {
         super("list", "Browse taming & breeding settings");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeSettingsLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeSettingsLogic(ctx);
      }

      private static void executeSettingsLogic(CommandContext ctx) {
         if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be used by players").color("#FF5555"));
         } else {
            Player player = CommandUtil.player(ctx);
            World world = Universe.get().getDefaultWorld();
            if (world == null) {
               ctx.sendMessage(Message.raw("World not available").color("#FF5555"));
            } else {
               boolean isAdmin = HytamePermissions.hasAdminAccess(player);
               world.execute(() -> {
                  try {
                     Ref<EntityStore> playerEntityRef = player.getReference();
                     Store<EntityStore> store = playerEntityRef.getStore();
                     ConfigPanelUIPage configPage = new ConfigPanelUIPage(player.getPlayerRef(), !isAdmin);
                     player.getPageManager().openCustomPage(playerEntityRef, store, configPage);
                  } catch (Exception e) {
                     player.getPlayerRef().sendMessage(Message.raw("Failed to open config panel: " + e.getMessage()).color("#FF5555"));
                  }

               });
            }
         }
      }
   }

   public static class HytameScanSubCommand extends AbstractCommand {
      public HytameScanSubCommand() {
         super("scan", "Scan for untracked baby animals");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeScanLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeScanLogic(ctx);
      }

      private static void executeScanLogic(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
         } else {
            ctx.sendMessage(Message.raw("Scanning for untracked babies...").color("#AAAAAA"));
            World world = Universe.get().getDefaultWorld();
            if (world != null) {
               world.execute(() -> {
                  int found = plugin.scanForUntrackedBabies();
                  if (found > 0) {
                     ctx.sendMessage(Message.raw("Found and registered ").color("#55FF55").insert(Message.raw(String.valueOf(found)).color("#FFFFFF")).insert(Message.raw(" untracked babies!").color("#55FF55")));
                  } else {
                     ctx.sendMessage(Message.raw("No untracked babies found.").color("#AAAAAA"));
                  }

                  BreedingManager breeding = plugin.getBreedingManager();
                  int babyCount = breeding.getTrackedBabyUuids().size();
                  ctx.sendMessage(Message.raw("Total tracked babies: ").color("#AAAAAA").insert(Message.raw(String.valueOf(babyCount)).color("#FFFFFF")));
               });
            } else {
               ctx.sendMessage(Message.raw("World not available!").color("#FF5555"));
            }

         }
      }
   }

   public static class HytameFoodsSubCommand extends AbstractCommand {
      public HytameFoodsSubCommand() {
         super("foods", "Quick reference for animal foods");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeFoodsLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeFoodsLogic(ctx);
      }

      private static void executeFoodsLogic(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         ConfigManager config = plugin != null ? plugin.getConfigManager() : null;
         ctx.sendMessage(Message.raw("=== Animal Food Reference ===").color("#FF9900"));
         ctx.sendMessage(Message.raw(""));
         if (config == null) {
            ctx.sendMessage(Message.raw("Error: Could not load configuration.").color("#FF5555"));
         } else {
            ctx.sendMessage(Message.raw("Enabled Animals:").color("#FFAA00"));
            int count = 0;

            for(AnimalType type : AnimalType.values()) {
               if (config.isBreedingEnabled(type) || config.isTamingEnabled(type)) {
                  List<String> foods = config.getBreedingFoods(type);
                  String foodList = BreedingConfigCommand.getFoodDisplayList(foods);
                  String status = "";
                  if (config.isBreedingEnabled(type) && config.isTamingEnabled(type)) {
                     status = " [B+T]";
                  } else if (config.isBreedingEnabled(type)) {
                     status = " [B]";
                  } else {
                     status = " [T]";
                  }

                  ctx.sendMessage(Message.raw("  " + type.getId()).color("#FFFFFF").insert(Message.raw(status).color("#888888")).insert(Message.raw(" - ").color("#555555")).insert(Message.raw(foodList).color("#55FF55")));
                  ++count;
               }
            }

            Map<String, CustomAnimalConfig> customAnimals = config.getCustomAnimals();
            if (customAnimals != null && !customAnimals.isEmpty()) {
               ctx.sendMessage(Message.raw(""));
               ctx.sendMessage(Message.raw("Custom Animals:").color("#FFAA00"));

               for(Map.Entry<String, CustomAnimalConfig> entry : customAnimals.entrySet()) {
                  CustomAnimalConfig custom = (CustomAnimalConfig)entry.getValue();
                  if (custom.isBreedingEnabled() || custom.isTamingEnabled()) {
                     List<String> foods = custom.getBreedingFoods();
                     String foodList = BreedingConfigCommand.getFoodDisplayList(foods);
                     String status = "";
                     if (custom.isBreedingEnabled() && custom.isTamingEnabled()) {
                        status = " [B+T]";
                     } else if (custom.isBreedingEnabled()) {
                        status = " [B]";
                     } else {
                        status = " [T]";
                     }

                     ctx.sendMessage(Message.raw("  " + (String)entry.getKey()).color("#FFFFFF").insert(Message.raw(status).color("#888888")).insert(Message.raw(" - ").color("#555555")).insert(Message.raw(foodList).color("#55FF55")));
                     ++count;
                  }
               }
            }

            if (count == 0) {
               ctx.sendMessage(Message.raw("  (no animals enabled)").color("#888888"));
            }

            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("Legend: ").color("#888888").insert(Message.raw("[B]").color("#AAAAAA")).insert(Message.raw("=Breeding ").color("#666666")).insert(Message.raw("[T]").color("#AAAAAA")).insert(Message.raw("=Taming ").color("#666666")).insert(Message.raw("[B+T]").color("#AAAAAA")).insert(Message.raw("=Both").color("#666666")));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("How to Tame:").color("#FFAA00"));
            ctx.sendMessage(Message.raw("  1. Hold a ").color("#AAAAAA").insert(Message.raw("Name Tag").color("#55FFFF")).insert(Message.raw(" item").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("  2. Press ").color("#AAAAAA").insert(Message.raw("F").color("#FFFF55")).insert(Message.raw(" on an animal").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("  3. Enter a name in the UI").color("#AAAAAA"));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("Commands:").color("#FFAA00"));
            ctx.sendMessage(Message.raw("  /hytame config info <animal>").color("#FFFFFF").insert(Message.raw(" - Full details").color("#AAAAAA")));
            ctx.sendMessage(Message.raw("  /hytame config list").color("#FFFFFF").insert(Message.raw(" - All animals").color("#AAAAAA")));
         }
      }
   }

   public static class HytameConfigSubCommand extends AbstractCommand {
      public HytameConfigSubCommand() {
         super("config", "Configuration commands (some require admin)");
         this.addSubCommand(new BreedingConfigCommand.ReloadSubCommand());
         this.addSubCommand(new BreedingConfigCommand.SaveSubCommand());
         this.addSubCommand(new BreedingConfigCommand.ListSubCommand());
         this.addSubCommand(new BreedingConfigCommand.InfoSubCommand());
         this.addSubCommand(new BreedingConfigCommand.EnableSubCommand());
         this.addSubCommand(new BreedingConfigCommand.DisableSubCommand());
         this.addSubCommand(new BreedingConfigCommand.EnableTamingSubCommand());
         this.addSubCommand(new BreedingConfigCommand.DisableTamingSubCommand());
         this.addSubCommand(new BreedingConfigCommand.SetSubCommand());
         this.addSubCommand(new BreedingConfigCommand.AddFoodSubCommand());
         this.addSubCommand(new BreedingConfigCommand.RemoveFoodSubCommand());
         this.addSubCommand(new BreedingConfigCommand.PresetSubCommand());
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeConfigLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeConfigLogic(ctx);
      }

      private static void executeConfigLogic(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getConfigManager() != null) {
            ctx.sendMessage(Message.raw("=== HyTame Config ===").color("#FF9900"));
            ctx.sendMessage(Message.raw("Active Preset: ").color("#AAAAAA").insert(Message.raw(plugin.getConfigManager().getActivePreset()).color("#FFFFFF")));
            ctx.sendMessage(Message.raw("Type ").color("#AAAAAA").insert(Message.raw("/hytame config").color("#FFFFFF")).insert(Message.raw(" and press TAB for subcommands").color("#AAAAAA")));
         } else {
            ctx.sendMessage(Message.raw("Config not loaded!").color("#FF5555"));
         }
      }
   }

   public static class HytameGrowthSubCommand extends AbstractCommand {
      public HytameGrowthSubCommand() {
         super("growth", "Toggle baby animal growth (admin)");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeGrowthLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         if (!HytameCommand.checkAdminDenied(ctx)) {
            executeGrowthLogic(ctx);
         }
      }

      private static void executeGrowthLogic(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getConfigManager() != null) {
            ConfigManager config = plugin.getConfigManager();
            boolean current = config.isGrowthEnabled();
            config.setGrowthEnabled(!current);
            if (config.isGrowthEnabled()) {
               ctx.sendMessage(Message.raw("Baby growth: ").color("#AAAAAA").insert(Message.raw("ENABLED").color("#55FF55")));
               ctx.sendMessage(Message.raw("Babies will grow into adults over time.").color("#AAAAAA"));
            } else {
               ctx.sendMessage(Message.raw("Baby growth: ").color("#AAAAAA").insert(Message.raw("DISABLED").color("#FF5555")));
               ctx.sendMessage(Message.raw("Babies will stay babies forever!").color("#AAAAAA"));
            }

         } else {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
         }
      }
   }

   public static class HytameCustomSubCommand extends AbstractCommand {
      public HytameCustomSubCommand() {
         super("custom", "Manage custom animals from other mods");
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalAddCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalRemoveCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalListCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalInfoCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalEnableCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalDisableCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalAddFoodCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalRemoveFoodCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalScanCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalSetRoleCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalSetBabyCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalSetGrowthCommand());
         this.addSubCommand(new CustomAnimalCommand.CustomAnimalSetCooldownCommand());
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeCustomLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         executeCustomLogic(ctx);
      }

      private static void executeCustomLogic(CommandContext ctx) {
         ctx.sendMessage(Message.raw("=== Custom Animal Commands ===").color("#FF9900"));
         ctx.sendMessage(Message.raw("/hytame custom scan").color("#FFFFFF").insert(Message.raw(" - Find creature names in world").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame custom add <role> <food>").color("#FFFFFF").insert(Message.raw(" - Register custom animal").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame custom remove <model>").color("#FFFFFF").insert(Message.raw(" - Remove custom animal").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame custom list").color("#FFFFFF").insert(Message.raw(" - List added custom animals").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame custom setrole <model> <role>").color("#FFFFFF").insert(Message.raw(" - Set NPC role for spawning").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame custom setbaby <model> <babyRole>").color("#FFFFFF").insert(Message.raw(" - Set baby NPC role").color("#AAAAAA")));
         ctx.sendMessage(Message.raw(""));
         ctx.sendMessage(Message.raw("Use /hytame config for info, foods, growth, cooldown:").color("#FFAA00"));
         ctx.sendMessage(Message.raw("/hytame config info <animal>").color("#FFFFFF").insert(Message.raw(" - Show details").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame config addfood <animal> <food>").color("#FFFFFF").insert(Message.raw(" - Add food").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame config set <animal> growth <min>").color("#FFFFFF").insert(Message.raw(" - Set growth").color("#AAAAAA")));
         ctx.sendMessage(Message.raw(""));
         ctx.sendMessage(Message.raw("Run ").color("#AAAAAA").insert(Message.raw("/hytame custom scan").color("#FFFF55")).insert(Message.raw(" first to find creature names!").color("#AAAAAA")));
      }
   }

   public static class HytameClearTamesSubCommand extends AbstractCommand {
      private final OptionalArg<String> playerArg;

      public HytameClearTamesSubCommand() {
         super("clear", "Remove all tamed animals (admin)");
         this.playerArg = this.withOptionalArg("player", "Player name to clear tames for (omit for all)", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getTamingManager() != null) {
               TamingManager taming = plugin.getTamingManager();
               String playerName = (String)ctx.get(this.playerArg);
               if (playerName != null && !playerName.isEmpty()) {
                  int removed = taming.clearPlayerTamesByName(playerName);
                  if (removed > 0) {
                     ctx.sendMessage(Message.raw("Cleared ").color("#55FF55").insert(Message.raw(String.valueOf(removed)).color("#FFFFFF")).insert(Message.raw(" tamed animals for ").color("#55FF55")).insert(Message.raw(playerName).color("#FFFF55")));
                  } else {
                     ctx.sendMessage(Message.raw("No tamed animals found for player ").color("#FFAA00").insert(Message.raw(playerName).color("#FFFF55")));
                  }
               } else {
                  int removed = taming.clearAllTames();
                  ctx.sendMessage(Message.raw("Cleared all ").color("#55FF55").insert(Message.raw(String.valueOf(removed)).color("#FFFFFF")).insert(Message.raw(" tamed animals.").color("#55FF55")));
               }

               return CompletableFuture.completedFuture((Object)null);
            } else {
               ctx.sendMessage(Message.raw("Taming system not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class HytameDebugSubCommand extends AbstractCommand {
      public HytameDebugSubCommand() {
         super("debug", "Debug commands for taming system (admin)");
         this.addSubCommand(new DebugLogSubCommand());
         this.addSubCommand(new DebugMemorySubCommand());
         this.addSubCommand(new DebugFileSubCommand());
         this.addSubCommand(new DebugEventsSubCommand());
         this.addSubCommand(new DebugClearSubCommand());
         this.addSubCommand(new DebugTameStatusCommand());
         this.addSubCommand(new DebugSpawnAllSubCommand());
         this.addSubCommand(new DebugSyncSubCommand());
         this.addSubCommand(new DebugModPathsSubCommand());
         this.addSubCommand(new DebugOverpopSubCommand());
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (HytameCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else if (HytameCommand.checkHytalorWarning(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HytameCommand.checkDeprecatedAlias(ctx);
            executeDebugLogic(ctx);
            return CompletableFuture.completedFuture((Object)null);
         }
      }

      public void executeFromDeprecated(CommandContext ctx) {
         if (!HytameCommand.checkAdminDenied(ctx)) {
            executeDebugLogic(ctx);
         }
      }

      private static void executeDebugLogic(CommandContext ctx) {
         ctx.sendMessage(Message.raw("=== Debug Commands ===").color("#FF9900"));
         ctx.sendMessage(Message.raw("/hytame debug log").color("#FFFFFF").insert(Message.raw(" - Toggle verbose logging").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug memory").color("#FFFFFF").insert(Message.raw(" - Log tamed animals in memory").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug file").color("#FFFFFF").insert(Message.raw(" - Log tamed animals from save file").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug events").color("#FFFFFF").insert(Message.raw(" - Log last detected death/despawn UUIDs").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug clear").color("#FFFFFF").insert(Message.raw(" - Clear tracked event UUIDs").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug tameStatus").color("#FFFFFF").insert(Message.raw(" - Get target npcs tame status").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug spawnAll").color("#FFFFFF").insert(Message.raw(" - Spawn all animal types near you (1s delay each)").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame debug sync").color("#FFFFFF").insert(Message.raw(" - Force sync all config patches to asset pack").color("#AAAAAA")));
      }

      public static class DebugLogSubCommand extends AbstractCommand {
         public DebugLogSubCommand() {
            super("log", "Toggle verbose logging");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
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
      }

      public static class DebugMemorySubCommand extends AbstractCommand {
         public DebugMemorySubCommand() {
            super("memory", "Log tamed animals currently in memory");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else if (HytameCommand.checkHytalorWarning(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               HytameCommand.checkDeprecatedAlias(ctx);
               HyTamePlugin plugin = HyTamePlugin.getInstance();
               if (plugin != null && plugin.getTamingManager() != null) {
                  TamingManager taming = plugin.getTamingManager();
                  Collection<TamedAnimalData> animals = taming.getAllTamedAnimals();
                  ctx.sendMessage(Message.raw("=== Tamed Animals in Memory ===").color("#FF9900"));
                  ctx.sendMessage(Message.raw("Total: ").color("#AAAAAA").insert(Message.raw(String.valueOf(animals.size())).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw(""));
                  if (animals.isEmpty()) {
                     ctx.sendMessage(Message.raw("No tamed animals in memory.").color("#AAAAAA"));
                  } else {
                     int index = 1;

                     for(TamedAnimalData data : animals) {
                        String status = data.isDead() ? "[DEAD]" : (data.isDespawned() ? "[DESPAWNED]" : "[ACTIVE]");
                        String statusColor = data.isDead() ? "#FF5555" : (data.isDespawned() ? "#FFFF55" : "#55FF55");
                        ctx.sendMessage(Message.raw(index + ". ").color("#AAAAAA").insert(Message.raw(data.getCustomName()).color("#FFFFFF")).insert(Message.raw(" ").color("#AAAAAA")).insert(Message.raw(status).color(statusColor)));
                        ctx.sendMessage(Message.raw("   UUID: ").color("#AAAAAA").insert(Message.raw(data.getAnimalUuid().toString()).color("#888888")));
                        String typeStr = data.getAnimalType() != null ? data.getAnimalType().name() : "CUSTOM";
                        ctx.sendMessage(Message.raw("   Type: ").color("#AAAAAA").insert(Message.raw(typeStr).color("#FFFFFF")));
                        ++index;
                     }
                  }

                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }

      public static class DebugFileSubCommand extends AbstractCommand {
         public DebugFileSubCommand() {
            super("file", "Log tamed animals from save file");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else if (HytameCommand.checkHytalorWarning(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               HytameCommand.checkDeprecatedAlias(ctx);
               HyTamePlugin plugin = HyTamePlugin.getInstance();
               if (plugin != null && plugin.getPersistenceManager() != null) {
                  PersistenceManager persistence = plugin.getPersistenceManager();
                  List<TamedAnimalData> animals = persistence.loadData();
                  ctx.sendMessage(Message.raw("=== Tamed Animals in File ===").color("#FF9900"));
                  ctx.sendMessage(Message.raw("File: ").color("#AAAAAA").insert(Message.raw(persistence.getSaveFilePath().toString()).color("#888888")));
                  ctx.sendMessage(Message.raw("Total: ").color("#AAAAAA").insert(Message.raw(String.valueOf(animals.size())).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw(""));
                  if (animals.isEmpty()) {
                     ctx.sendMessage(Message.raw("No tamed animals in save file.").color("#AAAAAA"));
                  } else {
                     int index = 1;

                     for(TamedAnimalData data : animals) {
                        String status = data.isDead() ? "[DEAD]" : (data.isDespawned() ? "[DESPAWNED]" : "[ACTIVE]");
                        String statusColor = data.isDead() ? "#FF5555" : (data.isDespawned() ? "#FFFF55" : "#55FF55");
                        ctx.sendMessage(Message.raw(index + ". ").color("#AAAAAA").insert(Message.raw(data.getCustomName()).color("#FFFFFF")).insert(Message.raw(" ").color("#AAAAAA")).insert(Message.raw(status).color(statusColor)));
                        ctx.sendMessage(Message.raw("   UUID: ").color("#AAAAAA").insert(Message.raw(data.getAnimalUuid().toString()).color("#888888")));
                        String typeStr = data.getAnimalType() != null ? data.getAnimalType().name() : "CUSTOM";
                        ctx.sendMessage(Message.raw("   Type: ").color("#AAAAAA").insert(Message.raw(typeStr).color("#FFFFFF")));
                        ++index;
                     }
                  }

                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }

      public static class DebugEventsSubCommand extends AbstractCommand {
         public DebugEventsSubCommand() {
            super("events", "Log last detected death and despawn UUIDs");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else if (HytameCommand.checkHytalorWarning(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               HytameCommand.checkDeprecatedAlias(ctx);
               ctx.sendMessage(Message.raw("=== Last Detected Events ===").color("#FF9900"));
               List<UUID> deaths = DetectTamedDeath.getLastDetectedDeaths();
               ctx.sendMessage(Message.raw(""));
               ctx.sendMessage(Message.raw("Deaths (last " + deaths.size() + "):").color("#FF5555"));
               if (deaths.isEmpty()) {
                  ctx.sendMessage(Message.raw("  None").color("#AAAAAA"));
               } else {
                  for(int i = 0; i < deaths.size(); ++i) {
                     ctx.sendMessage(Message.raw("  " + (i + 1) + ". ").color("#AAAAAA").insert(Message.raw(((UUID)deaths.get(i)).toString()).color("#888888")));
                  }
               }

               List<UUID> despawns = HyTamePlugin.getLastDetectedDespawns();
               ctx.sendMessage(Message.raw(""));
               ctx.sendMessage(Message.raw("Despawns (last " + despawns.size() + "):").color("#FFFF55"));
               if (despawns.isEmpty()) {
                  ctx.sendMessage(Message.raw("  None").color("#AAAAAA"));
               } else {
                  for(int i = 0; i < despawns.size(); ++i) {
                     ctx.sendMessage(Message.raw("  " + (i + 1) + ". ").color("#AAAAAA").insert(Message.raw(((UUID)despawns.get(i)).toString()).color("#888888")));
                  }
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }

      public static class DebugClearSubCommand extends AbstractCommand {
         public DebugClearSubCommand() {
            super("clear", "Clear tracked event UUIDs");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else if (HytameCommand.checkHytalorWarning(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               HytameCommand.checkDeprecatedAlias(ctx);
               DetectTamedDeath.clearTrackedDeaths();
               HyTamePlugin.clearTrackedDespawns();
               ctx.sendMessage(Message.raw("Cleared tracked death and despawn UUIDs.").color("#55FF55"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }

      public class DebugTameStatusCommand extends AbstractTargetEntityCommand {
         public DebugTameStatusCommand() {
            Objects.requireNonNull(HytameDebugSubCommand.this);
            super("TameStatus", "Displays NPC's tame status");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected void execute(@Nonnull CommandContext context, @Nonnull List<Ref<EntityStore>> entities, @Nonnull World world, @Nonnull Store<EntityStore> store) {
            Player player = CommandUtil.player(context);
            if (player instanceof Player && !HytamePermissions.hasAdminAccess(player)) {
               context.sendMessage(Message.raw("This command requires admin permissions.").color("#FF5555"));
            } else {
               Ref<EntityStore> playerRef = context.senderAsPlayerRef();
               if (playerRef != null) {
                  for(Ref<EntityStore> entityRef : entities) {
                     ComponentType<EntityStore, NPCEntity> componentType = NPCEntity.getComponentType();
                     if (componentType != null) {
                        NPCEntity npcComponent = (NPCEntity)store.getComponent(entityRef, componentType);
                        if (npcComponent != null) {
                           Role role = npcComponent.getRole();
                           if (role != null) {
                              WorldSupport worldSupport = role.getWorldSupport();
                              Attitude defaultAttitude = worldSupport.getDefaultPlayerAttitude();

                              Attitude currentAttitude;
                              try {
                                 currentAttitude = worldSupport.getAttitude(entityRef, playerRef, store);
                              } catch (NullPointerException var17) {
                                 context.sendMessage(Message.raw(role.getRoleName() + " attitude not initialized"));
                                 continue;
                              }

                              HyTameComponent tameComponent = (HyTameComponent)store.getComponent(entityRef, HyTameComponent.getComponentType());
                              String var10000 = String.valueOf(defaultAttitude);
                              String attitudeStatus = "Attitude: Default(" + var10000 + "), Current(" + String.valueOf(currentAttitude) + ")";
                              String tameStatus = tameComponent != null ? "Tamed: Status(" + tameComponent.isTamed() + "), Owner(" + tameComponent.getTamerName() + ")" : "Cannot be tamed";
                              context.sendMessage(Message.raw(npcComponent.getRoleName()).insert(attitudeStatus).insert(tameStatus));
                           }
                        }
                     }
                  }

               }
            }
         }
      }

      public static class DebugSyncSubCommand extends AbstractCommand {
         public DebugSyncSubCommand() {
            super("sync", "Force sync all config patches");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               HyTamePlugin plugin = HyTamePlugin.getInstance();
               PatchSyncService patchSync = plugin.getPatchSyncService();
               if (patchSync == null) {
                  ctx.sendMessage(Message.raw("PatchSyncService not available!").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("Force syncing all patches...").color("#FF9900"));
                  patchSync.forceSyncAllPatches();
                  ctx.sendMessage(Message.raw("Patch sync complete!").color("#55FF55"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }

      public static class DebugModPathsSubCommand extends AbstractCommand {
         public DebugModPathsSubCommand() {
            super("modpaths", "Debug mod path resolution for a role");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String[] testRoles = new String[]{"Cow", "Bear_Panda", "Baboon"};

               for(String role : testRoles) {
                  String debug = EcsReflectionUtil.debugModPaths(role);

                  for(String line : debug.split("\n")) {
                     ctx.sendMessage(Message.raw(line).color("#AAAAAA"));
                  }

                  ctx.sendMessage(Message.raw("---").color("#555555"));
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }

      public static class DebugOverpopSubCommand extends AbstractCommand {
         public DebugOverpopSubCommand() {
            super("overpop", "Force overpopulation despawn on nearby wild NPCs");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               Player var3 = CommandUtil.player(ctx);
               if (var3 instanceof Player) {
                  World world = Universe.get().getDefaultWorld();
                  if (world == null) {
                     ctx.sendMessage(Message.raw("World not available!").color("#FF5555"));
                     return CompletableFuture.completedFuture((Object)null);
                  } else {
                     ctx.sendMessage(Message.raw("=== Overpopulation Despawn Test ===").color("#FF9900"));
                     world.execute(() -> {
                        try {
                           Store<EntityStore> store = world.getEntityStore().getStore();
                           ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
                           ComponentType<EntityStore, HyTameComponent> hyTameType = HyTameComponent.getComponentType();
                           if (npcType == null) {
                              return;
                           }

                           int[] refSpawnConfig = new int[]{Integer.MIN_VALUE};
                           int[] refEnvironment = new int[]{Integer.MIN_VALUE};
                           store.forEachChunk((chunk, buf) -> {
                              for(int i = 0; i < chunk.size(); ++i) {
                                 if (refSpawnConfig[0] != Integer.MIN_VALUE) {
                                    return;
                                 }

                                 NPCEntity npc = (NPCEntity)chunk.getComponent(i, npcType);
                                 if (npc != null && npc.getSpawnConfiguration() != Integer.MIN_VALUE && npc.getEnvironment() != Integer.MIN_VALUE) {
                                    refSpawnConfig[0] = npc.getSpawnConfiguration();
                                    refEnvironment[0] = npc.getEnvironment();
                                 }
                              }

                           });
                           if (refSpawnConfig[0] == Integer.MIN_VALUE) {
                              ctx.sendMessage(Message.raw("No natural NPCs found with valid spawn config! Go near wild animals first.").color("#FF5555"));
                              return;
                           }

                           int[] markedCount = new int[]{0};
                           int[] tamedCount = new int[]{0};
                           int[] alreadyValidCount = new int[]{0};
                           Map<Integer, Integer> markedPerRole = new HashMap();
                           store.forEachChunk((chunk, buf) -> {
                              for(int i = 0; i < chunk.size(); ++i) {
                                 NPCEntity npc = (NPCEntity)chunk.getComponent(i, npcType);
                                 if (npc != null) {
                                    if (hyTameType != null) {
                                       HyTameComponent hyTame = (HyTameComponent)chunk.getComponent(i, hyTameType);
                                       if (hyTame != null && hyTame.isTamed()) {
                                          int var15 = tamedCount[0]++;
                                          continue;
                                       }
                                    }

                                    if (npc.getSpawnConfiguration() != Integer.MIN_VALUE) {
                                       int var10002 = alreadyValidCount[0]++;
                                       markedPerRole.merge(npc.getRoleIndex(), 0, Integer::sum);
                                    } else {
                                       try {
                                          npc.setSpawnConfiguration(refSpawnConfig[0]);
                                          npc.setEnvironment(refEnvironment[0]);
                                          int var14 = markedCount[0]++;
                                          markedPerRole.merge(npc.getRoleIndex(), 1, Integer::sum);
                                       } catch (Exception var13) {
                                       }
                                    }
                                 }
                              }

                           });
                           int totalInflated = 0;

                           try {
                              WorldSpawnData worldSpawnData = (WorldSpawnData)store.getResource(WorldSpawnData.getResourceType());
                              WorldEnvironmentSpawnData envData = worldSpawnData.getWorldEnvironmentSpawnData(refEnvironment[0]);
                              if (envData != null) {
                                 for(Map.Entry<Integer, Integer> entry : markedPerRole.entrySet()) {
                                    int roleIdx = (Integer)entry.getKey();
                                    int addCount = (Integer)entry.getValue();
                                    if (addCount > 0) {
                                       WorldNPCSpawnStat stat = (WorldNPCSpawnStat)envData.getNpcStatMap().get(roleIdx);
                                       if (stat != null) {
                                          stat.adjustActual(addCount);
                                          totalInflated += addCount;
                                          String roleName = NPCPlugin.get().getName(roleIdx);
                                          double threshold = stat.getExpected() * (double)1.25F + (double)4.0F;
                                          Message var10001 = Message.raw("  " + roleName + ": ").color("#AAAAAA");
                                          int var10002 = stat.getActual();
                                          var10001 = var10001.insert(Message.raw("actual=" + var10002).color("#FFFFFF"));
                                          String var26 = String.format("%.1f", stat.getExpected());
                                          var10001 = var10001.insert(Message.raw(" expected=" + var26).color("#AAAAAA"));
                                          var26 = String.format("%.1f", threshold);
                                          ctx.sendMessage(var10001.insert(Message.raw(" threshold=" + var26).color((double)stat.getActual() > threshold ? "#FF5555" : "#55FF55")));
                                       } else {
                                          ctx.sendMessage(Message.raw("  " + NPCPlugin.get().getName(roleIdx) + ": no spawn stat in this environment (role may not naturally spawn here)").color("#FFAA00"));
                                       }
                                    }
                                 }
                              }
                           } catch (Exception e) {
                              ctx.sendMessage(Message.raw("Failed to inflate stats: " + e.getMessage()).color("#FF5555"));
                           }

                           ctx.sendMessage(Message.raw(""));
                           ctx.sendMessage(Message.raw("Marked ").color("#AAAAAA").insert(Message.raw(String.valueOf(markedCount[0])).color("#FFFFFF")).insert(Message.raw(" wild NPCs as despawnable").color("#AAAAAA")));
                           ctx.sendMessage(Message.raw("Already valid: ").color("#AAAAAA").insert(Message.raw(String.valueOf(alreadyValidCount[0])).color("#FFFFFF")));
                           ctx.sendMessage(Message.raw("Tamed (protected): ").color("#AAAAAA").insert(Message.raw(String.valueOf(tamedCount[0])).color("#55FF55")));
                           ctx.sendMessage(Message.raw("Inflated actual count by: ").color("#AAAAAA").insert(Message.raw(String.valueOf(totalInflated)).color("#FF5555")));
                           ctx.sendMessage(Message.raw(""));
                           ctx.sendMessage(Message.raw("Despawn check runs every 30s. Wild NPCs above threshold will be removed.").color("#AAAAAA"));
                           ctx.sendMessage(Message.raw("Tamed NPCs have spawnConfig=MIN_VALUE and are immune.").color("#55FF55"));
                        } catch (Exception e) {
                           ctx.sendMessage(Message.raw("Error: " + e.getMessage()).color("#FF5555"));
                        }

                     });
                     return CompletableFuture.completedFuture((Object)null);
                  }
               } else {
                  ctx.sendMessage(Message.raw("Must be run by a player.").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }

      public static class DebugSpawnAllSubCommand extends AbstractCommand {
         public DebugSpawnAllSubCommand() {
            super("spawnAll", "Spawn all animal types near you");
         }

         protected boolean canGeneratePermission() {
            return false;
         }

         protected CompletableFuture<Void> execute(CommandContext ctx) {
            if (HytameCommand.checkAdminDenied(ctx)) {
               return CompletableFuture.completedFuture((Object)null);
            } else {
               Player player = CommandUtil.player(ctx);
               if (player instanceof Player) {
                  World world = Universe.get().getDefaultWorld();
                  if (world == null) {
                     ctx.sendMessage(Message.raw("World not available!").color("#FF5555"));
                     return CompletableFuture.completedFuture((Object)null);
                  } else {
                     Vector3d playerPos = EntityUtil.getEntityPosition(player);
                     if (playerPos == null) {
                        ctx.sendMessage(Message.raw("Could not get player position!").color("#FF5555"));
                        return CompletableFuture.completedFuture((Object)null);
                     } else {
                        List<AnimalType> spawnable = new ArrayList();

                        for(AnimalType type : AnimalType.values()) {
                           if (type.getAdultNpcRoleId() != null) {
                              spawnable.add(type);
                           }
                        }

                        ctx.sendMessage(Message.raw("Spawning " + spawnable.size() + " animal types (200ms delay each)...").color("#FF9900"));
                        double baseX = playerPos.x();
                        double baseY = playerPos.y();
                        double baseZ = playerPos.z();
                        (new Thread(() -> {
                           int spawned = 0;
                           int failed = 0;

                           for(int i = 0; i < spawnable.size(); ++i) {
                              AnimalType type = (AnimalType)spawnable.get(i);
                              String roleId = type.getAdultNpcRoleId();
                              int col = i % 10;
                              int row = i / 10;
                              double x = baseX + (double)(col * 5) - (double)22.5F;
                              double z = baseZ + (double)(row * 5) + (double)5.0F;

                              try {
                                 int roleIndex = NPCPlugin.get().getIndex(roleId);
                                 if (roleIndex < 0) {
                                    ++failed;
                                 } else {
                                    try {
                                       NPCPlugin.get().validateSpawnableRole(roleId);
                                    } catch (Exception var24) {
                                       ++failed;
                                       continue;
                                    }

                                    Vector3d spawnPos = new Vector3d(x, baseY, z);
                                    Rotation3f rotation = new Rotation3f(0.0F, 0.0F, 0.0F);
                                    world.execute(() -> {
                                       try {
                                          NPCPlugin.get().spawnEntity(world.getEntityStore().getStore(), roleIndex, spawnPos, rotation, (Model)null, (TriConsumer)null);
                                       } catch (Exception e) {
                                          String var10001 = type.name();
                                          ctx.sendMessage(Message.raw("  [FAIL] " + var10001 + ": " + e.getMessage()).color("#FF5555"));
                                       }

                                    });
                                    ++spawned;
                                    ctx.sendMessage(Message.raw("  [" + (i + 1) + "/" + spawnable.size() + "] Spawned ").color("#AAAAAA").insert(Message.raw(type.name()).color("#55FF55")));
                                    Thread.sleep(200L);
                                 }
                              } catch (Exception e) {
                                 String var10001 = type.name();
                                 ctx.sendMessage(Message.raw("  [FAIL] " + var10001 + ": " + e.getMessage()).color("#FF5555"));
                                 ++failed;
                              }
                           }

                           ctx.sendMessage(Message.raw("Done! Spawned " + spawned + " animals" + (failed > 0 ? ", " + failed + " failed" : "")).color("#FF9900"));
                        }, "HyTame-SpawnAll")).start();
                        return CompletableFuture.completedFuture((Object)null);
                     }
                  }
               } else {
                  ctx.sendMessage(Message.raw("Must be run by a player.").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }
   }
}
