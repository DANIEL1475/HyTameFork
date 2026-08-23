package com.hytame.tame.sensors;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hytame.models.AnimalType;
import com.hytame.models.GrowthStage;
import com.hytame.tame.HyTameComponent;
import com.hytame.util.EcsReflectionUtil;
import javax.annotation.Nonnull;

public class SensorScaledBaby extends SensorBase {
   public SensorScaledBaby(@Nonnull BuilderSensorScaledBaby builder, @Nonnull BuilderSupport support) {
      super(builder);
   }

   public boolean matches(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, double dt, @Nonnull Store<EntityStore> store) {
      try {
         ComponentType<EntityStore, HyTameComponent> componentType = HyTameComponent.getComponentType();
         if (componentType == null) {
            return false;
         } else {
            HyTameComponent hyTameComponent = (HyTameComponent)store.getComponent(ref, componentType);
            if (hyTameComponent == null) {
               return false;
            } else {
               GrowthStage currentStage = hyTameComponent.getGrowthStage();
               if (currentStage == GrowthStage.ADULT) {
                  return false;
               } else {
                  ModelComponent modelComp = (ModelComponent)store.getComponent(ref, EcsReflectionUtil.MODEL_TYPE);
                  if (modelComp == null) {
                     return false;
                  } else {
                     Model model = modelComp.getModel();
                     if (model == null) {
                        return false;
                     } else {
                        String modelAssetId = model.getModelAssetId();
                        if (modelAssetId == null) {
                           return false;
                        } else {
                           AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
                           if (animalType == null) {
                              return false;
                           } else {
                              boolean usesScaling = !animalType.hasBabyVariant();
                              return super.matches(ref, role, dt, store) && usesScaling;
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var14) {
         return false;
      }
   }

   public InfoProvider getSensorInfo() {
      return null;
   }
}
