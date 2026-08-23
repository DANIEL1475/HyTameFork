package com.hytame.managers;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.tame.HyTameComponent;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import com.hytame.util.NameplateUtil;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.joml.Vector3d;

public class RespawnManager {
   private TamingManager tamingManager;
   private BreedingManager breedingManager;
   private Supplier<ComponentType<EntityStore, HyTameComponent>> hyTameTypeSupplier;
   private Function<Ref<EntityStore>, Vector3d> positionGetter;
   private boolean _isFirstTimeRunning = true;
   private static final double RESPAWN_RADIUS = (double)64.0F;
   private final Set<UUID> recentlySpawnedHytameIds = ConcurrentHashMap.newKeySet();

   public void clearRecentlySpawned() {
      this.recentlySpawnedHytameIds.clear();
   }

   public void setTamingManager(TamingManager tamingManager) {
      this.tamingManager = tamingManager;
   }

   public void setBreedingManager(BreedingManager breedingManager) {
      this.breedingManager = breedingManager;
   }

   public void setHyTameTypeSupplier(Supplier<ComponentType<EntityStore, HyTameComponent>> supplier) {
      this.hyTameTypeSupplier = supplier;
   }

   public void setPositionGetter(Function<Ref<EntityStore>, Vector3d> getter) {
      this.positionGetter = getter;
   }

   private void logVerbose(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   private void logWarning(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atWarning()).log(message);
      }

   }

   public void updateTamedAnimalPositions() {
      if (this.tamingManager != null) {
         Map<String, List<TamedAnimalData>> animalsByWorld = new HashMap();

         for(TamedAnimalData data : this.tamingManager.getAllTamedAnimals()) {
            if (data != null && !data.isDespawned() && data.getEntityRef() != null) {
               String worldId = data.getWorldId();
               if (worldId == null) {
                  worldId = this.getWorldNameFromRef(data.getEntityRef());
                  if (worldId == null) {
                     worldId = "default";
                  }
               }

               ((List)animalsByWorld.computeIfAbsent(worldId, (k) -> new ArrayList())).add(data);
            }
         }

         int[] totalUpdated = new int[]{0};

         for(Map.Entry<String, List<TamedAnimalData>> entry : animalsByWorld.entrySet()) {
            String worldId = (String)entry.getKey();
            List<TamedAnimalData> animals = (List)entry.getValue();
            World world = "default".equals(worldId) ? Universe.get().getDefaultWorld() : Universe.get().getWorld(worldId);
            if (world == null) {
               world = Universe.get().getDefaultWorld();
            }

            if (world != null) {
               world.execute(() -> {
                  try {
                     int updated = 0;

                     for(TamedAnimalData data : animals) {
                        Object refObj = data.getEntityRef();
                        if (refObj != null) {
                           Ref<EntityStore> entityRef = (Ref)refObj;

                           try {
                              Vector3d pos = this.positionGetter != null ? (Vector3d)this.positionGetter.apply(entityRef) : null;
                              if (pos != null) {
                                 double dx = pos.x() - data.getLastX();
                                 double dy = pos.y() - data.getLastY();
                                 double dz = pos.z() - data.getLastZ();
                                 double distSq = dx * dx + dy * dy + dz * dz;
                                 if (distSq > (double)0.25F) {
                                    data.setLastPosition(pos.x(), pos.y(), pos.z());
                                    ++updated;
                                 }
                              }
                           } catch (Exception var19) {
                              data.setDespawned(true);
                              data.setEntityRef((Ref)null);
                           }
                        }
                     }

                     if (updated > 0) {
                        synchronized(totalUpdated) {
                           totalUpdated[0] += updated;
                        }
                     }
                  } catch (Exception var20) {
                  }

               });
            }
         }

         if (!animalsByWorld.isEmpty()) {
            World defaultWorld = Universe.get().getDefaultWorld();
            if (defaultWorld != null) {
               defaultWorld.execute(() -> {
                  if (totalUpdated[0] > 0) {
                     this.tamingManager.saveImmediately();
                     this.logVerbose("Updated positions for " + totalUpdated[0] + " tamed animals across all worlds");
                  }

               });
            }
         }

      }
   }

   public void checkAndRespawnTamedAnimals() {
      if (this.tamingManager == null) {
         this.logVerbose("[RespawnCheck] tamingManager is null");
      } else if (this.tamingManager.isInGracePeriod()) {
         this.logVerbose("[RespawnCheck] In initialization grace period - skipping respawn checks");
      } else {
         Collection<TamedAnimalData> allAnimals = this.tamingManager.getAllTamedAnimals();
         this.logVerbose("[RespawnCheck] Tamed animals count: " + allAnimals.size());
         if (!allAnimals.isEmpty()) {
            Map<String, List<TamedAnimalData>> animalsByWorld = new HashMap();

            for(TamedAnimalData data : allAnimals) {
               String worldId = data.getWorldId();
               if (worldId == null || worldId.isEmpty()) {
                  worldId = this.getWorldNameFromRef(data.getEntityRef());
                  if (worldId == null) {
                     worldId = "default";
                  }
               }

               ((List)animalsByWorld.computeIfAbsent(worldId, (k) -> new ArrayList())).add(data);
            }

            for(Map.Entry<String, List<TamedAnimalData>> entry : animalsByWorld.entrySet()) {
               String worldName = (String)entry.getKey();
               List<TamedAnimalData> worldAnimals = (List)entry.getValue();
               World world = "default".equals(worldName) ? Universe.get().getDefaultWorld() : Universe.get().getWorld(worldName);
               if (world == null) {
                  this.logVerbose("[RespawnCheck] World not found: " + worldName + ", trying default");
                  world = Universe.get().getDefaultWorld();
               }

               if (world == null) {
                  this.logVerbose("[RespawnCheck] No world available for: " + worldName);
               } else {
                  world.execute(() -> {
                     try {
                        Store<EntityStore> store = world.getEntityStore().getStore();
                        Map<UUID, Ref<EntityStore>> entitiesByHytameId = new HashMap();
                        ComponentType<EntityStore, HyTameComponent> hyTameType = this.hyTameTypeSupplier != null ? (ComponentType)this.hyTameTypeSupplier.get() : null;
                        if (hyTameType != null) {
                           store.forEachChunk((chunk, buffer) -> {
                              int chunkSize = chunk.size();

                              for(int i = 0; i < chunkSize; ++i) {
                                 try {
                                    HyTameComponent hyTameComp = (HyTameComponent)chunk.getComponent(i, hyTameType);
                                    if (hyTameComp != null && hyTameComp.isTamed() && hyTameComp.getHytameId() != null) {
                                       Ref<EntityStore> ref = chunk.getReferenceTo(i);
                                       if (ref != null && ref.isValid()) {
                                          NPCEntity npc = (NPCEntity)store.getComponent(ref, EcsReflectionUtil.NPC_TYPE);
                                          DespawnComponent despawn = (DespawnComponent)store.getComponent(ref, EcsReflectionUtil.DESPAWN_TYPE);
                                          if (npc != null && !npc.isDespawning() && despawn == null) {
                                             entitiesByHytameId.put(hyTameComp.getHytameId(), ref);
                                          }
                                       }
                                    }
                                 } catch (Exception var11) {
                                 }
                              }

                           });
                           this.logVerbose("[RespawnCheck] World " + worldName + ": Found " + entitiesByHytameId.size() + " entities with HyTameComponent");
                        }

                        for(TamedAnimalData tamedData : worldAnimals) {
                           if (!tamedData.isDead()) {
                              if (this._isFirstTimeRunning) {
                                 tamedData.setRespawnInProgress(false);
                              }

                              boolean entityExists = false;
                              Ref<EntityStore> tamedRef = tamedData.getEntityRef();
                              UUID hytameId = tamedData.getHytameId();
                              if (tamedRef != null && tamedRef.isValid()) {
                                 try {
                                    NPCEntity npcEntity = (NPCEntity)store.getComponent(tamedRef, EcsReflectionUtil.NPC_TYPE);
                                    DespawnComponent despawnComp = (DespawnComponent)store.getComponent(tamedRef, EcsReflectionUtil.DESPAWN_TYPE);
                                    if (npcEntity != null && !npcEntity.isDespawning() && despawnComp == null) {
                                       entityExists = true;
                                       if (this.positionGetter != null && tamedData.getLastX() == (double)0.0F && tamedData.getLastY() == (double)0.0F && tamedData.getLastZ() == (double)0.0F) {
                                          Vector3d refPos = (Vector3d)this.positionGetter.apply(tamedRef);
                                          if (refPos != null) {
                                             tamedData.setLastPosition(refPos.x(), refPos.y(), refPos.z());
                                          }
                                       }
                                    }
                                 } catch (ArrayIndexOutOfBoundsException var19) {
                                 }
                              }

                              if (!entityExists && hytameId != null) {
                                 Ref<EntityStore> foundRef = (Ref)entitiesByHytameId.get(hytameId);
                                 entitiesByHytameId.remove(hytameId);
                                 if (foundRef != null && foundRef.isValid()) {
                                    try {
                                       tamedData.setEntityRef(foundRef);
                                       entityExists = true;
                                       this.logVerbose("[RespawnCheck] Found entity by HytameId: " + tamedData.getCustomName());
                                       if (this.positionGetter != null) {
                                          Vector3d linkPos = (Vector3d)this.positionGetter.apply(foundRef);
                                          if (linkPos != null) {
                                             tamedData.setLastPosition(linkPos.x(), linkPos.y(), linkPos.z());
                                          }
                                       }

                                       UUIDComponent uuidComp = (UUIDComponent)store.getComponent(foundRef, EcsReflectionUtil.UUID_TYPE);
                                       if (uuidComp != null) {
                                          UUID newUuid = uuidComp.getUuid();
                                          if (newUuid != null && !newUuid.equals(tamedData.getAnimalUuid())) {
                                             this.tamingManager.markRespawned(tamedData.getAnimalUuid(), newUuid, foundRef);
                                          }
                                       }

                                       this.tamingManager.onEntityLinked(hytameId);
                                    } catch (ArrayIndexOutOfBoundsException var18) {
                                       entityExists = false;
                                    }
                                 }
                              }

                              if (entityExists) {
                                 if (tamedData.isDespawned()) {
                                    tamedData.setDespawned(false);
                                    this.logVerbose("[RespawnCheck] Entity found for: " + tamedData.getCustomName() + ", unmarking despawned");
                                    if (hytameId != null) {
                                       this.tamingManager.onEntityLinked(hytameId);
                                    }
                                 }
                              } else if (!tamedData.isDespawned()) {
                                 double x = tamedData.getLastX();
                                 double y = tamedData.getLastY();
                                 double z = tamedData.getLastZ();
                                 this.tamingManager.onTamedAnimalDespawn(tamedData.getAnimalUuid(), x, y, z);
                                 this.logVerbose("[RespawnCheck] Marking despawned: " + tamedData.getCustomName());
                              }
                           }
                        }

                        this._isFirstTimeRunning = false;

                        for(Player player : world.getPlayers()) {
                           try {
                              Vector3d playerPos = EntityUtil.getEntityPosition(player);
                              if (playerPos != null) {
                                 for(TamedAnimalData tamedData : this.tamingManager.getDespawnedAnimalsInRegion(playerPos.x(), playerPos.z(), (double)64.0F)) {
                                    String animalWorld = tamedData.getWorldId();
                                    if (animalWorld == null || animalWorld.isEmpty()) {
                                       animalWorld = this.getWorldNameFromRef(tamedData.getEntityRef());
                                       if (animalWorld == null) {
                                          animalWorld = "default";
                                       }
                                    }

                                    if (worldName.equals(animalWorld) && !tamedData.isDead()) {
                                       this.respawnTamedAnimal(world, store, tamedData);
                                    }
                                 }
                              }
                           } catch (Exception e) {
                              this.logVerbose("[RespawnCheck] Error processing player: " + e.getMessage());
                           }
                        }
                     } catch (Exception e) {
                        this.logWarning("[RespawnCheck] Exception in world.execute: " + e.getMessage());
                        e.printStackTrace();
                     }

                  });
               }
            }

         }
      }
   }

   private void respawnTamedAnimal(World world, Store<EntityStore> store, TamedAnimalData tamedData) {
      if (tamedData != null && tamedData.isDespawned()) {
         GrowthStage growthStage = tamedData.getGrowthStage();
         if (growthStage != GrowthStage.BABY && growthStage != GrowthStage.JUVENILE) {
            tamedData.setRespawnInProgress(true);
            AnimalType animalType = tamedData.getAnimalType();
            if (animalType != null) {
               UUID animalUuid = tamedData.getAnimalUuid();
               if (tamedData.isCaptured()) {
                  this.logVerbose("[Respawn] Skipping - animal is in storage (coop or capture crate): " + tamedData.getCustomName());
               } else {
                  UUID hytameId = tamedData.getHytameId();
                  if (hytameId != null) {
                     if (this.recentlySpawnedHytameIds.contains(hytameId)) {
                        this.logVerbose("[Respawn] Skipping duplicate respawn for hytameId: " + String.valueOf(hytameId));
                        return;
                     }

                     this.recentlySpawnedHytameIds.add(hytameId);
                  }

                  Vector3d spawnPos = new Vector3d(tamedData.getLastX(), tamedData.getLastY() + (double)0.5F, tamedData.getLastZ());
                  UUID oldUuid = tamedData.getAnimalUuid();
                  AnimalType finalAnimalType = animalType;
                  TamedAnimalData finalTamedData = tamedData;

                  try {
                     String roleId;
                     if (tamedData.getGrowthStage() != GrowthStage.ADULT && tamedData.getGrowthStage() != null) {
                        if (finalAnimalType.usesVanillaTaming()) {
                           String tamedBabyRole = finalAnimalType.getVanillaTamedBabyRoleName();
                           roleId = tamedBabyRole != null ? tamedBabyRole : finalAnimalType.getVanillaTamedRoleName();
                        } else {
                           roleId = finalAnimalType.hasBabyVariant() ? finalAnimalType.getBabyNpcRoleId() : finalAnimalType.getAdultNpcRoleId();
                        }
                     } else {
                        roleId = finalAnimalType.usesVanillaTaming() ? finalAnimalType.getVanillaTamedRoleName() : finalAnimalType.getAdultNpcRoleId();
                     }

                     NPCPlugin npcPlugin = NPCPlugin.get();
                     int roleIndex = npcPlugin.getIndex(roleId);
                     if (roleIndex < 0) {
                        this.logWarning("Could not find role for respawn: " + roleId);
                        return;
                     }

                     Rotation3f rotation = new Rotation3f(0.0F, finalTamedData.getLastRotation(), 0.0F);
                     Pair<Ref<EntityStore>, NPCEntity> newNpc = NPCPlugin.get().spawnEntity(store, roleIndex, spawnPos, rotation, (Model)null, (TriConsumer)null);
                     tamedData.setRespawnInProgress(false);
                     if (newNpc != null) {
                        Ref<EntityStore> entityRef = (Ref)newNpc.first();
                        if (entityRef != null) {
                           UUID newUuid = null;

                           try {
                              UUIDComponent uuidComp = (UUIDComponent)store.getComponent(entityRef, EcsReflectionUtil.UUID_TYPE);
                              if (uuidComp != null) {
                                 newUuid = uuidComp.getUuid();
                              }
                           } catch (Exception var24) {
                              newUuid = UUID.randomUUID();
                           }

                           if (newUuid == null) {
                              newUuid = UUID.randomUUID();
                           }

                           this.tamingManager.markRespawned(oldUuid, newUuid, entityRef);
                           UUID ownerUuid = finalTamedData.getOwnerUuid();
                           String ownerName = finalTamedData.getOwnerName();
                           if (ownerUuid != null && this.hyTameTypeSupplier != null) {
                              String effectiveOwnerName = ownerName != null ? ownerName : "Unknown";
                              ComponentType<EntityStore, HyTameComponent> hyTameType = (ComponentType)this.hyTameTypeSupplier.get();
                              if (hyTameType != null) {
                                 HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(entityRef, hyTameType);
                                 if (hyTameComp != null) {
                                    hyTameComp.setTamed(ownerUuid, effectiveOwnerName);
                                    if (hytameId != null) {
                                       hyTameComp.setHytameId(hytameId);
                                    }

                                    this.logVerbose("Set HyTameComponent on respawned entity: owner=" + effectiveOwnerName + ", hytameId=" + String.valueOf(hytameId));
                                 }
                              }
                           }

                           BreedingData bData = this.breedingManager.getOrCreateData(newUuid, finalAnimalType);
                           finalTamedData.applyToBreedingData(bData);
                           bData.setTamed(true, finalTamedData.getOwnerUuid());
                           bData.setCustomName(finalTamedData.getCustomName());
                           bData.setEntityRef(entityRef);
                           String customName = finalTamedData.getCustomName();
                           if (customName != null && !customName.isEmpty() && !customName.equalsIgnoreCase("_UNDEFINED")) {
                              NameplateUtil.setEntityNameplate(entityRef, customName);
                           }

                           String var10001 = finalTamedData.getCustomName();
                           this.logVerbose("Respawned tamed animal: " + var10001 + " (" + String.valueOf(finalAnimalType) + ")");
                        }
                     }
                  } catch (Exception e) {
                     this.logWarning("Failed to respawn tamed animal: " + e.getMessage());
                  }

               }
            }
         } else {
            this.logVerbose("[Respawn] Skipping - animal is a baby/juvenile: " + tamedData.getCustomName());
         }
      }
   }

   public static double getRespawnRadius() {
      return (double)64.0F;
   }

   private String getWorldNameFromRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         try {
            UUID entityUuid = EcsReflectionUtil.getUuidFromRef(ref);
            if (entityUuid == null) {
               this.logVerbose("[WorldDebug] Could not get UUID from ref");
               return null;
            }

            this.logVerbose("[WorldDebug] Searching all worlds for entity UUID: " + String.valueOf(entityUuid));

            for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
               String worldName = (String)entry.getKey();
               World world = (World)entry.getValue();
               if (world != null) {
                  try {
                     Store<EntityStore> store = world.getEntityStore().getStore();
                     if (store != null && ref.getStore() == store) {
                        this.logVerbose("[WorldDebug] Found entity in world: " + worldName);
                        return worldName;
                     }
                  } catch (Exception var8) {
                  }
               }
            }

            this.logVerbose("[WorldDebug] Entity not found in any world");
         } catch (Exception e) {
            this.logVerbose("getWorldNameFromRef error: " + e.getMessage());
         }

         return null;
      }
   }
}
