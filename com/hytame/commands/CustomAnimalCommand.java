package com.hytame.commands;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.patch.PatchSyncService;
import com.hytame.util.AnimalFinder;
import com.hytame.util.EcsReflectionUtil;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.joml.Vector3d;

public class CustomAnimalCommand extends AbstractCommand {
   private static final String PERM_ADMIN = "hytame.admin.";

   private static boolean checkAdminDenied(CommandContext ctx) {
      Player player = CommandUtil.player(ctx);
      if (player instanceof Player && !HytamePermissions.hasAdminAccess(player)) {
         ctx.sendMessage(Message.raw("This command requires admin permissions.").color("#FF5555"));
         return true;
      } else {
         return false;
      }
   }

   private static void syncCustomAnimalPatch(HyTamePlugin plugin, String modelAssetId) {
      PatchSyncService patchSyncService = plugin.getPatchSyncService();
      if (patchSyncService != null) {
         CustomAnimalConfig custom = plugin.getConfigManager().getCustomAnimal(modelAssetId);
         if (custom != null) {
            patchSyncService.syncForCustomAnimal(custom);
         }
      }
   }

   public CustomAnimalCommand() {
      super("customanimal", "[Deprecated] Manage custom animals - Use /hytame custom instead");
      this.addSubCommand(new CustomAnimalAddCommand());
      this.addSubCommand(new CustomAnimalRemoveCommand());
      this.addSubCommand(new CustomAnimalListCommand());
      this.addSubCommand(new CustomAnimalInfoCommand());
      this.addSubCommand(new CustomAnimalEnableCommand());
      this.addSubCommand(new CustomAnimalDisableCommand());
      this.addSubCommand(new CustomAnimalAddFoodCommand());
      this.addSubCommand(new CustomAnimalRemoveFoodCommand());
      this.addSubCommand(new CustomAnimalScanCommand());
      this.addSubCommand(new CustomAnimalSetRoleCommand());
      this.addSubCommand(new CustomAnimalSetBabyCommand());
      this.addSubCommand(new CustomAnimalSetGrowthCommand());
      this.addSubCommand(new CustomAnimalSetCooldownCommand());
   }

   protected boolean canGeneratePermission() {
      return false;
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw("[Deprecated] Use /hytame custom instead").color("#FFAA00"));
      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("=== Custom Animal Commands ===").color("#FF9900"));
      ctx.sendMessage(Message.raw("/customanimal add <model> <food> ").color("#AAAAAA").insert(Message.raw("- Add custom animal").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("/customanimal remove <model> ").color("#AAAAAA").insert(Message.raw("- Remove custom animal").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("/customanimal list ").color("#AAAAAA").insert(Message.raw("- List all custom animals").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("/customanimal info <model> ").color("#AAAAAA").insert(Message.raw("- Show custom animal info").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("/customanimal enable/disable <model> ").color("#AAAAAA").insert(Message.raw("- Toggle enabled").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("/customanimal addfood/removefood <model> <food> ").color("#AAAAAA").insert(Message.raw("- Modify foods").color("#FFFFFF")));
      ctx.sendMessage(Message.raw("Use /hytame config save after changes to persist!").color("#FFAA00"));
      return CompletableFuture.completedFuture((Object)null);
   }

   public static class CustomAnimalAddCommand extends AbstractCommand {
      private final RequiredArg<String> roleArg;
      private final RequiredArg<String> food1Arg;
      private final OptionalArg<String> food2Arg;
      private final OptionalArg<String> food3Arg;

      public CustomAnimalAddCommand() {
         super("add", "Add a custom animal by NPC role");
         this.roleArg = this.withRequiredArg("npcRole", "NPC role name (validates and auto-discovers model)", ArgTypes.STRING);
         this.food1Arg = this.withRequiredArg("food1", "Primary breeding food item ID", ArgTypes.STRING);
         this.food2Arg = this.withOptionalArg("food2", "Optional second food", ArgTypes.STRING);
         this.food3Arg = this.withOptionalArg("food3", "Optional third food", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String roleName = (String)ctx.get(this.roleArg);
               String food1 = BreedingConfigCommand.resolveFoodShortcut((String)ctx.get(this.food1Arg));
               List<String> foods = new ArrayList();
               foods.add(food1);
               String food2 = (String)ctx.get(this.food2Arg);
               if (food2 != null && !food2.isEmpty()) {
                  foods.add(BreedingConfigCommand.resolveFoodShortcut(food2));
               }

               String food3 = (String)ctx.get(this.food3Arg);
               if (food3 != null && !food3.isEmpty()) {
                  foods.add(BreedingConfigCommand.resolveFoodShortcut(food3));
               }

               NPCPlugin npcPlugin = NPCPlugin.get();
               int roleIndex = npcPlugin.getIndex(roleName);
               if (roleIndex < 0) {
                  ctx.sendMessage(Message.raw("NPC role not found: " + roleName).color("#FF5555"));
                  ctx.sendMessage(Message.raw("Make sure this is a valid NPC role name.").color("#AAAAAA"));
                  ctx.sendMessage(Message.raw("Use /hytame custom scan to find creatures nearby.").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("Discovering model for role: " + roleName + "...").color("#AAAAAA"));
                  String modelAssetId = this.discoverModelFromRole(plugin, roleName, roleIndex);
                  if (modelAssetId == null) {
                     ctx.sendMessage(Message.raw("Could not determine model for role: " + roleName).color("#FF5555"));
                     ctx.sendMessage(Message.raw("The role exists but model discovery failed.").color("#AAAAAA"));
                     return CompletableFuture.completedFuture((Object)null);
                  } else if (plugin.getConfigManager().isCustomAnimal(modelAssetId)) {
                     ctx.sendMessage(Message.raw("Model '" + modelAssetId + "' already registered!").color("#FFAA00"));
                     ctx.sendMessage(Message.raw("Use /hytame custom remove" + modelAssetId + " first.").color("#AAAAAA"));
                     return CompletableFuture.completedFuture((Object)null);
                  } else {
                     plugin.getConfigManager().addCustomAnimal(modelAssetId, foods);
                     plugin.getConfigManager().setCustomAnimalNpcRole(modelAssetId, roleName);
                     CustomAnimalCommand.syncCustomAnimalPatch(plugin, modelAssetId);
                     ctx.sendMessage(Message.raw("Added custom animal!").color("#55FF55"));
                     ctx.sendMessage(Message.raw("  NPC Role: ").color("#AAAAAA").insert(Message.raw(roleName).color("#FFFFFF")));
                     ctx.sendMessage(Message.raw("  Model: ").color("#AAAAAA").insert(Message.raw(modelAssetId).color("#FFFFFF")));
                     ctx.sendMessage(Message.raw("  Foods: ").color("#AAAAAA").insert(Message.raw(String.join(", ", foods)).color("#FFFFFF")));
                     ctx.sendMessage(Message.raw("Scanning world for creatures...").color("#AAAAAA"));
                     plugin.autoSetupNearbyAnimals();
                     ctx.sendMessage(Message.raw("Interactions set up! Feed the creature to breed.").color("#55FF55"));
                     ctx.sendMessage(Message.raw("Use ").color("#AAAAAA").insert(Message.raw("/hytame config save").color("#FFFFFF")).insert(Message.raw(" to persist changes.").color("#AAAAAA")));
                     ctx.sendMessage(Message.raw("To set a baby role: ").color("#AAAAAA").insert(Message.raw("/hytame custom setbaby" + modelAssetId + " <babyRole>").color("#FFFF55")));
                     return CompletableFuture.completedFuture((Object)null);
                  }
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }

      private String discoverModelFromRole(HyTamePlugin plugin, String roleName, int roleIndex) {
         try {
            World world = Universe.get().getDefaultWorld();
            if (world == null) {
               try {
                  Method getWorlds = Universe.class.getMethod("getWorlds");
                  Collection<World> worlds = (Collection)getWorlds.invoke(Universe.get());
                  if (worlds != null && !worlds.isEmpty()) {
                     world = (World)worlds.iterator().next();
                  }
               } catch (Exception var7) {
               }
            }

            if (world == null) {
               ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("No world available for model discovery");
               return null;
            } else {
               CompletableFuture<String> future = new CompletableFuture();
               world.execute(() -> {
                  try {
                     Store<EntityStore> store = world.getEntityStore().getStore();
                     ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
                     ComponentType<EntityStore, ModelComponent> modelType = EcsReflectionUtil.MODEL_TYPE;
                     String[] foundModel = new String[]{null};
                     store.forEachChunk((chunk, buffer) -> {
                        if (foundModel[0] == null) {
                           for(int i = 0; i < chunk.size(); ++i) {
                              NPCEntity npc = (NPCEntity)chunk.getComponent(i, npcType);
                              if (npc != null && roleName.equals(npc.getRoleName())) {
                                 Ref<EntityStore> ref = chunk.getReferenceTo(i);
                                 String modelId = this.extractModelFromRef(plugin, store, ref);
                                 if (modelId != null) {
                                    foundModel[0] = modelId;
                                    return;
                                 }
                              }
                           }

                        }
                     });
                     if (foundModel[0] != null) {
                        if (HyTamePlugin.isVerboseLogging()) {
                           ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[ModelDiscovery] Found existing entity with role %s, model: %s", roleName, foundModel[0]);
                        }

                        future.complete(foundModel[0]);
                        return;
                     }

                     if (HyTamePlugin.isVerboseLogging()) {
                        ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[ModelDiscovery] No existing entity found, spawning temp for role: %s", roleName);
                     }

                     Vector3d tempPos = new Vector3d((double)0.0F, (double)500.0F, (double)0.0F);
                     Rotation3f rotation = new Rotation3f(0.0F, 0.0F, 0.0F);
                     Pair<Ref<EntityStore>, NPCEntity> result = NPCPlugin.get().spawnEntity(store, roleIndex, tempPos, rotation, (Model)null, (TriConsumer)null);
                     if (result != null) {
                        Ref<EntityStore> entityRef = (Ref)result.left();
                        NPCEntity npcEntity = (NPCEntity)result.right();
                        if (entityRef != null) {
                           String modelId = this.extractModelFromRef(plugin, store, entityRef);
                           npcEntity.setDespawning(true);
                           future.complete(modelId);
                           return;
                        }
                     }

                     ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[ModelDiscovery] All strategies failed for %s", roleName);
                     future.complete((Object)null);
                  } catch (Exception e) {
                     ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[ModelDiscovery] Error: %s", e.getMessage());
                     future.complete((Object)null);
                  }

               });
               return (String)future.get(5L, TimeUnit.SECONDS);
            }
         } catch (Exception e) {
            ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("Model discovery failed for %s: %s", roleName, e.getMessage());
            return null;
         }
      }

      private String extractModelFromRef(HyTamePlugin plugin, Store<EntityStore> store, Ref<EntityStore> ref) {
         try {
            ModelComponent modelComp = (ModelComponent)store.getComponent(ref, EcsReflectionUtil.MODEL_TYPE);
            if (modelComp == null) {
               ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[ModelDiscovery] ModelComponent is null");
               return null;
            } else {
               Field modelField = ModelComponent.class.getDeclaredField("model");
               modelField.setAccessible(true);
               Object model = modelField.get(modelComp);
               if (model == null) {
                  ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[ModelDiscovery] model field is null");
                  return null;
               } else {
                  String modelStr = model.toString();
                  if (HyTamePlugin.isVerboseLogging()) {
                     ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[ModelDiscovery] Model toString: %s", modelStr);
                  }

                  int start = modelStr.indexOf("modelAssetId='");
                  if (start >= 0) {
                     start += 14;
                     int end = modelStr.indexOf("'", start);
                     if (end > start) {
                        return modelStr.substring(start, end);
                     }
                  }

                  start = modelStr.indexOf("modelAssetId=");
                  if (start >= 0) {
                     start += 13;
                     int end = modelStr.indexOf(",", start);
                     if (end < 0) {
                        end = modelStr.indexOf(")", start);
                     }

                     if (end < 0) {
                        end = modelStr.indexOf("}", start);
                     }

                     if (end > start) {
                        return modelStr.substring(start, end).trim();
                     }
                  }

                  ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[ModelDiscovery] Could not parse modelAssetId from: %s", modelStr);
                  return null;
               }
            }
         } catch (Exception e) {
            ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[ModelDiscovery] extractModelFromRef error: %s", e.getMessage());
            return null;
         }
      }
   }

   public static class CustomAnimalRemoveCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;

      public CustomAnimalRemoveCommand() {
         super("remove", "Remove a custom animal");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID to remove", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               if (plugin.getConfigManager().removeCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Removed custom animal: ").color("#55FF55").insert(Message.raw(modelId).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Use /hytame config save to persist changes!").color("#FFAA00"));
               } else {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
               }

               return CompletableFuture.completedFuture((Object)null);
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalListCommand extends AbstractCommand {
      public CustomAnimalListCommand() {
         super("list", "List all custom animals");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getConfigManager() != null) {
            Map<String, CustomAnimalConfig> customs = plugin.getConfigManager().getCustomAnimals();
            if (customs.isEmpty()) {
               ctx.sendMessage(Message.raw("No custom animals defined.").color("#AAAAAA"));
               ctx.sendMessage(Message.raw("Use /customanimal add <model> <food> to add one!").color("#FFAA00"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               ctx.sendMessage(Message.raw("=== Custom Animals (" + customs.size() + ") ===").color("#FF9900"));

               for(CustomAnimalConfig custom : customs.values()) {
                  String status = custom.isEnabled() ? "[ON]" : "[OFF]";
                  String statusColor = custom.isEnabled() ? "#55FF55" : "#FF5555";
                  ctx.sendMessage(Message.raw(status).color(statusColor).insert(Message.raw(" " + custom.getModelAssetId()).color("#FFFFFF")).insert(Message.raw(" - " + custom.getBreedingFoods().size() + " foods").color("#AAAAAA")));
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         } else {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class CustomAnimalInfoCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;

      public CustomAnimalInfoCommand() {
         super("info", "[Deprecated] Use /hytame config info <animal> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getConfigManager() != null) {
            Object var10001 = ctx.get(this.modelArg);
            ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config info " + (String)var10001 + " instead").color("#FFAA00"));
            String modelId = (String)ctx.get(this.modelArg);
            CustomAnimalConfig custom = plugin.getConfigManager().getCustomAnimal(modelId);
            if (custom == null) {
               ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String var7 = custom.getDisplayName();
               ctx.sendMessage(Message.raw("=== " + var7 + " ===").color("#FF9900"));
               ctx.sendMessage(Message.raw("Model ID: ").color("#AAAAAA").insert(Message.raw(custom.getModelAssetId()).color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Enabled: ").color("#AAAAAA").insert(Message.raw(custom.isEnabled() ? "Yes" : "No").color(custom.isEnabled() ? "#55FF55" : "#FF5555")));
               ctx.sendMessage(Message.raw("Mountable: ").color("#AAAAAA").insert(Message.raw(custom.isMountable() ? "Yes" : "No").color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Growth Time: ").color("#AAAAAA").insert(Message.raw(custom.getGrowthTimeMinutes() + " min").color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Breed Cooldown: ").color("#AAAAAA").insert(Message.raw(custom.getBreedCooldownMinutes() + " min").color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Breeding Foods:").color("#AAAAAA"));

               for(String food : custom.getBreedingFoods()) {
                  ctx.sendMessage(Message.raw("  - " + food).color("#FFFFFF"));
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         } else {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class CustomAnimalEnableCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;

      public CustomAnimalEnableCommand() {
         super("enable", "[Deprecated] Use /hytame config enable <animal> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config enable " + modelId + " instead").color("#FFAA00"));
                  plugin.getConfigManager().setCustomAnimalEnabled(modelId, true);
                  ctx.sendMessage(Message.raw("Enabled custom animal: ").color("#55FF55").insert(Message.raw(modelId).color("#FFFFFF")));
                  plugin.autoSetupNearbyAnimals();
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalDisableCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;

      public CustomAnimalDisableCommand() {
         super("disable", "[Deprecated] Use /hytame config disable <animal> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config disable " + modelId + " instead").color("#FFAA00"));
                  plugin.getConfigManager().setCustomAnimalEnabled(modelId, false);
                  ctx.sendMessage(Message.raw("Disabled custom animal: ").color("#FF5555").insert(Message.raw(modelId).color("#FFFFFF")));
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalAddFoodCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;
      private final RequiredArg<String> foodArg;

      public CustomAnimalAddFoodCommand() {
         super("addfood", "[Deprecated] Use /hytame config addfood <animal> <food> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
         this.foodArg = this.withRequiredArg("food", "Food item ID to add", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config addfood " + modelId + " <food> instead").color("#FFAA00"));
                  String food = BreedingConfigCommand.resolveFoodShortcut((String)ctx.get(this.foodArg));
                  plugin.getConfigManager().addCustomAnimalFood(modelId, food);
                  CustomAnimalCommand.syncCustomAnimalPatch(plugin, modelId);
                  ctx.sendMessage(Message.raw("Added food ").color("#55FF55").insert(Message.raw(food).color("#FFFFFF")).insert(Message.raw(" to " + modelId).color("#AAAAAA")));
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalRemoveFoodCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;
      private final RequiredArg<String> foodArg;

      public CustomAnimalRemoveFoodCommand() {
         super("removefood", "[Deprecated] Use /hytame config removefood <animal> <food> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
         this.foodArg = this.withRequiredArg("food", "Food item ID to remove", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config removefood " + modelId + " <food> instead").color("#FFAA00"));
                  String food = BreedingConfigCommand.resolveFoodShortcut((String)ctx.get(this.foodArg));
                  plugin.getConfigManager().removeCustomAnimalFood(modelId, food);
                  CustomAnimalCommand.syncCustomAnimalPatch(plugin, modelId);
                  ctx.sendMessage(Message.raw("Removed food ").color("#FF5555").insert(Message.raw(food).color("#FFFFFF")).insert(Message.raw(" from " + modelId).color("#AAAAAA")));
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalScanCommand extends AbstractCommand {
      public CustomAnimalScanCommand() {
         super("scan", "Scan world for all creature modelAssetIds");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("Scanning world for creatures...").color("#FFFF55"));
            World world = Universe.get().getDefaultWorld();
            if (world == null) {
               ctx.sendMessage(Message.raw("No world available!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               AnimalFinder.findAnimals(world, false, (animals) -> {
                  if (animals.isEmpty()) {
                     ctx.sendMessage(Message.raw("No creatures found in the world.").color("#AAAAAA"));
                  } else {
                     Map<String, Integer> counts = new TreeMap();

                     for(AnimalFinder.FoundAnimal animal : animals) {
                        String id = animal.getModelAssetId();
                        counts.merge(id, 1, Integer::sum);
                     }

                     ctx.sendMessage(Message.raw("=== Detected Creatures (" + counts.size() + " types) ===").color("#FF9900"));
                     ctx.sendMessage(Message.raw("Built-in animals:").color("#55FF55"));
                     int builtInCount = 0;

                     for(Map.Entry<String, Integer> entry : counts.entrySet()) {
                        AnimalType type = AnimalType.fromModelAssetId((String)entry.getKey());
                        if (type != null) {
                           ctx.sendMessage(Message.raw("  " + (String)entry.getKey()).color("#AAAAAA").insert(Message.raw(" x" + String.valueOf(entry.getValue())).color("#FFFFFF")).insert(Message.raw(" [" + String.valueOf(type) + "]").color("#55FF55")));
                           ++builtInCount;
                        }
                     }

                     if (builtInCount == 0) {
                        ctx.sendMessage(Message.raw("  (none found)").color("#AAAAAA"));
                     }

                     ctx.sendMessage(Message.raw("Other creatures (can add as custom):").color("#FFAA00"));
                     int otherCount = 0;

                     for(Map.Entry<String, Integer> entry : counts.entrySet()) {
                        AnimalType type = AnimalType.fromModelAssetId((String)entry.getKey());
                        if (type == null) {
                           boolean isCustom = plugin.getConfigManager().isCustomAnimal((String)entry.getKey());
                           String status = isCustom ? " [ADDED]" : "";
                           String statusColor = isCustom ? "#55FF55" : "#FFAA00";
                           ctx.sendMessage(Message.raw("  " + (String)entry.getKey()).color("#FFFFFF").insert(Message.raw(" x" + String.valueOf(entry.getValue())).color("#AAAAAA")).insert(Message.raw(status).color(statusColor)));
                           ++otherCount;
                        }
                     }

                     if (otherCount == 0) {
                        ctx.sendMessage(Message.raw("  (none found)").color("#AAAAAA"));
                     }

                     ctx.sendMessage(Message.raw("Use ").color("#AAAAAA").insert(Message.raw("/hytame custom add <name> <food>").color("#FFFFFF")).insert(Message.raw(" to add a creature").color("#AAAAAA")));
                     Map<String, CustomAnimalConfig> customAnimals = plugin.getConfigManager().getCustomAnimals();
                     if (!customAnimals.isEmpty()) {
                        ctx.sendMessage(Message.raw(""));
                        ctx.sendMessage(Message.raw("Registered custom animals:").color("#55FFFF"));

                        for(String registeredName : customAnimals.keySet()) {
                           boolean foundInWorld = counts.containsKey(registeredName);
                           String foundStatus = foundInWorld ? " [IN WORLD]" : " [NOT FOUND]";
                           String foundColor = foundInWorld ? "#55FF55" : "#FF5555";
                           ctx.sendMessage(Message.raw("  " + registeredName).color("#FFFFFF").insert(Message.raw(foundStatus).color(foundColor)));
                        }

                        ctx.sendMessage(Message.raw("Setting up interactions for custom animals...").color("#AAAAAA"));
                        plugin.autoSetupNearbyAnimals();
                     }

                  }
               });
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalSetRoleCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;
      private final RequiredArg<String> roleArg;

      public CustomAnimalSetRoleCommand() {
         super("setrole", "Set the NPC role ID for spawning babies");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
         this.roleArg = this.withRequiredArg("roleId", "NPC role ID", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               String roleId = (String)ctx.get(this.roleArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  ctx.sendMessage(Message.raw("Use /hytame custom add first").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  plugin.getConfigManager().setCustomAnimalNpcRole(modelId, roleId);
                  ctx.sendMessage(Message.raw("Set NPC role for ").color("#55FF55").insert(Message.raw(modelId).color("#FFFFFF")).insert(Message.raw(" to ").color("#55FF55")).insert(Message.raw(roleId).color("#FFAA00")));
                  ctx.sendMessage(Message.raw("Use /hytame config save to persist").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalSetBabyCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;
      private final RequiredArg<String> babyRoleArg;

      public CustomAnimalSetBabyCommand() {
         super("setbaby", "Set the NPC role for spawning babies");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID of the adult", ArgTypes.STRING);
         this.babyRoleArg = this.withRequiredArg("babyRoleId", "NPC role ID for baby spawning", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               String babyRoleId = (String)ctx.get(this.babyRoleArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  ctx.sendMessage(Message.raw("Use /hytame custom add first").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  NPCPlugin npcPlugin = NPCPlugin.get();
                  int roleIndex = npcPlugin.getIndex(babyRoleId);
                  if (roleIndex < 0) {
                     ctx.sendMessage(Message.raw("Baby NPC role not found: " + babyRoleId).color("#FF5555"));
                     ctx.sendMessage(Message.raw("Make sure this is a valid NPC role name.").color("#AAAAAA"));
                     return CompletableFuture.completedFuture((Object)null);
                  } else {
                     plugin.getConfigManager().setCustomAnimalBabyRole(modelId, babyRoleId);
                     CustomAnimalCommand.syncCustomAnimalPatch(plugin, modelId);
                     ctx.sendMessage(Message.raw("Set baby NPC role for ").color("#55FF55").insert(Message.raw(modelId).color("#FFFFFF")).insert(Message.raw(" to ").color("#55FF55")).insert(Message.raw(babyRoleId).color("#FFAA00")));
                     ctx.sendMessage(Message.raw("Babies will now spawn using this role instead of scaling.").color("#AAAAAA"));
                     ctx.sendMessage(Message.raw("Use /hytame config save to persist").color("#AAAAAA"));
                     return CompletableFuture.completedFuture((Object)null);
                  }
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalSetGrowthCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;
      private final RequiredArg<Double> timeArg;

      public CustomAnimalSetGrowthCommand() {
         super("setgrowth", "[Deprecated] Use /hytame config set <animal> growth <min> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
         this.timeArg = this.withRequiredArg("minutes", "Growth time in minutes", ArgTypes.DOUBLE);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               double minutes = (Double)ctx.get(this.timeArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else if (minutes <= (double)0.0F) {
                  ctx.sendMessage(Message.raw("Growth time must be positive").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config set " + modelId + " growth " + minutes + " instead").color("#FFAA00"));
                  plugin.getConfigManager().setCustomAnimalGrowthTime(modelId, minutes);
                  CustomAnimalCommand.syncCustomAnimalPatch(plugin, modelId);
                  ctx.sendMessage(Message.raw("Set growth time for ").color("#55FF55").insert(Message.raw(modelId).color("#FFFFFF")).insert(Message.raw(" to ").color("#55FF55")).insert(Message.raw(minutes + " min").color("#FFAA00")));
                  ctx.sendMessage(Message.raw("Use /hytame config save to persist").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class CustomAnimalSetCooldownCommand extends AbstractCommand {
      private final RequiredArg<String> modelArg;
      private final RequiredArg<Double> timeArg;

      public CustomAnimalSetCooldownCommand() {
         super("setcooldown", "[Deprecated] Use /hytame config set <animal> cooldown <min> instead");
         this.modelArg = this.withRequiredArg("modelAssetId", "Model asset ID", ArgTypes.STRING);
         this.timeArg = this.withRequiredArg("minutes", "Cooldown in minutes", ArgTypes.DOUBLE);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (CustomAnimalCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               String modelId = (String)ctx.get(this.modelArg);
               double minutes = (Double)ctx.get(this.timeArg);
               if (!plugin.getConfigManager().isCustomAnimal(modelId)) {
                  ctx.sendMessage(Message.raw("Custom animal not found: " + modelId).color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else if (minutes < (double)0.0F) {
                  ctx.sendMessage(Message.raw("Cooldown must be non-negative").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config set " + modelId + " cooldown " + minutes + " instead").color("#FFAA00"));
                  plugin.getConfigManager().setCustomAnimalCooldown(modelId, minutes);
                  CustomAnimalCommand.syncCustomAnimalPatch(plugin, modelId);
                  ctx.sendMessage(Message.raw("Set cooldown for ").color("#55FF55").insert(Message.raw(modelId).color("#FFFFFF")).insert(Message.raw(" to ").color("#55FF55")).insert(Message.raw(minutes + " min").color("#FFAA00")));
                  ctx.sendMessage(Message.raw("Use /hytame config save to persist").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            } else {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }
}
