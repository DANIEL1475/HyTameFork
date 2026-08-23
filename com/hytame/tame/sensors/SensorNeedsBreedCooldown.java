package com.hytame.tame.sensors;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hytame.tame.HyTameComponent;
import javax.annotation.Nonnull;

public class SensorNeedsBreedCooldown extends SensorBase {
   public SensorNeedsBreedCooldown(@Nonnull BuilderSensorNeedsBreedCooldown builder, @Nonnull BuilderSupport support) {
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
               return super.matches(ref, role, dt, store) && hyTameComponent.isNeedsBreedCooldown();
            }
         }
      } catch (Exception var8) {
         return false;
      }
   }

   public InfoProvider getSensorInfo() {
      return null;
   }
}
