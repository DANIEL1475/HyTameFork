package com.hytame.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.components.HyTameInteractionComponent;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.GrowthManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.TameHelper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector3d;

public class FeedAnimalInteraction extends SimpleInteraction {
   public static final BuilderCodec<FeedAnimalInteraction> CODEC;
   private static final String HEARTS_PARTICLE = "BreedingHearts";
   private static final String TAMING_PARTICLE = "TameHearts";
   private static final double BREEDING_DISTANCE = (double)5.0F;
   private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE;
   private static final ComponentType<EntityStore, ModelComponent> MODEL_TYPE;
   private static final ComponentType<EntityStore, UUIDComponent> UUID_TYPE;
   private static Field cachedModelField;
   private static boolean modelFieldInitialized;
   private boolean shouldFail = false;
   private Ref<EntityStore> failedTargetRef = null;

   protected void tick0(boolean firstRun, float time, InteractionType type, InteractionContext context, CooldownHandler cooldownHandler) {
      this.log("FeedAnimalInteraction tick0 called");
      if (firstRun && HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin p = HyTamePlugin.getInstance();
         if (p != null) {
            ((HytaleLogger.Api)p.getLogger().atInfo()).log("[FeedAnimal] tick0 triggered! firstRun=%s, type=%s", firstRun, type);
         }
      }

      if (firstRun) {
         this.shouldFail = false;
         this.failedTargetRef = null;

         try {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin == null) {
               this.shouldFail = true;
               return;
            }

            BreedingManager breeding = plugin.getBreedingManager();
            if (breeding == null) {
               this.shouldFail = true;
               return;
            }

            Ref<EntityStore> targetRef = context.getTargetEntity();
            if (targetRef == null) {
               this.log("targetRef is null");
               this.shouldFail = true;
               return;
            }

            ConfigManager configManager = plugin.getConfigManager();
            if (configManager == null) {
               this.log("configManager is null");
               this.shouldFail = true;
               return;
            }

            if (this.isPlayerEntity(targetRef)) {
               this.log("Target is a player entity, skipping FeedAnimal interaction");
               this.shouldFail = true;
               return;
            }

            ItemStack heldItem = context.getHeldItem();
            String itemId = heldItem != null ? heldItem.getItemId() : null;
            String var10001 = heldItem != null ? heldItem.getClass().getSimpleName() : "null";
            this.log("Held item: " + var10001 + ", itemId: " + itemId);
            String modelAssetId = this.getModelAssetIdFromEntity(targetRef);
            AnimalType animalType = modelAssetId != null ? AnimalType.fromModelAssetId(modelAssetId) : null;
            CustomAnimalConfig customAnimal = null;
            this.log("Model asset ID: " + modelAssetId);
            var10001 = animalType != null ? animalType.name() : "null";
            this.log("Animal type: " + var10001);
            if (animalType == null && modelAssetId != null && configManager != null) {
               customAnimal = configManager.getCustomAnimal(modelAssetId);
               if (customAnimal != null) {
                  this.log("Found custom animal: " + customAnimal.getDisplayName());
               }
            }

            if (animalType == null && customAnimal == null) {
               this.log("Not a breedable animal, triggering fallback");
               this.shouldFail = true;
               this.failedTargetRef = targetRef;
               this.triggerFallbackInteraction(context, targetRef);
               return;
            }

            if (modelAssetId != null && AnimalType.isBabyVariant(modelAssetId)) {
               this.log("Target is a baby variant animal, skipping feed interaction");
               this.shouldFail = true;
               return;
            }

            UUID targetUuid = this.getUuidFromRef(targetRef);
            if (targetUuid != null) {
               GrowthManager growthMgr = plugin.getGrowthManager();
               if (growthMgr != null && !growthMgr.isFullyGrown(targetUuid)) {
                  this.log("Target is a growing baby animal, skipping feed interaction");
                  this.shouldFail = true;
                  return;
               }

               BreedingData babyData = plugin.getBreedingManager().getData(targetUuid);
               if (babyData != null && babyData.getGrowthStage() != null && babyData.getGrowthStage() != GrowthStage.ADULT) {
                  this.log("Target is a scaled baby animal, skipping feed interaction");
                  this.shouldFail = true;
                  return;
               }
            }

            TamingManager tamingManager = plugin.getTamingManager();
            UUID playerUuid = this.getPlayerUuid(context);
            if (tamingManager != null && playerUuid != null) {
               UUID animalUuid = this.getUuidFromRef(targetRef);
               if (!tamingManager.canPlayerInteract(animalUuid, playerUuid)) {
                  TamedAnimalData tamedData = tamingManager.getTamedData(animalUuid);
                  String ownerName = tamedData != null ? tamedData.getOwnerUuid().toString().substring(0, 8) + "..." : "someone";
                  this.sendPlayerMessage(context, "This animal belongs to " + ownerName + "!", "#FF5555");
                  this.shouldFail = true;
                  return;
               }
            }

            if (animalType != null && itemId != null && configManager != null) {
               boolean isTamingFood = configManager.isTamingFood(animalType, itemId);
               boolean isAlreadyTamed = TameHelper.isTamed(targetRef);
               if (!isAlreadyTamed) {
                  if (animalType != null && !configManager.isTamingEnabled(animalType)) {
                     return;
                  }

                  if (customAnimal != null && !configManager.isCustomAnimalTamingEnabled(customAnimal.getModelAssetId())) {
                     return;
                  }
               }

               if (isTamingFood && !isAlreadyTamed) {
                  UUID tamerUuid = this.getPlayerUuid(context);
                  if (tamerUuid != null && tamingManager != null) {
                     Vector3d limitPos = this.getPositionFromRef(targetRef);
                     String limitWorldName = this.getWorldNameFromRef(targetRef);
                     String denial = tamingManager.checkTameLimit(tamerUuid, limitPos != null ? limitPos.x() : (double)0.0F, limitPos != null ? limitPos.z() : (double)0.0F, limitWorldName);
                     if (denial != null) {
                        this.sendPlayerMessage(context, denial, "#FF5555");
                        this.shouldFail = true;
                        return;
                     }
                  }

                  String tamerName = this.getPlayerName(context);
                  if (tamerName == null) {
                     tamerName = "Unknown";
                  }

                  if (tamerUuid != null) {
                     this.log("Taming animal with food: " + itemId);
                     World world = this.getWorldFromRef(targetRef);
                     if (world == null) {
                        world = Universe.get().getDefaultWorld();
                     }

                     UUID finalEntityUuid = this.getUuidFromRef(targetRef);
                     TameHelper.tameAnimalDeferred(targetRef, tamerUuid, tamerName, world, (hyTameComp) -> {
                        if (hyTameComp != null) {
                           this.log("Animal tamed successfully via HyTameComponent");
                           String animalName = "_UNDEFINED";
                           Vector3d pos = this.getPositionFromRef(targetRef);
                           double posX = pos != null ? pos.x() : (double)0.0F;
                           double posY = pos != null ? pos.y() : (double)0.0F;
                           double posZ = pos != null ? pos.z() : (double)0.0F;
                           String worldName = this.getWorldNameFromRef(targetRef);
                           TamedAnimalData tamedData = tamingManager.tameAnimal(hyTameComp.getHytameId(), finalEntityUuid, tamerUuid, animalName, animalType, targetRef, posX, posY, posZ, GrowthStage.ADULT, worldName);
                           if (tamedData != null) {
                              tamedData.setOwnerName(tamerName);
                           }

                           String var10001 = String.valueOf(animalType);
                           this.log("Successfully tamed " + var10001 + " for player " + tamerName);
                        } else {
                           this.log("Failed to tame animal - HyTameComponent is null");
                        }

                     });
                     this.spawnTamingParticles(targetRef);
                     this.playSoundAndConsumeItem(plugin, context, targetRef);
                     return;
                  }
               }
            }

            boolean isCorrectFood = false;
            if (animalType != null) {
               if (configManager != null) {
                  isCorrectFood = itemId != null && configManager.isBreedingFood(animalType, itemId);
                  var10001 = String.valueOf(configManager.getBreedingFoods(animalType));
                  this.log("Valid foods: " + var10001 + ", isCorrectFood: " + isCorrectFood);
               } else {
                  isCorrectFood = itemId != null && animalType.isBreedingFood(itemId);
                  var10001 = animalType.getBreedingFood();
                  this.log("Expected food (fallback): " + var10001 + ", isCorrectFood: " + isCorrectFood);
               }
            } else if (customAnimal != null) {
               isCorrectFood = itemId != null && customAnimal.isBreedingFood(itemId);
               var10001 = String.valueOf(customAnimal.getBreedingFoods());
               this.log("Custom animal valid foods: " + var10001 + ", isCorrectFood: " + isCorrectFood);
            }

            if (!isCorrectFood) {
               this.log("Wrong food or no item, triggering fallback");
               this.shouldFail = true;
               this.failedTargetRef = targetRef;
               this.triggerFallbackInteraction(context, targetRef, animalType);
               return;
            }

            this.log("Correct food! Proceeding with breeding");
            UUID breedLimitUuid = this.getPlayerUuid(context);
            if (breedLimitUuid != null && tamingManager != null) {
               Vector3d limitPos = this.getPositionFromRef(targetRef);
               String limitWorldName = this.getWorldNameFromRef(targetRef);
               String denial = tamingManager.checkTameLimit(breedLimitUuid, limitPos != null ? limitPos.x() : (double)0.0F, limitPos != null ? limitPos.z() : (double)0.0F, limitWorldName);
               if (denial != null) {
                  this.sendPlayerMessage(context, "Can't breed — " + denial, "#FF5555");
                  this.shouldFail = true;
                  return;
               }
            }

            UUID animalId = this.getUuidFromRef(targetRef);
            String worldName = this.getWorldNameFromRef(targetRef);
            if (animalType != null) {
               if (!configManager.isTamingEnabled(animalType)) {
                  return;
               }

               BreedingManager.FeedResult result = breeding.tryFeed(animalId, animalType, itemId, targetRef, worldName);
               switch (result) {
                  case SUCCESS:
                     this.spawnHeartParticles(targetRef);
                     this.checkForMateAndBreedInstantly(breeding, animalId, animalType, targetRef);
                     this.playSoundAndConsumeItem(plugin, context, targetRef);
                     break;
                  case NOT_ADULT:
                  case ON_COOLDOWN:
                  case ALREADY_IN_LOVE:
                  case WRONG_FOOD:
                     this.shouldFail = true;
                     this.failedTargetRef = targetRef;
                     this.triggerFallbackInteraction(context, targetRef, animalType);
                     return;
               }
            } else if (customAnimal != null) {
               if (!configManager.isCustomAnimalBreedingEnabled(customAnimal.getModelAssetId())) {
                  return;
               }

               this.log("Feeding custom animal: " + customAnimal.getDisplayName());
               BreedingManager.FeedResult result = breeding.tryFeedCustomAnimal(animalId, modelAssetId, targetRef, worldName);
               switch (result) {
                  case SUCCESS:
                     this.spawnHeartParticles(targetRef);
                     this.checkForCustomAnimalMateAndBreed(breeding, animalId, modelAssetId, targetRef);
                     this.playSoundAndConsumeItem(plugin, context, targetRef);
                  case NOT_ADULT:
                  default:
                     break;
                  case ON_COOLDOWN:
                     this.shouldFail = true;
                     this.failedTargetRef = targetRef;
                     return;
                  case ALREADY_IN_LOVE:
                     this.spawnHeartParticles(targetRef);
                     this.playSoundAndConsumeItem(plugin, context, targetRef);
               }
            }
         } catch (Exception var28) {
            this.shouldFail = true;
            return;
         }
      }

      if (!this.shouldFail) {
         super.tick0(firstRun, time, type, context, cooldownHandler);
      }

   }

   private void triggerFallbackInteraction(InteractionContext context, Ref<EntityStore> targetRef) {
      this.triggerFallbackInteraction(context, targetRef, (AnimalType)null);
   }

   private void triggerFallbackInteraction(InteractionContext context, Ref<EntityStore> targetRef, AnimalType animalType) {
      try {
         if (!TameHelper.isTamed(targetRef)) {
            this.log("Skipping fallback - animal is not tamed");
            return;
         }

         String originalInteractionId = InteractionStateCache.getInstance().getOriginalInteractionId(targetRef, animalType);
         String var10001 = String.valueOf(targetRef);
         this.log("Looking up fallback for targetRef: " + var10001 + " (animalType: " + String.valueOf(animalType) + ")");
         this.log("Memory cache lookup: " + (originalInteractionId != null ? originalInteractionId : "NOT FOUND"));
         if (originalInteractionId == null) {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getHyTameInteractionComponentType() != null && targetRef.isValid()) {
               try {
                  Store<EntityStore> store = targetRef.getStore();
                  HyTameInteractionComponent origComp = (HyTameInteractionComponent)store.getComponent(targetRef, plugin.getHyTameInteractionComponentType());
                  if (origComp != null && origComp.isCaptured()) {
                     originalInteractionId = origComp.getOriginalInteractionId();
                     this.log("ECS component lookup: " + (originalInteractionId != null ? originalInteractionId : "null (valid)"));
                  }
               } catch (Exception e) {
                  this.log("ECS lookup failed: " + e.getMessage());
               }
            }
         }

         if (originalInteractionId == null || originalInteractionId.isEmpty()) {
            this.log("No fallback interaction found for entity - interaction will just end");
            return;
         }

         this.log("Triggering fallback interaction: " + originalInteractionId);
         if (Universe.get() == null || Universe.get().getWorlds().isEmpty()) {
            this.log("Server is shutting down, skipping fallback interaction");
            return;
         }

         InteractionManager interactionManager = context.getInteractionManager();
         if (interactionManager == null) {
            this.log("InteractionManager is null");
            return;
         }

         RootInteraction rootInteraction;
         try {
            rootInteraction = RootInteraction.getRootInteractionOrUnknown(originalInteractionId);
         } catch (Exception e) {
            this.log("Failed to get RootInteraction (likely server shutting down): " + e.getMessage());
            return;
         }

         if (rootInteraction == null) {
            this.log("Could not find RootInteraction: " + originalInteractionId);
            return;
         }

         try {
            Ref<EntityStore> entityRef = context.getEntity();
            if (entityRef == null) {
               this.log("EntityRef is null in context");
               return;
            }

            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
            World world = this.getWorldFromRef(targetRef);
            if (world == null) {
               world = this.getWorldFromRef(entityRef);
            }

            if (world == null) {
               world = Universe.get().getDefaultWorld();
            }

            if (world != null) {
               world.execute(() -> {
                  try {
                     interactionManager.startChain(entityRef, commandBuffer, InteractionType.Use, context, rootInteraction);
                     this.log("Successfully triggered fallback via startChain!");
                  } catch (Exception ex) {
                     this.log("Scheduled startChain failed: " + ex.getMessage());
                  }

               });
               this.log("Scheduled fallback interaction for next tick");
               return;
            }
         } catch (Exception e) {
            this.log("startChain failed: " + e.getMessage());
         }
      } catch (Exception e) {
         this.log("triggerFallbackInteraction error: " + e.getMessage());
      }

   }

   private void playSoundAndConsumeItem(HyTamePlugin plugin, InteractionContext context, Ref<EntityStore> targetRef) {
      try {
         this.log("playSoundAndConsumeItem called");
         this.playFeedingSoundAtPosition(targetRef);
         this.consumePlayerHeldItem(context);
      } catch (Exception e) {
         this.log("playSoundAndConsumeItem error: " + e.getMessage());
      }

   }

   private void log(String msg) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[FeedAnimal] " + msg);
         }

      }
   }

   private void playFeedingSoundAtPosition(Ref<EntityStore> targetRef) {
      try {
         Vector3d pos = this.getEntityPosition(targetRef);
         if (pos == null) {
            this.log("playFeedingSoundAtPosition: pos is null");
            return;
         }

         Store<EntityStore> store = targetRef.getStore();
         if (store == null) {
            this.log("playFeedingSoundAtPosition: store is null");
            return;
         }

         int soundId = SoundEvent.getAssetMap().getIndex("SFX_Consume_Bread");
         if (soundId < 0) {
            this.log("playFeedingSoundAtPosition: soundId < 0, aborting");
            return;
         }

         SoundUtil.playSoundEvent3d(soundId, pos.x(), pos.y(), pos.z(), (p) -> true, store);
      } catch (Exception e) {
         this.log("playFeedingSoundAtPosition error: " + e.getMessage());
      }

   }

   private void consumePlayerHeldItem(InteractionContext context) {
      try {
         ItemContainer container = context.getHeldItemContainer();
         if (container != null) {
            short slot = (short)context.getHeldItemSlot();

            try {
               container.removeItemStackFromSlot(slot, 1);
               return;
            } catch (Exception e) {
               this.log("consumePlayerHeldItem: removeItemStackFromSlot failed: " + e.getMessage());
            }
         }

         this.log("consumePlayerHeldItem: could not consume item");
      } catch (Exception e) {
         this.log("consumePlayerHeldItem error: " + e.getMessage());
      }

   }

   private void spawnHeartParticles(Ref<EntityStore> targetRef) {
      this.spawnParticles(targetRef, "BreedingHearts");
   }

   private void spawnTamingParticles(Ref<EntityStore> targetRef) {
      this.spawnParticles(targetRef, "TameHearts");
   }

   private void spawnParticles(Ref<EntityStore> targetRef, String particleId) {
      try {
         Vector3d position = this.getEntityPosition(targetRef);
         if (position == null) {
            return;
         }

         double x = position.x();
         double y = position.y() + (double)1.5F;
         double z = position.z();
         Store<EntityStore> store = targetRef.getStore();
         Vector3d particlePos = new Vector3d(x, y, z);
         ParticleUtil.spawnParticleEffect(particleId, particlePos, store);
      } catch (Exception var12) {
      }

   }

   private Vector3d getEntityPosition(Ref<EntityStore> ref) {
      try {
         Store<EntityStore> store = ref.getStore();
         if (store == null) {
            return null;
         }

         TransformComponent transform = (TransformComponent)store.getComponent(ref, TRANSFORM_TYPE);
         if (transform != null) {
            return transform.getPosition();
         }
      } catch (Exception var4) {
      }

      return null;
   }

   private void checkForMateAndBreedInstantly(BreedingManager breeding, UUID animalId, AnimalType animalType, Ref<EntityStore> targetRef) {
      Vector3d thisPos = this.getEntityPosition(targetRef);
      if (thisPos != null) {
         BreedingData currentData = breeding.getData(animalId);
         if (currentData != null && currentData.getEntityRef() == null) {
            currentData.setEntityRef(targetRef);
         }

         List<UUID> toRemove = new ArrayList();

         for(UUID otherId : breeding.getTrackedAnimalIds()) {
            if (!otherId.equals(animalId)) {
               BreedingData otherData = breeding.getData(otherId);
               if (otherData != null && otherData.getAnimalType() == animalType && otherData.isInLove() && !otherData.isPregnant()) {
                  Ref<EntityStore> otherRef = otherData.getEntityRef();
                  if (otherRef != null) {
                     Vector3d otherPos = this.getEntityPosition(otherRef);
                     if (otherPos == null) {
                        toRemove.add(otherId);
                     } else {
                        double distance = this.calculateDistance(thisPos, otherPos);
                        if (!(distance > (double)5.0F)) {
                           BreedingData animalData = breeding.getData(animalId);
                           if (animalData != null) {
                              animalData.completeBreeding();
                           }

                           otherData.completeBreeding();
                           Vector3d midpoint = new Vector3d((thisPos.x() + otherPos.x()) / (double)2.0F, (thisPos.y() + otherPos.y()) / (double)2.0F, (thisPos.z() + otherPos.z()) / (double)2.0F);
                           HyTamePlugin pluginInstance = HyTamePlugin.getInstance();
                           if (pluginInstance != null && pluginInstance.getSpawningManager() != null) {
                              String worldName = this.getWorldNameFromRef(targetRef);
                              pluginInstance.getSpawningManager().spawnBabyAnimal(animalType, midpoint, animalId, otherId, worldName);
                           }

                           return;
                        }
                     }
                  }
               }
            }
         }

         for(UUID id : toRemove) {
            breeding.removeData(id);
         }

      }
   }

   private void checkForCustomAnimalMateAndBreed(BreedingManager breeding, UUID animalId, String modelAssetId, Ref<EntityStore> targetRef) {
      Vector3d thisPos = this.getEntityPosition(targetRef);
      if (thisPos != null) {
         BreedingManager.CustomAnimalLoveData currentData = breeding.getCustomAnimalLoveData(animalId);
         if (currentData != null && currentData.getEntityRef() == null) {
            currentData.setEntityRef(targetRef);
         }

         for(BreedingManager.CustomAnimalLoveData otherData : breeding.getCustomAnimalsInLove()) {
            if (!otherData.getAnimalId().equals(animalId) && otherData.getModelAssetId().equals(modelAssetId) && otherData.isInLove()) {
               Ref<EntityStore> otherRef = otherData.getEntityRef();
               if (otherRef != null) {
                  Vector3d otherPos = this.getEntityPosition(otherRef);
                  if (otherPos != null) {
                     double distance = this.calculateDistance(thisPos, otherPos);
                     if (!(distance > (double)5.0F)) {
                        this.log("Custom animals breeding: " + modelAssetId + " at distance " + distance);
                        if (currentData != null) {
                           currentData.completeBreeding();
                        }

                        otherData.completeBreeding();
                        Vector3d midpoint = new Vector3d((thisPos.x() + otherPos.x()) / (double)2.0F, (thisPos.y() + otherPos.y()) / (double)2.0F, (thisPos.z() + otherPos.z()) / (double)2.0F);
                        HyTamePlugin plugin = HyTamePlugin.getInstance();
                        if (plugin != null && plugin.getSpawningManager() != null) {
                           CustomAnimalConfig customConfig = plugin.getConfigManager().getCustomAnimal(modelAssetId);
                           String worldName = this.getWorldNameFromRef(targetRef);
                           plugin.getSpawningManager().spawnCustomAnimalBaby(modelAssetId, customConfig, midpoint, worldName, animalId, otherData.getAnimalId());
                        }

                        return;
                     }
                  }
               }
            }
         }

      }
   }

   private double calculateDistance(Vector3d pos1, Vector3d pos2) {
      double dx = pos2.x() - pos1.x();
      double dy = pos2.y() - pos1.y();
      double dz = pos2.z() - pos1.z();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private UUID getUuidFromRef(Ref<EntityStore> ref) {
      return ref != null ? EcsReflectionUtil.getUuidFromRef(ref) : null;
   }

   private Vector3d getPositionFromRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         try {
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
               return null;
            } else {
               TransformComponent transform = (TransformComponent)store.getComponent(ref, TRANSFORM_TYPE);
               return transform != null ? transform.getPosition() : null;
            }
         } catch (Exception var4) {
            return null;
         }
      }
   }

   private boolean isPlayerEntity(Ref<EntityStore> ref) {
      try {
         if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
               return false;
            } else {
               PlayerRef playerRef = (PlayerRef)store.getComponent(ref, EcsReflectionUtil.PLAYER_REF_TYPE);
               return playerRef != null;
            }
         } else {
            return false;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   private String getModelAssetIdFromEntity(Ref<EntityStore> targetRef) {
      try {
         Store<EntityStore> store = targetRef.getStore();
         if (store == null) {
            return null;
         } else {
            ModelComponent modelComp = (ModelComponent)store.getComponent(targetRef, MODEL_TYPE);
            if (modelComp == null) {
               return null;
            } else if (modelFieldInitialized && cachedModelField != null) {
               Object model = cachedModelField.get(modelComp);
               if (model == null) {
                  return null;
               } else {
                  String modelStr = model.toString();
                  int start = modelStr.indexOf("modelAssetId='");
                  if (start < 0) {
                     return null;
                  } else {
                     start += 14;
                     int end = modelStr.indexOf("'", start);
                     return end <= start ? null : modelStr.substring(start, end);
                  }
               }
            } else {
               return null;
            }
         }
      } catch (Exception var8) {
         return null;
      }
   }

   private AnimalType getAnimalTypeFromEntity(Ref<EntityStore> targetRef) {
      String modelAssetId = this.getModelAssetIdFromEntity(targetRef);
      return modelAssetId != null ? AnimalType.fromModelAssetId(modelAssetId) : null;
   }

   private UUID getPlayerUuid(InteractionContext context) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return null;
         }

         Store<EntityStore> store = entityRef.getStore();
         if (store == null) {
            return null;
         }

         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(entityRef, UUID_TYPE);
         if (uuidComp != null && uuidComp.getUuid() != null) {
            return uuidComp.getUuid();
         }
      } catch (Exception e) {
         this.log("getPlayerUuid error: " + e.getMessage());
      }

      return null;
   }

   private String getPlayerName(InteractionContext context) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return null;
         }

         Store<EntityStore> store = entityRef.getStore();
         if (store == null) {
            return null;
         }

         Player player = (Player)store.getComponent(entityRef, EcsReflectionUtil.PLAYER_TYPE);
         if (player != null) {
            return player.getLegacyDisplayName();
         }
      } catch (Exception e) {
         this.log("getPlayerName error: " + e.getMessage());
      }

      return null;
   }

   private boolean isHoldingNameTag(String itemId) {
      if (itemId == null) {
         return false;
      } else {
         return itemId.equalsIgnoreCase("NameTag") || itemId.equalsIgnoreCase("Name_Tag") || itemId.equalsIgnoreCase("Misc_NameTag") || itemId.toLowerCase().contains("nametag");
      }
   }

   private void sendPlayerMessage(InteractionContext context, String message, String color) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return;
         }

         Store<EntityStore> store = entityRef.getStore();
         if (store == null) {
            return;
         }

         PlayerRef playerRef = (PlayerRef)store.getComponent(entityRef, EcsReflectionUtil.PLAYER_REF_TYPE);
         if (playerRef == null) {
            return;
         }

         playerRef.sendMessage(Message.raw(message));
      } catch (Exception e) {
         this.log("sendPlayerMessage error: " + e.getMessage());
      }

   }

   private World getWorldFromRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         try {
            Store<EntityStore> entityStore = ref.getStore();
            if (entityStore == null) {
               return null;
            }

            for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
               World world = (World)entry.getValue();
               if (world != null) {
                  try {
                     Store<EntityStore> worldStore = world.getEntityStore().getStore();
                     if (worldStore == entityStore) {
                        return world;
                     }
                  } catch (Exception var7) {
                  }
               }
            }
         } catch (Exception e) {
            this.log("getWorldFromRef error: " + e.getMessage());
         }

         return null;
      }
   }

   private String getWorldNameFromRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         try {
            UUID entityUuid = this.getUuidFromRef(ref);
            if (entityUuid == null) {
               this.log("[WorldDebug] Could not get UUID from ref");
               return null;
            }

            this.log("[WorldDebug] Searching all worlds for entity UUID: " + String.valueOf(entityUuid));

            for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
               String worldName = (String)entry.getKey();
               World world = (World)entry.getValue();
               if (world != null) {
                  try {
                     Store<EntityStore> store = world.getEntityStore().getStore();
                     if (store != null && ref.getStore() == store) {
                        this.log("[WorldDebug] Found entity in world: " + worldName);
                        return worldName;
                     }
                  } catch (Exception var8) {
                  }
               }
            }

            this.log("[WorldDebug] Entity not found in any world");
         } catch (Exception e) {
            this.log("getWorldNameFromRef error: " + e.getMessage());
         }

         return null;
      }
   }

   private void playTamingSound(Ref<EntityStore> targetRef) {
      try {
         Vector3d pos = this.getEntityPosition(targetRef);
         if (pos == null) {
            return;
         }

         Store<EntityStore> store = targetRef.getStore();
         if (store == null) {
            return;
         }

         int soundId = SoundEvent.getAssetMap().getIndex("SFX_LevelUp");
         if (soundId < 0) {
            soundId = SoundEvent.getAssetMap().getIndex("SFX_Consume_Bread");
         }

         if (soundId < 0) {
            return;
         }

         SoundUtil.playSoundEvent3d(soundId, pos.x(), pos.y(), pos.z(), (p) -> true, store);
      } catch (Exception e) {
         this.log("playTamingSound error: " + e.getMessage());
      }

   }

   static {
      CODEC = BuilderCodec.builder(FeedAnimalInteraction.class, FeedAnimalInteraction::new, SimpleInteraction.CODEC).build();
      TRANSFORM_TYPE = EcsReflectionUtil.TRANSFORM_TYPE;
      MODEL_TYPE = EcsReflectionUtil.MODEL_TYPE;
      UUID_TYPE = EcsReflectionUtil.UUID_TYPE;
      cachedModelField = null;
      modelFieldInitialized = false;

      try {
         cachedModelField = ModelComponent.class.getDeclaredField("model");
         cachedModelField.setAccessible(true);
         modelFieldInitialized = true;
      } catch (Exception var1) {
         modelFieldInitialized = false;
      }

   }
}
