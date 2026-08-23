package com.hytame.tame.actions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.tame.HyTameComponent;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.NameplateUtil;
import it.unimi.dsi.fastutil.Pair;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class ActionGrowToNextStage extends ActionBase {
   public ActionGrowToNextStage(@Nonnull BuilderActionGrowToNextStage builder, @Nonnull BuilderSupport support) {
      super(builder);
   }

   public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
      HyTameComponent hyTame = (HyTameComponent)store.getComponent(ref, HyTameComponent.getComponentType());
      return hyTame == null ? false : hyTame.canGrow();
   }

   public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
      super.execute(ref, role, sensorInfo, dt, store);
      this.log("ActionGrowToNextStage: execute called");
      HyTameComponent hyTame = (HyTameComponent)store.getComponent(ref, HyTameComponent.getComponentType());
      if (hyTame == null) {
         this.log("ActionGrowToNextStage: HyTameComponent not found");
         return false;
      } else {
         GrowthStage currentStage = hyTame.getGrowthStage();
         if (currentStage == GrowthStage.ADULT) {
            this.log("ActionGrowToNextStage: Already adult, nothing to do");
            return false;
         } else {
            ModelComponent modelComp = (ModelComponent)store.getComponent(ref, EcsReflectionUtil.MODEL_TYPE);
            if (modelComp != null && modelComp.getModel() != null) {
               String modelAssetId = modelComp.getModel().getModelAssetId();
               AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
               if (animalType == null) {
                  this.log("ActionGrowToNextStage: Unknown animal type: " + modelAssetId);
                  return false;
               } else {
                  return animalType.hasBabyVariant() ? this.executeTransformToAdult(ref, role, store, hyTame, animalType) : this.executeScaleGrowth(ref, role, store, hyTame, animalType, modelComp);
               }
            } else {
               this.log("ActionGrowToNextStage: No model component");
               return false;
            }
         }
      }
   }

   private boolean executeTransformToAdult(Ref<EntityStore> ref, Role role, Store<EntityStore> store, HyTameComponent hyTame, AnimalType animalType) {
      this.log("ActionGrowToNextStage: Transforming baby NPC to adult: " + animalType.getId());
      TransformComponent transform = (TransformComponent)store.getComponent(ref, EcsReflectionUtil.TRANSFORM_TYPE);
      if (transform == null) {
         this.log("ActionGrowToNextStage: No transform component");
         return false;
      } else {
         Vector3d position = transform.getPosition();
         if (position == null) {
            this.log("ActionGrowToNextStage: No position");
            return false;
         } else {
            boolean wasTamed = hyTame.isTamed();
            UUID tamerUUID = hyTame.getTamerUUID();
            String tamerName = hyTame.getTamerName();
            UUID hytameId = hyTame.getHytameId();
            String adultRoleId = animalType.getAdultNpcRoleId();
            int roleIndex = NPCPlugin.get().getIndex(adultRoleId);
            if (roleIndex < 0) {
               this.log("ActionGrowToNextStage: Adult role not found: " + adultRoleId);
               return false;
            } else {
               Rotation3f rotation = new Rotation3f(0.0F, 0.0F, 0.0F);
               Pair<Ref<EntityStore>, NPCEntity> result = NPCPlugin.get().spawnEntity(store, roleIndex, position, rotation, (Model)null, (TriConsumer)null);
               if (result != null && result.first() != null) {
                  Ref<EntityStore> adultRef = (Ref)result.first();

                  try {
                     RemoveReason removeReason = this.findRemoveReason();
                     World world = ((EntityStore)store.getExternalData()).getWorld();
                     world.execute(() -> {
                        try {
                           if (wasTamed && adultRef.isValid()) {
                              HyTameComponent adultTame = (HyTameComponent)store.ensureAndGetComponent(adultRef, HyTameComponent.getComponentType());
                              if (adultTame != null && tamerUUID != null && tamerName != null) {
                                 adultTame.setTamed(tamerUUID, tamerName);
                                 if (hytameId != null) {
                                    adultTame.setHytameId(hytameId);
                                 }

                                 adultTame.setGrowthStage(GrowthStage.ADULT);
                                 this.log("ActionGrowToNextStage: Transferred tame data to adult");
                                 HyTamePlugin plugin = HyTamePlugin.getInstance();
                                 if (plugin != null && plugin.getTamingManager() != null && hytameId != null) {
                                    UUID newAdultUuid = EcsReflectionUtil.getUuidFromRef(adultRef);
                                    if (newAdultUuid != null) {
                                       plugin.getTamingManager().updateEntityAfterGrowth(hytameId, newAdultUuid, adultRef);
                                       TamedAnimalData tamedData = plugin.getTamingManager().getTamedData(newAdultUuid);
                                       if (tamedData != null) {
                                          String customName = tamedData.getCustomName();
                                          if (customName != null && !customName.isEmpty() && !customName.equalsIgnoreCase("_UNDEFINED")) {
                                             NameplateUtil.setEntityNameplate(adultRef, customName);
                                             this.log("ActionGrowToNextStage: Restored nameplate: " + customName);
                                          }
                                       }
                                    } else {
                                       plugin.getTamingManager().updateEntityRef(hytameId, adultRef, true);
                                    }
                                 }
                              }
                           }

                           if (removeReason != null && ref.isValid()) {
                              store.removeEntity(ref, removeReason);
                           }
                        } catch (Exception ex) {
                           this.log("ActionGrowToNextStage: Deferred operations failed: " + ex.getMessage());
                        }

                     });
                  } catch (Exception e) {
                     this.log("ActionGrowToNextStage: Error scheduling deferred operations: " + e.getMessage());
                  }

                  String var10001 = animalType.getId();
                  this.log("ActionGrowToNextStage: " + var10001 + " grew into an adult at " + String.format("%.0f, %.0f, %.0f", position.x(), position.y(), position.z()));
                  return true;
               } else {
                  this.log("ActionGrowToNextStage: Failed to spawn adult");
                  return false;
               }
            }
         }
      }
   }

   private boolean executeScaleGrowth(Ref<EntityStore> ref, Role role, Store<EntityStore> store, HyTameComponent hyTame, AnimalType animalType, ModelComponent modelComp) {
      GrowthStage currentStage = hyTame.getGrowthStage();
      GrowthStage nextStage = currentStage.getNextStage();
      String var10001 = animalType.getId();
      this.log("ActionGrowToNextStage: Scaling " + var10001 + " from " + String.valueOf(currentStage) + " to " + String.valueOf(nextStage));
      float targetScale = animalType.getScaleForStage(nextStage);
      Model currentModel = modelComp.getModel();
      String modelAssetId = currentModel.getModelAssetId();
      ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset(modelAssetId);
      if (modelAsset == null) {
         this.log("ActionGrowToNextStage: ModelAsset not found: " + modelAssetId);
         return false;
      } else {
         try {
            Model newModel = Model.createScaledModel(modelAsset, targetScale);
            ModelComponent newModelComp = new ModelComponent(newModel);
            store.replaceComponent(ref, EcsReflectionUtil.MODEL_TYPE, newModelComp);
            hyTame.setGrowthStage(nextStage);
            var10001 = animalType.getId();
            this.log("ActionGrowToNextStage: " + var10001 + " grew to " + String.valueOf(nextStage) + " (scale " + String.format("%.1f", targetScale) + ")");
            if (nextStage == GrowthStage.ADULT) {
               HyTamePlugin plugin = HyTamePlugin.getInstance();
               if (plugin != null && plugin.getTamingManager() != null && hyTame.getHytameId() != null) {
                  plugin.getTamingManager().updateGrowthStage(hyTame.getHytameId(), GrowthStage.ADULT);
               }
            }

            return true;
         } catch (Exception e) {
            this.log("ActionGrowToNextStage: Error scaling model: " + e.getMessage());
            return false;
         }
      }
   }

   private RemoveReason findRemoveReason() {
      for(RemoveReason reason : RemoveReason.values()) {
         String name = reason.name();
         if (name.contains("DESPAWN") || name.contains("REMOVE") || name.contains("DELETE")) {
            return reason;
         }
      }

      if (RemoveReason.values().length > 0) {
         return RemoveReason.values()[0];
      } else {
         return null;
      }
   }

   private void log(String msg) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[Growth] " + msg);
         }

      }
   }
}
