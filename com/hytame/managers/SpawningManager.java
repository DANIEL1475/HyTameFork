package com.hytame.managers;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.tame.HyTameComponent;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.NameplateUtil;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.joml.Vector3d;

public class SpawningManager {
   private BreedingManager breedingManager;
   private TamingManager tamingManager;
   private Supplier<ComponentType<EntityStore, HyTameComponent>> hyTameTypeSupplier;
   private Function<Object[], String> modelAssetIdGetter;

   public void setBreedingManager(BreedingManager breedingManager) {
      this.breedingManager = breedingManager;
   }

   public void setTamingManager(TamingManager tamingManager) {
      this.tamingManager = tamingManager;
   }

   public void setHyTameTypeSupplier(Supplier<ComponentType<EntityStore, HyTameComponent>> supplier) {
      this.hyTameTypeSupplier = supplier;
   }

   public void setModelAssetIdGetter(Function<Object[], String> getter) {
      this.modelAssetIdGetter = getter;
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

   private void logError(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atSevere()).log(message);
      }

   }

   public void spawnBabyAnimal(AnimalType animalType, Vector3d position, UUID parent1Id, UUID parent2Id) {
      this.spawnBabyAnimal(animalType, position, parent1Id, parent2Id, (String)null);
   }

   public void spawnBabyAnimal(AnimalType animalType, Vector3d position, UUID parent1Id, UUID parent2Id, String worldName) {
      try {
         boolean hasBabyVariant = animalType.hasBabyVariant();
         String roleId;
         if (animalType.usesVanillaTaming()) {
            if (hasBabyVariant) {
               String tamedBabyRole = animalType.getVanillaTamedBabyRoleName();
               roleId = tamedBabyRole != null ? tamedBabyRole : animalType.getBabyNpcRoleId();
            } else {
               roleId = animalType.getVanillaTamedRoleName();
            }
         } else {
            roleId = hasBabyVariant ? animalType.getBabyNpcRoleId() : animalType.getAdultNpcRoleId();
         }

         float initialScale = hasBabyVariant ? 1.0F : animalType.getScaleForStage(GrowthStage.BABY);
         this.logVerbose("Attempting to spawn " + (hasBabyVariant ? "baby" : "scaled adult") + ": " + roleId + (hasBabyVariant ? "" : " at scale " + initialScale));
         World world = null;
         if (worldName != null) {
            world = Universe.get().getWorld(worldName);
            this.logVerbose("Using world: " + worldName);
         }

         if (world == null) {
            world = Universe.get().getDefaultWorld();
            this.logVerbose("Using default world");
         }

         if (world == null) {
            this.logWarning("Cannot spawn baby - world is null");
            return;
         }

         world.execute(() -> {
            try {
               Store<EntityStore> store = world.getEntityStore().getStore();
               boolean roleExists = NPCPlugin.get().hasRoleName(roleId);
               if (!roleExists) {
                  this.logWarning("NPC role not found: " + roleId);
                  return;
               }

               Rotation3f rotation = new Rotation3f(0.0F, 0.0F, 0.0F);
               Model scaledModel = null;
               if (!hasBabyVariant) {
                  try {
                     DefaultAssetMap<String, ModelAsset> assetMap = ModelAsset.getAssetMap();
                     ModelAsset modelAsset = (ModelAsset)assetMap.getAsset(animalType.getModelAssetId());
                     if (modelAsset != null) {
                        scaledModel = Model.createScaledModel(modelAsset, initialScale);
                     }
                  } catch (Exception e) {
                     this.logWarning("Failed to create scaled model: " + e.getMessage());
                  }
               }

               Ref<EntityStore> entityRef = null;
               int roleIndex = NPCPlugin.get().getIndex(roleId);
               if (roleIndex >= 0) {
                  try {
                     NPCPlugin.get().validateSpawnableRole(roleId);
                  } catch (Exception var23) {
                  }

                  try {
                     NPCPlugin.get().prepareRoleBuilderInfo(roleIndex);
                  } catch (Exception var22) {
                  }

                  try {
                     entityRef = (Ref)NPCPlugin.get().spawnEntity(store, roleIndex, position, rotation, scaledModel, (TriConsumer)null, (TriConsumer)null).first();
                  } catch (Exception var21) {
                  }
               }

               if (entityRef != null) {
                  String logMessage = hasBabyVariant ? "Baby " + animalType.getId() + " born" : "Young " + animalType.getId() + " born (scale " + String.format("%.1f", initialScale) + ")";
                  this.logVerbose("[HyTame] " + logMessage + " at " + String.format("%.0f, %.0f, %.0f", position.x(), position.y(), position.z()));
                  UUID babyId = EcsReflectionUtil.getUuidFromRef(entityRef);
                  this.breedingManager.registerBaby(babyId, animalType, entityRef);
                  if (this.hyTameTypeSupplier != null) {
                     ComponentType<EntityStore, HyTameComponent> hyTameType = (ComponentType)this.hyTameTypeSupplier.get();
                     if (hyTameType != null) {
                        HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(entityRef, hyTameType);
                        if (hyTameComp != null) {
                           hyTameComp.setGrowthStage(GrowthStage.BABY);
                           this.logVerbose("Set HyTameComponent.growthStage = BABY for new baby");
                        }
                     }
                  }

                  this.autoTameBabyIfParentsTamed(store, entityRef, babyId, animalType, position, parent1Id, parent2Id, worldName);
               } else {
                  this.logWarning("Failed to spawn " + (hasBabyVariant ? "baby" : "young") + " " + animalType.getId() + " - spawn returned null");
               }
            } catch (Exception e) {
               this.logError("Error spawning baby: " + e.getMessage());
            }

         });
      } catch (Exception e) {
         this.logError("Error in spawnBabyAnimal: " + e.getMessage());
      }

   }

   private void autoTameBabyIfParentsTamed(Store<EntityStore> store, Ref<EntityStore> entityRef, UUID babyId, AnimalType animalType, Vector3d spawnPos, UUID parent1Id, UUID parent2Id, String worldName) {
      if (this.tamingManager != null && parent1Id != null && parent2Id != null) {
         TamedAnimalData parent1Data = this.tamingManager.getTamedData(parent1Id);
         TamedAnimalData parent2Data = this.tamingManager.getTamedData(parent2Id);
         String var10001 = String.valueOf(parent1Id);
         this.logVerbose("Parent1 UUID: " + var10001 + " -> data: " + (parent1Data != null ? "found" : "NOT FOUND"));
         var10001 = String.valueOf(parent2Id);
         this.logVerbose("Parent2 UUID: " + var10001 + " -> data: " + (parent2Data != null ? "found" : "NOT FOUND"));
         if (parent1Data == null || parent2Data == null) {
            for(TamedAnimalData candidate : this.tamingManager.getAllTamedAnimals()) {
               if (candidate.getAnimalType() == animalType) {
                  double dx = candidate.getLastX() - spawnPos.x();
                  double dy = candidate.getLastY() - spawnPos.y();
                  double dz = candidate.getLastZ() - spawnPos.z();
                  double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                  if (!(dist > (double)10.0F)) {
                     if (parent1Data == null) {
                        parent1Data = candidate;
                        Object[] var10002 = new Object[]{dist};
                        this.logVerbose("Fallback: found parent1 by proximity (" + String.format("%.1f", var10002) + " blocks)");
                     } else if (parent2Data == null && candidate != parent1Data) {
                        parent2Data = candidate;
                        Object[] var26 = new Object[]{dist};
                        this.logVerbose("Fallback: found parent2 by proximity (" + String.format("%.1f", var26) + " blocks)");
                        break;
                     }
                  }
               }
            }
         }

         if (parent1Data != null && parent2Data != null) {
            UUID ownerUuid = parent1Data.getOwnerUuid();
            String ownerName = parent1Data.getOwnerName();
            if (ownerUuid == null) {
               ownerUuid = parent2Data.getOwnerUuid();
               ownerName = parent2Data.getOwnerName();
            }

            if (ownerUuid != null) {
               if (ownerName == null) {
                  ownerName = "Unknown";
               }

               String babyName = "_UNDEFINED";
               TamedAnimalData babyTameData = this.tamingManager.tameAnimal(babyId, ownerUuid, babyName, animalType, entityRef, spawnPos.x(), spawnPos.y(), spawnPos.z(), GrowthStage.BABY, worldName);
               if (babyTameData != null) {
                  babyTameData.setOwnerName(ownerName);
                  if (ownerUuid != null && this.hyTameTypeSupplier != null) {
                     ComponentType<EntityStore, HyTameComponent> hyTameType = (ComponentType)this.hyTameTypeSupplier.get();
                     if (hyTameType != null) {
                        HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(entityRef, hyTameType);
                        if (hyTameComp != null) {
                           hyTameComp.setTamed(ownerUuid, ownerName);
                           if (babyTameData.getHytameId() != null) {
                              hyTameComp.setHytameId(babyTameData.getHytameId());
                           }

                           this.logVerbose("Set HyTameComponent on baby: owner=" + ownerName + ", hytameId=" + String.valueOf(babyTameData.getHytameId()));
                        }
                     }
                  }

                  this.logVerbose("Auto-tamed baby " + babyName + " (UUID: " + String.valueOf(babyId) + ") with growthStage: " + String.valueOf(babyTameData.getGrowthStage()) + " to owner of both parents");
               } else {
                  this.logVerbose("Failed to auto-tame baby - tameAnimal returned null");
               }

            }
         }
      }
   }

   public void spawnCustomAnimalBaby(String modelAssetId, CustomAnimalConfig customConfig, Vector3d position) {
      this.spawnCustomAnimalBaby(modelAssetId, customConfig, position, (String)null, (UUID)null, (UUID)null);
   }

   public void spawnCustomAnimalBaby(String modelAssetId, CustomAnimalConfig customConfig, Vector3d position, String worldName) {
      this.spawnCustomAnimalBaby(modelAssetId, customConfig, position, worldName, (UUID)null, (UUID)null);
   }

   public void spawnCustomAnimalBaby(String modelAssetId, CustomAnimalConfig customConfig, Vector3d position, String worldName, UUID parent1Id, UUID parent2Id) {
      try {
         World world = null;
         if (worldName != null) {
            world = Universe.get().getWorld(worldName);
         }

         if (world == null) {
            world = Universe.get().getDefaultWorld();
         }

         if (world == null) {
            return;
         }

         world.execute(() -> {
            try {
               Store<EntityStore> store = world.getEntityStore().getStore();
               String usedRoleName = null;
               boolean usingBabyRole = false;
               boolean roleExists = false;
               if (customConfig != null && customConfig.getBabyNpcRoleId() != null) {
                  roleExists = NPCPlugin.get().hasRoleName(customConfig.getBabyNpcRoleId());
                  if (roleExists) {
                     usedRoleName = customConfig.getBabyNpcRoleId();
                     usingBabyRole = true;
                     this.logVerbose("Using dedicated baby NPC role: " + usedRoleName);
                  }
               }

               if (!roleExists) {
                  String adultRole = customConfig != null ? customConfig.getAdultNpcRoleId() : null;
                  if (adultRole == null) {
                     adultRole = modelAssetId;
                  }

                  roleExists = NPCPlugin.get().hasRoleName(adultRole);
                  if (roleExists) {
                     usedRoleName = adultRole;
                     this.logVerbose("Using adult NPC role with scaling: " + adultRole);
                  }
               }

               if (!roleExists || usedRoleName == null) {
                  this.logWarning("[CustomBreed] No valid NPC role found for: " + modelAssetId);
                  return;
               }

               Model scaledModel = null;
               float babyScale = 0.4F;
               if (!usingBabyRole) {
                  try {
                     DefaultAssetMap<String, ModelAsset> assetMap = ModelAsset.getAssetMap();
                     ModelAsset modelAsset = (ModelAsset)assetMap.getAsset(modelAssetId);
                     if (modelAsset != null) {
                        scaledModel = Model.createScaledModel(modelAsset, babyScale);
                        this.logVerbose("Created scaled model at " + babyScale + " for " + modelAssetId);
                     } else {
                        this.logWarning("[CustomBreed] ModelAsset not found for: " + modelAssetId);
                     }
                  } catch (Exception e) {
                     this.logVerbose("Could not create scaled model: " + e.getMessage());
                  }
               }

               Rotation3f rotation = new Rotation3f(0.0F, 0.0F, 0.0F);
               int roleIndex = NPCPlugin.get().getIndex(usedRoleName);
               Pair<Ref<EntityStore>, NPCEntity> result = NPCPlugin.get().spawnEntity(store, roleIndex, position, rotation, scaledModel, (TriConsumer)null, (TriConsumer)null);
               if (result == null) {
                  this.logWarning("[CustomBreed] Failed to spawn baby: " + usedRoleName);
                  return;
               }

               Ref<EntityStore> babyRef = (Ref)result.first();
               UUID babyId = EcsReflectionUtil.getUuidFromRef(babyRef);
               this.logVerbose("[CustomBreed] Spawned baby " + modelAssetId + " at " + String.format("(%.1f, %.1f, %.1f)", position.x(), position.y(), position.z()));
               this.breedingManager.registerBaby(babyId, (AnimalType)null, babyRef);
               if (this.hyTameTypeSupplier != null) {
                  ComponentType<EntityStore, HyTameComponent> hyTameType = (ComponentType)this.hyTameTypeSupplier.get();
                  if (hyTameType != null) {
                     HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(babyRef, hyTameType);
                     if (hyTameComp != null) {
                        hyTameComp.setGrowthStage(GrowthStage.BABY);
                        this.logVerbose("[CustomBreed] Set growthStage = BABY");
                     }
                  }
               }

               this.autoTameCustomBabyIfParentsTamed(store, babyRef, babyId, position, parent1Id, parent2Id, worldName);
            } catch (Exception e) {
               this.logWarning("[CustomBreed] Error spawning baby: " + e.getMessage());
            }

         });
      } catch (Exception e) {
         this.logWarning("[CustomBreed] Error in spawnCustomAnimalBaby: " + e.getMessage());
      }

   }

   private void autoTameCustomBabyIfParentsTamed(Store<EntityStore> store, Ref<EntityStore> entityRef, UUID babyId, Vector3d spawnPos, UUID parent1Id, UUID parent2Id, String worldName) {
      if (this.tamingManager != null && parent1Id != null && parent2Id != null) {
         TamedAnimalData parent1Data = this.tamingManager.getTamedData(parent1Id);
         TamedAnimalData parent2Data = this.tamingManager.getTamedData(parent2Id);
         String var10001 = String.valueOf(parent1Id);
         this.logVerbose("[CustomBreed] Parent1 UUID: " + var10001 + " -> " + (parent1Data != null ? "found" : "NOT FOUND"));
         var10001 = String.valueOf(parent2Id);
         this.logVerbose("[CustomBreed] Parent2 UUID: " + var10001 + " -> " + (parent2Data != null ? "found" : "NOT FOUND"));
         if (parent1Data != null && parent2Data != null) {
            UUID ownerUuid = parent1Data.getOwnerUuid();
            String ownerName = parent1Data.getOwnerName();
            if (ownerUuid == null) {
               ownerUuid = parent2Data.getOwnerUuid();
               ownerName = parent2Data.getOwnerName();
            }

            if (ownerUuid != null) {
               if (ownerName == null) {
                  ownerName = "Unknown";
               }

               String babyName = "_UNDEFINED";
               TamedAnimalData babyTameData = this.tamingManager.tameAnimal(babyId, ownerUuid, babyName, (AnimalType)null, entityRef, spawnPos.x(), spawnPos.y(), spawnPos.z(), GrowthStage.BABY, worldName);
               if (babyTameData != null) {
                  babyTameData.setOwnerName(ownerName);
                  if (this.hyTameTypeSupplier != null) {
                     ComponentType<EntityStore, HyTameComponent> hyTameType = (ComponentType)this.hyTameTypeSupplier.get();
                     if (hyTameType != null) {
                        HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(entityRef, hyTameType);
                        if (hyTameComp != null) {
                           hyTameComp.setTamed(ownerUuid, ownerName);
                           if (babyTameData.getHytameId() != null) {
                              hyTameComp.setHytameId(babyTameData.getHytameId());
                           }

                           this.logVerbose("[CustomBreed] Set HyTameComponent on baby: owner=" + ownerName);
                        }
                     }
                  }

                  var10001 = String.valueOf(babyId);
                  this.logVerbose("[CustomBreed] Auto-tamed baby (UUID: " + var10001 + ") to owner: " + ownerName);
               } else {
                  this.logVerbose("[CustomBreed] Failed to auto-tame baby - tameAnimal returned null");
               }

            }
         }
      }
   }

   public void updateEntityScale(UUID animalId, AnimalType animalType, float scale) {
      try {
         String var10001 = animalType.getId();
         this.logVerbose("Updating scale for " + var10001 + " to " + scale);
         BreedingData data = this.breedingManager.getData(animalId);
         if (data == null) {
            this.logWarning("Cannot update scale - no breeding data for animal");
            return;
         }

         Ref<EntityStore> entityRef = data.getEntityRef();
         if (entityRef == null || !entityRef.isValid()) {
            this.logVerbose("Cannot update scale - entity ref is " + (entityRef == null ? "null" : "stale"));
            return;
         }

         World world = null;
         String worldName = data.getWorldName();
         if (worldName != null) {
            world = Universe.get().getWorld(worldName);
         }

         if (world == null) {
            world = Universe.get().getDefaultWorld();
         }

         if (world == null) {
            this.logWarning("Cannot update scale - world is null");
            return;
         }

         world.execute(() -> {
            try {
               Store<EntityStore> store = world.getEntityStore().getStore();
               ComponentType<EntityStore, ModelComponent> modelType = EcsReflectionUtil.MODEL_TYPE;
               ModelComponent modelComp = null;

               try {
                  modelComp = (ModelComponent)store.getComponent(entityRef, modelType);
               } catch (Exception var14) {
                  Throwable cause = var14;
                  if (var14 instanceof InvocationTargetException) {
                     cause = ((InvocationTargetException)var14).getTargetException();
                  }

                  if (cause instanceof IllegalStateException && cause.getMessage() != null && cause.getMessage().contains("Invalid entity")) {
                     this.logVerbose("Entity ref is stale - removing tracking data");
                     this.breedingManager.removeData(animalId);
                     return;
                  }

                  throw var14;
               }

               if (modelComp == null) {
                  this.logVerbose("Entity has no ModelComponent - removing stale data");
                  this.breedingManager.removeData(animalId);
                  return;
               }

               Model currentModel = modelComp.getModel();
               if (currentModel == null) {
                  this.logWarning("Entity has no model - cannot scale");
                  return;
               }

               String modelAssetId = currentModel.getModelAssetId();
               ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset(modelAssetId);
               if (modelAsset == null) {
                  this.logWarning("ModelAsset not found: " + modelAssetId);
                  return;
               }

               Model newModel = Model.createScaledModel(modelAsset, scale);
               ModelComponent newModelComp = new ModelComponent(newModel);
               store.replaceComponent(entityRef, EcsReflectionUtil.MODEL_TYPE, newModelComp);
               this.logVerbose("Set model field to: " + newModel.toString());
               String var10001 = this.capitalize(animalType.getId());
               this.logVerbose(var10001 + " grew to scale " + String.format("%.1f", scale));
            } catch (Exception e) {
               Throwable cause = e;
               if (e instanceof InvocationTargetException) {
                  cause = ((InvocationTargetException)e).getTargetException();
                  if (cause == null) {
                     cause = e;
                  }
               }

               String errorMsg = cause.getMessage();
               if (errorMsg == null) {
                  errorMsg = cause.getClass().getSimpleName() + " (no message)";
               }

               this.logError("Error updating entity scale: " + errorMsg);
               cause.printStackTrace();
            }

         });
      } catch (Exception e) {
         this.logError("Error in updateEntityScale: " + e.getMessage());
      }

   }

   public void transformBabyToAdult(UUID animalId, AnimalType animalType) {
      try {
         String var10001 = animalType.getId();
         this.logWarning("Transforming baby " + var10001 + " to adult (animalId=" + String.valueOf(animalId) + ")");
         var10001 = animalType.getId();
         this.logVerbose("Transforming baby " + var10001 + " to adult (animalId=" + String.valueOf(animalId) + ")");
         BreedingData data = this.breedingManager.getData(animalId);
         if (data == null) {
            this.logVerbose("Cannot transform - no breeding data for animalId=" + String.valueOf(animalId));
            return;
         }

         String entityWorldName = this.getWorldNameFromRef(data.getEntityRef());
         if (entityWorldName != null && data.getWorldName() == null) {
            data.setWorldName(entityWorldName);
         }

         Ref<EntityStore> entityRef = data.getEntityRef();
         if (entityRef == null || !entityRef.isValid()) {
            Ref<EntityStore> safeEntityRef = this.tryReacquireBabyRef(animalId, animalType, data.getWorldName());
            if (safeEntityRef == null || !safeEntityRef.isValid()) {
               var10001 = animalType.getId();
               this.logVerbose("Cannot transform - reacquisition failed for " + var10001 + " in world=" + data.getWorldName());
               return;
            }

            data.setEntityRef(safeEntityRef);
            this.logVerbose("Re-acquired entityRef for baby " + animalType.getId());
         }

         if (data.getEntityRef() == null || !data.getEntityRef().isValid()) {
            return;
         }

         World finalWorld = Universe.get().getWorld(entityWorldName);
         String adultRoleId = animalType.usesVanillaTaming() ? animalType.getVanillaTamedRoleName() : animalType.getModelAssetId();
         Ref<EntityStore> finalEntityRef = data.getEntityRef();
         finalWorld.execute(() -> {
            try {
               Store<EntityStore> store = finalWorld.getEntityStore().getStore();
               TransformComponent transformComp = null;

               try {
                  transformComp = (TransformComponent)store.getComponent(finalEntityRef, EcsReflectionUtil.TRANSFORM_TYPE);
               } catch (ArrayIndexOutOfBoundsException var23) {
                  this.logVerbose("Baby entity ref is stale (ArrayIndexOutOfBounds) - removing tracking data");
                  this.breedingManager.removeData(animalId);
                  return;
               } catch (Exception var24) {
                  Throwable cause = var24;
                  if (var24 instanceof InvocationTargetException) {
                     cause = ((InvocationTargetException)var24).getTargetException();
                  }

                  if (cause instanceof IllegalStateException && cause.getMessage() != null && cause.getMessage().contains("Invalid entity")) {
                     this.logVerbose("Baby entity ref is stale - removing tracking data");
                     this.breedingManager.removeData(animalId);
                     return;
                  }

                  if (cause instanceof ArrayIndexOutOfBoundsException) {
                     this.logVerbose("Baby entity ref is stale (ArrayIndexOutOfBounds wrapped) - removing tracking data");
                     this.breedingManager.removeData(animalId);
                     return;
                  }

                  throw var24;
               }

               if (transformComp == null) {
                  this.logVerbose("Baby entity no longer exists - removing stale data");
                  this.breedingManager.removeData(animalId);
                  return;
               }

               Vector3d babyPosition = transformComp.getPosition();
               if (babyPosition == null) {
                  this.logVerbose("Baby entity no longer has valid position - removing stale data");
                  this.breedingManager.removeData(animalId);
                  return;
               }

               UUID babyUuid = this.getUuidFromRef(finalEntityRef);
               TamedAnimalData tamedData = null;
               if (babyUuid != null && this.tamingManager != null) {
                  tamedData = this.tamingManager.getTamedData(babyUuid);
                  if (tamedData != null) {
                     String var10001 = tamedData.getCustomName();
                     this.logVerbose("Baby is tamed - will transfer data to adult: " + var10001 + " (owner=" + tamedData.getOwnerName() + ")");
                  }
               }

               try {
                  RemoveReason despawnReason = null;

                  for(RemoveReason reason : RemoveReason.values()) {
                     String name = reason.name();
                     if (name.contains("DESPAWN") || name.contains("REMOVE") || name.contains("DELETE")) {
                        despawnReason = reason;
                        break;
                     }
                  }

                  if (despawnReason == null && RemoveReason.values().length > 0) {
                     despawnReason = RemoveReason.values()[0];
                  }

                  if (despawnReason != null) {
                     store.forEachChunk((chunk, commandBuffer) -> {
                        if (finalEntityRef.isValid()) {
                           commandBuffer.removeEntity(finalEntityRef, despawnReason);
                        }

                     });
                  }
               } catch (Exception var25) {
               }

               int roleIndex = NPCPlugin.get().getIndex(adultRoleId);
               if (roleIndex < 0) {
                  this.logWarning("Adult NPC role not found: " + adultRoleId);
                  return;
               }

               Rotation3f rotation = new Rotation3f(0.0F, 0.0F, 0.0F);
               Pair<Ref<EntityStore>, NPCEntity> result = NPCPlugin.get().spawnEntity(store, roleIndex, babyPosition, rotation, (Model)null, (TriConsumer)null);
               if (result != null && result.first() != null) {
                  Ref<EntityStore> adultRef = (Ref)result.first();
                  if (tamedData != null && babyUuid != null) {
                     UUID adultUuid = this.getUuidFromRef(adultRef);
                     if (adultUuid != null) {
                        UUID ownerUuid = tamedData.getOwnerUuid();
                        String ownerName = tamedData.getOwnerName();
                        UUID hytameId = tamedData.getHytameId();
                        String customName = tamedData.getCustomName();
                        if (ownerUuid != null && this.hyTameTypeSupplier != null) {
                           ComponentType<EntityStore, HyTameComponent> hyTameType = (ComponentType)this.hyTameTypeSupplier.get();
                           if (hyTameType != null) {
                              HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(adultRef, hyTameType);
                              if (hyTameComp != null) {
                                 String effectiveOwnerName = ownerName != null ? ownerName : "Unknown";
                                 hyTameComp.setTamed(ownerUuid, effectiveOwnerName);
                                 if (hytameId != null) {
                                    hyTameComp.setHytameId(hytameId);
                                 }

                                 this.logVerbose("Set HyTameComponent on adult: owner=" + effectiveOwnerName + ", hytameId=" + String.valueOf(hytameId));
                              }
                           }
                        }

                        this.tamingManager.markRespawned(babyUuid, adultUuid, adultRef);
                        TamedAnimalData updatedTamedData = this.tamingManager.getTamedData(adultUuid);
                        if (updatedTamedData != null) {
                           updatedTamedData.setGrowthStage(GrowthStage.ADULT);
                        }

                        if (customName != null && !customName.isEmpty() && !customName.equalsIgnoreCase("_UNDEFINED")) {
                           NameplateUtil.setEntityNameplate(adultRef, customName);
                        }

                        BreedingData adultBreedingData = this.breedingManager.getOrCreateData(adultUuid, animalType);
                        adultBreedingData.setTamed(true, ownerUuid);
                        adultBreedingData.setCustomName(customName);
                        adultBreedingData.setEntityRef(adultRef);
                        adultBreedingData.setGrowthStage(GrowthStage.ADULT);
                        this.logVerbose("Transferred taming data from baby to adult: " + customName + " (owner=" + ownerName + ", hytameId=" + String.valueOf(hytameId) + ")");
                     }
                  }

                  String var40 = this.capitalize(animalType.getId());
                  this.logVerbose(var40 + " grew into an adult at " + String.format("%.0f, %.0f, %.0f", babyPosition.x(), babyPosition.y(), babyPosition.z()));
               } else {
                  this.logWarning("Failed to spawn adult " + animalType.getId());
               }

               this.breedingManager.removeData(animalId);
            } catch (ArrayIndexOutOfBoundsException var26) {
               this.logVerbose("Entity ref became stale during transformation - cleaning up");
               this.breedingManager.removeData(animalId);
            } catch (Exception e) {
               Throwable cause = e;
               if (e instanceof InvocationTargetException) {
                  cause = ((InvocationTargetException)e).getTargetException();
                  if (cause == null) {
                     cause = e;
                  }
               }

               if (cause instanceof ArrayIndexOutOfBoundsException) {
                  this.logVerbose("Entity ref became stale during transformation (wrapped) - cleaning up");
                  this.breedingManager.removeData(animalId);
                  return;
               }

               String errorMsg = cause.getMessage();
               if (errorMsg == null) {
                  errorMsg = cause.getClass().getSimpleName() + " (no message)";
               }

               this.logError("Error transforming to adult: " + errorMsg);
               cause.printStackTrace();
               this.breedingManager.removeData(animalId);
            }

         });
      } catch (Exception e) {
         this.logError("Error in transformBabyToAdult: " + e.getMessage());
      }

   }

   private String getWorldNameFromRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         try {
            UUID entityUuid = this.getUuidFromRef(ref);
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

   private UUID getUuidFromRef(Ref<EntityStore> ref) {
      return ref != null ? EcsReflectionUtil.getUuidFromRef(ref) : null;
   }

   private Ref<EntityStore> tryReacquireBabyRef(UUID animalId, AnimalType animalType, String worldName) {
      try {
         World world = null;
         if (worldName != null) {
            world = Universe.get().getWorld(worldName);
         }

         if (world == null) {
            world = Universe.get().getDefaultWorld();
         }

         if (world == null) {
            return null;
         } else {
            String babyModelId = animalType.getBabyModelAssetId();
            if (babyModelId == null) {
               return null;
            } else {
               Store<EntityStore> store = world.getEntityStore().getStore();
               Method getAllRefs = null;

               for(Method m : store.getClass().getMethods()) {
                  if (m.getName().equals("getAllRefs") && m.getParameterCount() == 0) {
                     getAllRefs = m;
                     break;
                  }
               }

               if (getAllRefs == null) {
                  return null;
               } else {
                  for(Ref<EntityStore> ref : (Iterable)getAllRefs.invoke(store)) {
                     try {
                        String modelAssetId = null;
                        if (this.modelAssetIdGetter != null) {
                           modelAssetId = (String)this.modelAssetIdGetter.apply(new Object[]{store, ref});
                        }

                        if (modelAssetId != null && modelAssetId.equalsIgnoreCase(babyModelId)) {
                           UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
                           if (uuidComponent != null) {
                              UUID candidateId = uuidComponent.getUuid();
                              if (candidateId.equals(animalId)) {
                                 this.logVerbose("tryReacquireBabyRef: Found matching baby by UUID");
                                 return ref;
                              }

                              BreedingData foundData = this.breedingManager.findBabyByRef(ref);
                              if (foundData != null && foundData.getAnimalId().equals(animalId)) {
                                 this.logVerbose("tryReacquireBabyRef: Found matching baby by ref comparison");
                                 return ref;
                              }
                           }
                        }
                     } catch (Exception var15) {
                        this.logVerbose("Error while reacquiring ref");
                     }
                  }

                  this.logVerbose("tryReacquireBabyRef: No matching baby found for " + animalType.getId());
                  return null;
               }
            }
         }
      } catch (Exception e) {
         this.logVerbose("tryReacquireBabyRef error: " + e.getMessage());
         return null;
      }
   }

   public void performInstantBreeding(BreedingData animal1, BreedingData animal2, AnimalType type, Vector3d spawnPos) {
      animal1.completeBreeding();
      animal2.completeBreeding();
      this.spawnBabyAnimal(type, spawnPos, animal1.getAnimalId(), animal2.getAnimalId());
   }

   public Vector3d getPositionFromBreedingData(BreedingData data) {
      Object entityRef = data.getEntityRef();
      if (entityRef != null && entityRef instanceof Ref) {
         try {
            World world = null;
            String worldName = data.getWorldName();
            if (worldName != null) {
               world = Universe.get().getWorld(worldName);
            }

            if (world == null) {
               world = Universe.get().getDefaultWorld();
            }

            if (world == null) {
               return null;
            }

            Store<EntityStore> store = world.getEntityStore().getStore();
            TransformComponent transform = (TransformComponent)store.getComponent((Ref)entityRef, EcsReflectionUtil.TRANSFORM_TYPE);
            if (transform != null) {
               return transform.getPosition();
            }
         } catch (Exception var7) {
         }

         return null;
      } else {
         return null;
      }
   }

   public double calculateDistance(Vector3d pos1, Vector3d pos2) {
      double dx = pos2.x() - pos1.x();
      double dy = pos2.y() - pos1.y();
      double dz = pos2.z() - pos1.z();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private String capitalize(String str) {
      if (str != null && !str.isEmpty()) {
         String var10000 = str.substring(0, 1).toUpperCase();
         return var10000 + str.substring(1).toLowerCase();
      } else {
         return str;
      }
   }
}
