package com.hytame.tame.actions;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hytame.HyTamePlugin;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.GrowthManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.tame.HyTameComponent;
import com.hytame.tame.utils.Debug;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.TameHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class ActionHyTameFeedInteraction extends ActionBase {
   private static final String HEARTS_PARTICLE = "BreedingHearts";
   private static final String TAMING_PARTICLE = "TameHearts";
   private static final double BREEDING_DISTANCE = (double)5.0F;

   public ActionHyTameFeedInteraction(@Nonnull BuilderActionHyTameFeedInteraction builder, @Nonnull BuilderSupport support) {
      super(builder);
   }

   public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      Debug.log("CanExecute called for HyTameFeedInteraction", Level.INFO);
      ModelComponent modelComponent = (ModelComponent)store.getComponent(ref, EcsReflectionUtil.MODEL_TYPE);
      String modelAssetId = modelComponent.getModel().getModelAssetId();
      AnimalType animalType = modelAssetId != null ? AnimalType.fromModelAssetId(modelAssetId) : null;
      CustomAnimalConfig customAnimal = null;
      ConfigManager configManager = plugin.getConfigManager();
      if (animalType == null && modelAssetId != null && configManager != null) {
         customAnimal = configManager.getCustomAnimal(modelAssetId);
         if (customAnimal != null) {
         }
      }

      if (animalType == null) {
         customAnimal = configManager.getCustomAnimal(modelAssetId);
      }

      if (animalType == null || !configManager.isBreedingEnabled(animalType) && !configManager.isTamingEnabled(animalType)) {
         if (customAnimal == null || !customAnimal.isBreedingEnabled() && !customAnimal.isTamingEnabled()) {
            Debug.log("Can't execute tame or feed", Level.INFO);
            return false;
         } else {
            Debug.log("Can execute tame or feed", Level.INFO);
            return true;
         }
      } else {
         Debug.log("Can execute tame or feed", Level.INFO);
         return true;
      }
   }

   public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
      super.execute(ref, role, sensorInfo, dt, store);
      ModelComponent modelComponent = (ModelComponent)store.getComponent(ref, EcsReflectionUtil.MODEL_TYPE);
      String modelAssetId = modelComponent.getModel().getModelAssetId();
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      ConfigManager configManager = plugin.getConfigManager();
      CustomAnimalConfig customAnimal = null;
      AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
      if (animalType == null) {
         customAnimal = configManager.getCustomAnimal(modelAssetId);
      }

      Debug.log("ActionHyTameFeedInteraction: execute called", Level.INFO);
      Ref<EntityStore> playerRef = role.getStateSupport().getInteractionIterationTarget();
      if (playerRef == null) {
         Debug.log("ActionHyTameFeedInteraction: playerRef is null", Level.INFO);
         return false;
      } else {
         Player player = (Player)store.getComponent(playerRef, Player.getComponentType());
         UUIDComponent playerUUID = (UUIDComponent)store.getComponent(playerRef, UUIDComponent.getComponentType());
         PlayerRef playerMsgRef = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
         if (player != null && playerUUID != null) {
            HyTameComponent hyTame = (HyTameComponent)store.ensureAndGetComponent(ref, HyTameComponent.getComponentType());
            if (hyTame == null) {
               Debug.log("ActionHyTameFeedInteraction: HyTameComponent not found", Level.INFO);
               return false;
            } else if (!hyTame.isActionReady()) {
               String reason = "This animal is resting.";

               try {
                  HyTamePlugin p = HyTamePlugin.getInstance();
                  if (p != null && p.getBreedingManager() != null) {
                     UUIDComponent au = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
                     if (au != null) {
                        BreedingData bd = p.getBreedingManager().getData(au.getUuid());
                        if (bd != null && bd.isInLove()) {
                           reason = "This animal isn't hungry right now.";
                        }
                     }
                  }
               } catch (Exception var25) {
               }

               Debug.msg(playerMsgRef, reason, Level.INFO);
               return false;
            } else {
               String itemId = this.getHeldItemId(player);
               boolean isTamed = hyTame.isTamed();
               if (modelAssetId != null && AnimalType.isBabyVariant(modelAssetId)) {
                  Debug.log("Target is a baby variant, skipping feed interaction", Level.INFO);
                  return false;
               } else {
                  UUID animalUuid = EcsReflectionUtil.getUuidFromRef(ref);
                  if (animalUuid != null) {
                     GrowthManager growthMgr = plugin.getGrowthManager();
                     if (growthMgr != null && !growthMgr.isFullyGrown(animalUuid)) {
                        Debug.log("Target is a growing baby, skipping feed interaction", Level.INFO);
                        return false;
                     }

                     BreedingData babyData = plugin.getBreedingManager() != null ? plugin.getBreedingManager().getData(animalUuid) : null;
                     if (babyData != null && babyData.getGrowthStage() != null && babyData.getGrowthStage() != GrowthStage.ADULT) {
                        Debug.log("Target is a scaled baby, skipping feed interaction", Level.INFO);
                        return false;
                     }
                  }

                  if (!isTamed) {
                     TamingManager tamingMgr = plugin.getTamingManager();
                     if (tamingMgr != null) {
                        Vector3d pos = this.getPositionFromRef(ref);
                        String worldName = this.getWorldNameFromRef(ref);
                        String denial = tamingMgr.checkTameLimit(playerUUID.getUuid(), pos != null ? pos.x() : (double)0.0F, pos != null ? pos.z() : (double)0.0F, worldName);
                        if (denial != null) {
                           Debug.msg(playerMsgRef, denial, Level.WARNING);
                           Debug.log("Tame limit denied: " + denial, Level.INFO);
                           return false;
                        }
                     }
                  }

                  if (animalType != null) {
                     if (!isTamed && animalType.usesVanillaTaming()) {
                        Debug.log("Executing vanilla taming hook (livestock)", Level.INFO);
                        return this.executeVanillaTamingHook(ref, role, store, hyTame, playerUUID, player, playerMsgRef, animalType);
                     }

                     if (!isTamed && configManager.isTamingFood(animalType, itemId)) {
                        Debug.log("Executing taming", Level.INFO);
                        return this.executeTaming(ref, role, store, hyTame, playerUUID, player, playerMsgRef);
                     }

                     if (isTamed && configManager.isBreedingFood(animalType, itemId)) {
                        TamingManager tamingMgr = plugin.getTamingManager();
                        if (tamingMgr != null) {
                           Vector3d pos = this.getPositionFromRef(ref);
                           String worldName = this.getWorldNameFromRef(ref);
                           String denial = tamingMgr.checkTameLimit(playerUUID.getUuid(), pos != null ? pos.x() : (double)0.0F, pos != null ? pos.z() : (double)0.0F, worldName);
                           if (denial != null) {
                              Debug.msg(playerMsgRef, "Can't breed — " + denial, Level.WARNING);
                              Debug.log("Tame limit denied breeding: " + denial, Level.INFO);
                              return false;
                           }
                        }

                        Debug.log("Executing breeding", Level.INFO);
                        return this.executeBreeding(ref, store, hyTame, itemId, playerMsgRef);
                     }
                  } else if (customAnimal != null) {
                     if (!isTamed && customAnimal.isTamingEnabled() && customAnimal.isBreedingFood(itemId)) {
                        Debug.log("Executing custom animal taming", Level.INFO);
                        return this.executeTaming(ref, role, store, hyTame, playerUUID, player, playerMsgRef);
                     }

                     if (isTamed && customAnimal.isBreedingEnabled() && customAnimal.isBreedingFood(itemId)) {
                        TamingManager tamingMgr = plugin.getTamingManager();
                        if (tamingMgr != null) {
                           Vector3d pos = this.getPositionFromRef(ref);
                           String worldName = this.getWorldNameFromRef(ref);
                           String denial = tamingMgr.checkTameLimit(playerUUID.getUuid(), pos != null ? pos.x() : (double)0.0F, pos != null ? pos.z() : (double)0.0F, worldName);
                           if (denial != null) {
                              Debug.msg(playerMsgRef, "Can't breed — " + denial, Level.WARNING);
                              Debug.log("Tame limit denied custom breeding: " + denial, Level.INFO);
                              return false;
                           }
                        }

                        Debug.log("Executing custom animal breeding", Level.INFO);
                        return this.executeCustomBreeding(ref, store, modelAssetId, playerMsgRef);
                     }
                  }

                  Debug.log("ActionHyTameFeedInteraction: wrong food for state (tamed=" + isTamed + ", item=" + itemId + ")", Level.INFO);
                  return false;
               }
            }
         } else {
            Debug.log("ActionHyTameFeedInteraction: player or playerUUID is null", Level.INFO);
            return false;
         }
      }
   }

   private String getHeldItemId(Player player) {
      try {
         Inventory inventory = player.getInventory();
         if (inventory == null) {
            return null;
         } else {
            byte slot = inventory.getActiveHotbarSlot();
            ItemStack itemStack = inventory.getHotbar().getItemStack((short)slot);
            return itemStack == null ? null : itemStack.getItemId();
         }
      } catch (Exception e) {
         Debug.log("Error getting held item: " + e.getMessage(), Level.WARNING);
         return null;
      }
   }

   private boolean executeTaming(Ref<EntityStore> ref, Role role, Store<EntityStore> store, HyTameComponent hyTame, UUIDComponent playerUUID, Player player, PlayerRef playerMsgRef) {
      ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
      NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, npcType);
      if (npcEntity == null) {
         Debug.msg(playerMsgRef, "Failed to tame: NPC entity not found", Level.WARNING);
         return false;
      } else {
         hyTame.setTamed(playerUUID.getUuid(), player.getLegacyDisplayName());
         WorldSupport worldSupport = role.getWorldSupport();
         if (hyTame.getOriginalAttitudeOrdinal() == null) {
            hyTame.setOriginalAttitudeOrdinal(worldSupport.getDefaultPlayerAttitude().ordinal());
         }

         try {
            HyTamePlugin.getAttitudeField().set(worldSupport, Attitude.REVERED);
         } catch (IllegalAccessException e) {
            Debug.msg(playerMsgRef, "Failed to set attitude", Level.SEVERE);
            Debug.log("Attitude set error: " + e.getMessage(), Level.SEVERE);
            return false;
         }

         boolean oldState = npcEntity.updateSpawnTrackingState(false);
         if (oldState) {
            Debug.log("Stopped tracking entity " + npcEntity.getRoleName(), Level.INFO);
         }

         hyTame.setOriginalSpawnConfig(npcEntity.getSpawnConfiguration());
         npcEntity.setSpawnConfiguration(Integer.MIN_VALUE);
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getTamingManager() != null) {
            UUIDComponent animalUUID = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            if (animalUUID != null) {
               AnimalType animalType = this.getAnimalTypeFromRef(ref, store);
               String worldName = null;
               World world = null;

               try {
                  for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
                     world = (World)entry.getValue();
                     if (world != null) {
                        try {
                           Store<EntityStore> worldStore = world.getEntityStore().getStore();
                           if (worldStore == store) {
                              worldName = (String)entry.getKey();
                              Debug.log("[MultiWorld] Taming animal in world: " + worldName, Level.INFO);
                              break;
                           }
                        } catch (Exception var24) {
                        }
                     }
                  }
               } catch (Exception e) {
                  Debug.log("Could not get world name for taming: " + e.getMessage(), Level.WARNING);
               }

               String tamerName = player.getLegacyDisplayName();
               UUID finalTamerUuid = playerUUID.getUuid();
               TamingManager tamingManager = plugin.getTamingManager();
               TameHelper.tameAnimalDeferred(ref, playerUUID.getUuid(), tamerName, world, (hyTameComp) -> {
                  if (hyTameComp != null) {
                     this.log("Animal tamed successfully via HyTameComponent");
                     UUID entityUuid = this.getUuidFromRef(ref);
                     String animalName = "_UNDEFINED";
                     Vector3d pos = this.getPositionFromRef(ref);
                     double posX = pos != null ? pos.x() : (double)0.0F;
                     double posY = pos != null ? pos.y() : (double)0.0F;
                     double posZ = pos != null ? pos.z() : (double)0.0F;
                     String finalWorldName = this.getWorldNameFromRef(ref);
                     TamedAnimalData tamedData = tamingManager.tameAnimal(hyTameComp.getHytameId(), entityUuid, finalTamerUuid, animalName, animalType, ref, posX, posY, posZ, GrowthStage.ADULT, finalWorldName);
                     if (tamedData != null) {
                        tamedData.setOwnerName(tamerName);
                     }

                     String var10001 = String.valueOf(animalType);
                     this.log("Successfully tamed " + var10001 + " for player " + tamerName);
                  } else {
                     this.log("Failed to tame animal - HyTameComponent is null");
                  }

               });
               this.spawnTamingParticles(ref);
               this.playFeedingSoundAtPosition(ref);
            }
         }

         Debug.msg(playerMsgRef, npcEntity.getRoleName() + " successfully tamed!", Level.INFO);
         return true;
      }
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
               TransformComponent transform = (TransformComponent)store.getComponent(ref, EcsReflectionUtil.TRANSFORM_TYPE);
               return transform != null ? transform.getPosition() : null;
            }
         } catch (Exception var4) {
            return null;
         }
      }
   }

   private boolean executeVanillaTamingHook(Ref<EntityStore> ref, Role role, Store<EntityStore> store, HyTameComponent hyTame, UUIDComponent playerUUID, Player player, PlayerRef playerMsgRef, AnimalType animalType) {
      hyTame.setTamed(playerUUID.getUuid(), player.getLegacyDisplayName());
      NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
      if (npcEntity != null) {
         hyTame.setOriginalSpawnConfig(npcEntity.getSpawnConfiguration());
         npcEntity.updateSpawnTrackingState(false);
         npcEntity.setSpawnConfiguration(Integer.MIN_VALUE);
      }

      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null && plugin.getTamingManager() != null) {
         UUIDComponent animalUUID = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
         if (animalUUID != null) {
            Vector3d pos = this.getPositionFromRef(ref);
            String worldName = this.getWorldNameFromRef(ref);
            plugin.getTamingManager().tameAnimal(hyTame.getHytameId(), animalUUID.getUuid(), playerUUID.getUuid(), "_UNDEFINED", animalType, ref, pos != null ? pos.x() : (double)0.0F, pos != null ? pos.y() : (double)0.0F, pos != null ? pos.z() : (double)0.0F, GrowthStage.ADULT, worldName);
            Debug.log("Vanilla taming hook: set ownership for " + animalType.name(), Level.INFO);
         }
      }

      this.spawnTamingParticles(ref);
      this.playFeedingSoundAtPosition(ref);
      return true;
   }

   private boolean executeBreeding(Ref<EntityStore> ref, Store<EntityStore> store, HyTameComponent hyTame, String itemId, PlayerRef playerMsgRef) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin == null) {
         Debug.log("Plugin is null", Level.WARNING);
         return false;
      } else {
         BreedingManager breeding = plugin.getBreedingManager();
         if (breeding == null) {
            Debug.log("BreedingManager is null", Level.WARNING);
            return false;
         } else {
            UUIDComponent animalUUIDComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            if (animalUUIDComp == null) {
               Debug.log("Animal UUIDComponent is null", Level.WARNING);
               return false;
            } else {
               UUID animalId = animalUUIDComp.getUuid();
               AnimalType animalType = this.getAnimalTypeFromRef(ref, store);
               if (animalType == null) {
                  Debug.msg(playerMsgRef, "Unknown animal type", Level.WARNING);
                  return false;
               } else {
                  String worldName = null;

                  try {
                     Store<EntityStore> entityStore = store;

                     for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
                        World world = (World)entry.getValue();
                        if (world != null) {
                           try {
                              Store<EntityStore> worldStore = world.getEntityStore().getStore();
                              if (worldStore == entityStore) {
                                 worldName = (String)entry.getKey();
                                 Debug.log("[MultiWorld] Found entity in world: " + worldName, Level.INFO);
                                 break;
                              }
                           } catch (Exception var17) {
                           }
                        }
                     }

                     if (worldName == null) {
                        Debug.log("[MultiWorld] Entity not found in any world, will use default", Level.WARNING);
                     }
                  } catch (Exception e) {
                     Debug.log("Could not get world name: " + e.getMessage(), Level.WARNING);
                  }

                  BreedingManager.FeedResult result = breeding.tryFeed(animalId, animalType, itemId, ref, worldName);
                  Debug.log("Tried breeding", Level.INFO);
                  Debug.log(result.toString(), Level.INFO);
                  switch (result) {
                     case SUCCESS:
                        this.spawnHeartParticles(ref);
                        this.checkForMateAndBreedInstantly(breeding, animalId, animalType, ref);
                        this.playFeedingSoundAtPosition(ref);
                     default:
                        return true;
                     case DISABLED:
                     case NOT_ADULT:
                     case ON_COOLDOWN:
                     case ALREADY_IN_LOVE:
                     case WRONG_FOOD:
                        return false;
                  }
               }
            }
         }
      }
   }

   private boolean executeCustomBreeding(Ref<EntityStore> ref, Store<EntityStore> store, String modelAssetId, PlayerRef playerMsgRef) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin == null) {
         return false;
      } else {
         BreedingManager breeding = plugin.getBreedingManager();
         if (breeding == null) {
            return false;
         } else {
            UUIDComponent animalUUIDComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            if (animalUUIDComp == null) {
               return false;
            } else {
               UUID animalId = animalUUIDComp.getUuid();
               String worldName = this.getWorldNameFromRef(ref);
               BreedingManager.FeedResult result = breeding.tryFeedCustomAnimal(animalId, modelAssetId, ref, worldName);
               Debug.log("Tried custom breeding: " + String.valueOf(result), Level.INFO);
               switch (result) {
                  case SUCCESS:
                     this.spawnHeartParticles(ref);
                     this.checkForCustomMateAndBreedInstantly(breeding, animalId, modelAssetId, ref);
                     this.playFeedingSoundAtPosition(ref);
                     return true;
                  default:
                     return false;
               }
            }
         }
      }
   }

   private void checkForCustomMateAndBreedInstantly(BreedingManager breeding, UUID animalId, String modelAssetId, Ref<EntityStore> targetRef) {
      Vector3d thisPos = this.getEntityPosition(targetRef);
      if (thisPos == null) {
         Debug.log("Custom mate check: thisPos is null", Level.INFO);
      } else {
         int candidateCount = 0;

         for(BreedingManager.CustomAnimalLoveData otherData : breeding.getCustomAnimalsInLove()) {
            ++candidateCount;
            UUID otherId = otherData.getAnimalId();
            if (!otherId.equals(animalId)) {
               if (!modelAssetId.equals(otherData.getModelAssetId())) {
                  Debug.log("Custom mate check: model mismatch - ours: " + modelAssetId + " theirs: " + otherData.getModelAssetId(), Level.INFO);
               } else {
                  Ref<EntityStore> otherRef = otherData.getEntityRef();
                  if (otherRef != null) {
                     Vector3d otherPos = this.getEntityPosition(otherRef);
                     if (otherPos != null) {
                        double distance = this.calculateDistance(thisPos, otherPos);
                        Debug.log("Custom mate check: found mate at distance " + distance + " (max: 5.0)", Level.INFO);
                        if (!(distance > (double)5.0F)) {
                           boolean bred = breeding.tryBreedCustomAnimals(animalId, otherId, modelAssetId);
                           Debug.log("Custom mate check: tryBreedCustomAnimals result: " + bred, Level.INFO);
                           return;
                        }
                     }
                  }
               }
            }
         }

         Debug.log("Custom mate check: no mate found among " + candidateCount + " candidates in love", Level.INFO);
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
                           if (HyTamePlugin.isAlarmBasedBreedCooldown()) {
                              try {
                                 ComponentType<EntityStore, HyTameComponent> hyTameType = HyTameComponent.getComponentType();
                                 if (hyTameType != null) {
                                    HyTameComponent parentTame = (HyTameComponent)targetRef.getStore().getComponent(targetRef, hyTameType);
                                    if (parentTame != null) {
                                       parentTame.setNeedsBreedCooldown(true);
                                    }

                                    HyTameComponent otherTame = (HyTameComponent)otherRef.getStore().getComponent(otherRef, hyTameType);
                                    if (otherTame != null) {
                                       otherTame.setNeedsBreedCooldown(true);
                                    }
                                 }
                              } catch (Exception var19) {
                              }
                           }

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

   private double calculateDistance(Vector3d pos1, Vector3d pos2) {
      double dx = pos2.x() - pos1.x();
      double dy = pos2.y() - pos1.y();
      double dz = pos2.z() - pos1.z();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private Vector3d getEntityPosition(Ref<EntityStore> ref) {
      try {
         Store<EntityStore> store = ref.getStore();
         if (store == null) {
            return null;
         }

         TransformComponent transform = (TransformComponent)store.getComponent(ref, EcsReflectionUtil.TRANSFORM_TYPE);
         if (transform != null) {
            return transform.getPosition();
         }
      } catch (Exception var4) {
      }

      return null;
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

   private UUID getUuidFromRef(Ref<EntityStore> ref) {
      return ref != null ? EcsReflectionUtil.getUuidFromRef(ref) : null;
   }

   private void log(String msg) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[FeedAnimal] " + msg);
         }

      }
   }

   private AnimalType getAnimalTypeFromRef(Ref<EntityStore> ref, Store<EntityStore> store) {
      String modelAssetId = this.getModelAssetIdFromRef(ref, store);
      return modelAssetId != null ? AnimalType.fromModelAssetId(modelAssetId) : null;
   }

   private String getModelAssetIdFromRef(Ref<EntityStore> ref, Store<EntityStore> store) {
      try {
         ModelComponent modelComp = (ModelComponent)store.getComponent(ref, ModelComponent.getComponentType());
         if (modelComp == null) {
            return null;
         }

         Model model = modelComp.getModel();
         if (model != null) {
            return model.getModelAssetId();
         }
      } catch (Exception e) {
         Debug.log("Error getting model asset ID: " + e.getMessage(), Level.INFO);
      }

      return null;
   }
}
