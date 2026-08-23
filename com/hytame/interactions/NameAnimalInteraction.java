package com.hytame.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.ui.NametagUIPage;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import com.hytame.util.TameHelper;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.joml.Vector3d;

public class NameAnimalInteraction extends SimpleInteraction {
   public static final BuilderCodec<NameAnimalInteraction> CODEC;
   private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE;
   private static final ComponentType<EntityStore, ModelComponent> MODEL_TYPE;
   private static final ComponentType<EntityStore, UUIDComponent> UUID_TYPE;
   private static final String[] RANDOM_NAMES;
   private static final Random random;
   private boolean shouldFail = false;

   protected void tick0(boolean firstRun, float time, InteractionType type, InteractionContext context, CooldownHandler cooldownHandler) {
      if (firstRun) {
         this.shouldFail = false;

         try {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin == null) {
               this.shouldFail = true;
            } else {
               TamingManager tamingManager = plugin.getTamingManager();
               BreedingManager breedingManager = plugin.getBreedingManager();
               if (tamingManager != null && breedingManager != null) {
                  Ref<EntityStore> targetRef = context.getTargetEntity();
                  if (targetRef == null) {
                     this.log("No target entity");
                     this.shouldFail = true;
                  } else {
                     UUID animalUuid = this.getUuidFromRef(targetRef);
                     if (!tamingManager.isTamed(animalUuid)) {
                        this.log("Animal is not tamed");
                        this.shouldFail = true;
                     } else if (this.isPlayerEntity(targetRef)) {
                        this.log("Target is a player, skipping");
                        this.shouldFail = true;
                     } else {
                        String modelAssetId = this.getModelAssetIdFromEntity(targetRef);
                        if (modelAssetId == null) {
                           this.log("Could not get model asset ID");
                           this.shouldFail = true;
                        } else {
                           AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
                           if (animalType != null || plugin.getConfigManager() != null && plugin.getConfigManager().isCustomAnimal(modelAssetId)) {
                              boolean tamingEnabled = false;
                              if (animalType != null) {
                                 tamingEnabled = plugin.getConfigManager().isTamingEnabled(animalType);
                              } else {
                                 tamingEnabled = plugin.getConfigManager().isCustomAnimalTamingEnabled(modelAssetId);
                              }

                              if (!tamingEnabled) {
                                 this.sendPlayerMessage(context, "Taming is disabled for this animal!", "#FF5555");
                                 this.shouldFail = true;
                              } else {
                                 UUID playerUuid = this.getPlayerUuid(context);
                                 String playerName = this.getPlayerName(context);
                                 if (playerUuid == null) {
                                    this.log("Could not get player UUID");
                                    this.shouldFail = true;
                                 } else if (animalUuid == null) {
                                    this.log("Could not get animal UUID");
                                    this.shouldFail = true;
                                 } else {
                                    boolean isEcsTamed = TameHelper.isTamed(targetRef);
                                    UUID ecsOwner = TameHelper.getOwnerUuid(targetRef);
                                    TamedAnimalData existingTamed = tamingManager.getTamedData(animalUuid);
                                    if (existingTamed != null && !existingTamed.isOwnedBy(playerUuid)) {
                                       this.sendPlayerMessage(context, "This animal belongs to someone else!", "#FF5555");
                                       this.shouldFail = true;
                                    } else if (isEcsTamed && ecsOwner != null && !ecsOwner.equals(playerUuid)) {
                                       this.sendPlayerMessage(context, "This animal belongs to someone else!", "#FF5555");
                                       this.shouldFail = true;
                                    } else {
                                       if (isEcsTamed && existingTamed == null && ecsOwner != null && ecsOwner.equals(playerUuid)) {
                                          UUID hytameId = TameHelper.getHytameId(targetRef);
                                          if (TameHelper.getHyTameComponent(targetRef) != null) {
                                             TameHelper.getHyTameComponent(targetRef).getTamerName();
                                          }

                                          String var10000 = animalType != null ? animalType.name() : modelAssetId;
                                          String defaultName = var10000 + "_" + animalUuid.toString().substring(0, 4);
                                          String worldName = this.getWorldNameFromRef(targetRef);
                                          Vector3d syncPos = EntityUtil.getPositionFromRef(targetRef);
                                          double syncX = syncPos != null ? syncPos.x() : (double)0.0F;
                                          double syncY = syncPos != null ? syncPos.y() : (double)0.0F;
                                          double syncZ = syncPos != null ? syncPos.z() : (double)0.0F;
                                          tamingManager.tameAnimal(hytameId, animalUuid, playerUuid, defaultName, animalType, targetRef, syncX, syncY, syncZ, GrowthStage.ADULT, worldName);
                                          String var10001 = String.valueOf(animalUuid);
                                          this.log("Synced ECS tame state to TamingManager for " + var10001 + " in world " + worldName);
                                          existingTamed = tamingManager.getTamedData(animalUuid);
                                       }

                                       Player player = this.getPlayerFromContext(context);
                                       if (player == null) {
                                          this.log("Could not get player object");
                                          this.shouldFail = true;
                                       } else {
                                          try {
                                             PlayerRef playerRef = player.getPlayerRef();
                                             String animalDisplayName = animalType != null ? animalType.name() : modelAssetId;
                                             String existingName = existingTamed != null ? existingTamed.getCustomName() : null;
                                             Ref<EntityStore> playerEntityRef = context.getEntity();
                                             Store<EntityStore> store = playerEntityRef.getStore();
                                             NametagUIPage nametagPage = new NametagUIPage(playerRef, targetRef, playerUuid, animalDisplayName, existingName);
                                             player.getPageManager().openCustomPage(playerEntityRef, store, nametagPage);
                                             this.log("Opened nametag UI for " + modelAssetId);
                                          } catch (Exception e) {
                                             this.log("Failed to open nametag UI: " + e.getMessage());
                                             String name = RANDOM_NAMES[random.nextInt(RANDOM_NAMES.length)];
                                             GrowthStage growthStage = GrowthStage.ADULT;
                                             BreedingData breedingData = breedingManager.getData(animalUuid);
                                             if (breedingData != null && breedingData.getGrowthStage() != null) {
                                                growthStage = breedingData.getGrowthStage();
                                             } else if (modelAssetId != null && (modelAssetId.contains("_Calf") || modelAssetId.contains("_Piglet") || modelAssetId.contains("_Chick") || modelAssetId.contains("_Lamb") || modelAssetId.contains("_Foal") || modelAssetId.contains("_Bunny"))) {
                                                growthStage = GrowthStage.BABY;
                                             }

                                             Vector3d pos = this.getEntityPosition(targetRef);
                                             double x = pos != null ? pos.x() : (double)0.0F;
                                             double y = pos != null ? pos.y() : (double)0.0F;
                                             double z = pos != null ? pos.z() : (double)0.0F;
                                             String worldName = this.getWorldNameFromRef(targetRef);
                                             TamedAnimalData tamedData = tamingManager.tameAnimal(animalUuid, playerUuid, name, animalType, targetRef, x, y, z, growthStage, worldName);
                                             if (tamedData != null) {
                                                this.consumePlayerHeldItem(context);
                                                this.sendPlayerMessage(context, name + " is now yours!", "#55FF55");
                                                this.playTamingSound(targetRef);
                                                this.spawnHeartParticles(targetRef);
                                             }
                                          }

                                          this.shouldFail = true;
                                       }
                                    }
                                 }
                              }
                           } else {
                              this.sendPlayerMessage(context, "This is not an animal!", "#FF5555");
                              this.shouldFail = true;
                           }
                        }
                     }
                  }
               } else {
                  this.shouldFail = true;
               }
            }
         } catch (Exception e) {
            this.log("Error in NameAnimalInteraction: " + e.getMessage());
            this.shouldFail = true;
         }
      } else {
         if (!this.shouldFail) {
            super.tick0(firstRun, time, type, context, cooldownHandler);
         }

      }
   }

   private void log(String message) {
      HyTamePlugin p = HyTamePlugin.getInstance();
      if (p != null && HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)p.getLogger().atInfo()).log("[NameAnimal] " + message);
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

   private UUID getUuidFromRef(Ref<EntityStore> ref) {
      try {
         Store<EntityStore> store = ref.getStore();
         if (store != null) {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUID_TYPE);
            if (uuidComp != null && uuidComp.getUuid() != null) {
               return uuidComp.getUuid();
            }
         }
      } catch (Exception var4) {
      }

      return UUID.nameUUIDFromBytes(ref.toString().getBytes());
   }

   private UUID getPlayerUuidFromPlayer(Player player) {
      try {
         Ref<EntityStore> entityRef = player.getReference();
         if (entityRef instanceof Ref) {
            Store<EntityStore> store = entityRef.getStore();
            if (store != null) {
               UUIDComponent uuidComp = (UUIDComponent)store.getComponent(entityRef, UUID_TYPE);
               if (uuidComp != null) {
                  return uuidComp.getUuid();
               }
            }
         }
      } catch (Exception var5) {
      }

      return null;
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
            } else {
               Model model = modelComp.getModel();
               return model == null ? null : model.getModelAssetId();
            }
         }
      } catch (Exception var5) {
         return null;
      }
   }

   private UUID getPlayerUuid(InteractionContext context) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         return entityRef == null ? null : this.getUuidFromRef(entityRef);
      } catch (Exception var3) {
         return null;
      }
   }

   private String getPlayerName(InteractionContext context) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return null;
         }

         UUID playerUuid = this.getUuidFromRef(entityRef);

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               for(Player player : world.getPlayers()) {
                  UUID pUuid = this.getPlayerUuidFromPlayer(player);
                  if (playerUuid != null && playerUuid.equals(pUuid)) {
                     return player.getLegacyDisplayName();
                  }
               }
            }
         }
      } catch (Exception var10) {
      }

      return null;
   }

   private Player getPlayerFromContext(InteractionContext context) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return null;
         }

         UUID playerUuid = this.getUuidFromRef(entityRef);

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               for(Player player : world.getPlayers()) {
                  UUID pUuid = this.getPlayerUuidFromPlayer(player);
                  if (playerUuid != null && playerUuid.equals(pUuid)) {
                     return player;
                  }
               }
            }
         }
      } catch (Exception var10) {
      }

      return null;
   }

   private Vector3d getEntityPosition(Ref<EntityStore> targetRef) {
      try {
         Store<EntityStore> store = targetRef.getStore();
         if (store == null) {
            return null;
         }

         TransformComponent transform = (TransformComponent)store.getComponent(targetRef, TRANSFORM_TYPE);
         if (transform != null) {
            return transform.getPosition();
         }
      } catch (Exception var4) {
      }

      return null;
   }

   private void sendPlayerMessage(InteractionContext context, String message, String color) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return;
         }

         UUID playerUuid = this.getUuidFromRef(entityRef);

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               for(Player player : world.getPlayers()) {
                  UUID pUuid = this.getPlayerUuidFromPlayer(player);
                  if (playerUuid != null && playerUuid.equals(pUuid)) {
                     player.getPlayerRef().sendMessage(Message.raw(message).color(color));
                     return;
                  }
               }
            }
         }
      } catch (Exception var12) {
      }

   }

   private void consumePlayerHeldItem(InteractionContext context) {
      try {
         ItemStack heldItem = context.getHeldItem();
         if (heldItem == null) {
            return;
         }

         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return;
         }

         UUID playerUuid = this.getUuidFromRef(entityRef);

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               for(Player player : world.getPlayers()) {
                  UUID pUuid = this.getPlayerUuidFromPlayer(player);
                  if (playerUuid != null && playerUuid.equals(pUuid)) {
                     Inventory inventory = ((LivingEntity)player).getInventory();
                     if (inventory != null) {
                        byte activeSlot = inventory.getActiveHotbarSlot();
                        inventory.getHotbar().removeItemStackFromSlot((short)activeSlot, 1);
                     }

                     return;
                  }
               }
            }
         }
      } catch (Exception e) {
         this.log("Error consuming item: " + e.getMessage());
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

         int soundId = SoundEvent.getAssetMap().getIndex("SFX_UI_Level_Up");
         if (soundId >= 0) {
            SoundUtil.playSoundEvent3d(soundId, SoundCategory.SFX, pos.x(), pos.y(), pos.z(), store);
         }
      } catch (Exception var5) {
      }

   }

   private void spawnHeartParticles(Ref<EntityStore> targetRef) {
      try {
         Vector3d position = this.getEntityPosition(targetRef);
         if (position == null) {
            return;
         }

         double x = position.x();
         double y = position.y() + (double)1.5F;
         double z = position.z();
         Store<EntityStore> store = targetRef.getStore();
         if (store == null) {
            return;
         }

         Vector3d heartsPos = new Vector3d(x, y, z);
         ParticleUtil.spawnParticleEffect("BreedingHearts", heartsPos, store);
      } catch (Exception var11) {
      }

   }

   private String getWorldNameFromRef(Ref<EntityStore> ref) {
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
                        return (String)entry.getKey();
                     }
                  } catch (Exception var7) {
                  }
               }
            }
         } catch (Exception var8) {
         }

         return null;
      }
   }

   static {
      CODEC = BuilderCodec.builder(NameAnimalInteraction.class, NameAnimalInteraction::new, SimpleInteraction.CODEC).build();
      TRANSFORM_TYPE = EcsReflectionUtil.TRANSFORM_TYPE;
      MODEL_TYPE = EcsReflectionUtil.MODEL_TYPE;
      UUID_TYPE = EcsReflectionUtil.UUID_TYPE;
      RANDOM_NAMES = new String[]{"Fluffy", "Spot", "Buddy", "Max", "Bella", "Charlie", "Luna", "Milo", "Coco", "Rocky", "Daisy", "Duke", "Sadie", "Bear", "Molly", "Tucker", "Bailey", "Maggie", "Jack", "Sophie", "Oliver", "Lucy", "Buster", "Chloe", "Teddy", "Penny", "Zeus", "Zoey", "Gus", "Lily", "Winston", "Gracie"};
      random = new Random();
   }
}
