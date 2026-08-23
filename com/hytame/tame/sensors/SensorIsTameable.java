package com.hytame.tame.sensors;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import javax.annotation.Nonnull;

public class SensorIsTameable extends SensorBase {
   protected final boolean value;

   public SensorIsTameable(@Nonnull BuilderSensorIsTameable builder, @Nonnull BuilderSupport support) {
      super(builder);
      this.value = builder.getValue(support);
   }

   public boolean matches(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, double dt, @Nonnull Store<EntityStore> store) {
      ModelComponent modelComponent = (ModelComponent)store.getComponent(ref, EcsReflectionUtil.MODEL_TYPE);
      String modelAssetId = modelComponent.getModel().getModelAssetId();
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      ConfigManager configManager = plugin.getConfigManager();
      CustomAnimalConfig customAnimal = null;
      AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
      if (configManager.isDebugMode()) {
         boolean breedingEnabled = animalType != null && configManager.isBreedingEnabled(animalType);
         boolean tamingEnabled = animalType != null && configManager.isTamingEnabled(animalType);
         ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[IsTameable] modelAssetId=%s, animalType=%s, breedingEnabled=%s, tamingEnabled=%s", modelAssetId, animalType, breedingEnabled, tamingEnabled);
      }

      if (animalType == null || !configManager.isBreedingEnabled(animalType) && !configManager.isTamingEnabled(animalType)) {
         if (animalType != null) {
            return false;
         } else {
            customAnimal = configManager.getCustomAnimal(modelAssetId);
            return customAnimal == null || !configManager.isCustomAnimalBreedingEnabled(customAnimal.getModelAssetId()) && !configManager.isCustomAnimalTamingEnabled(customAnimal.getModelAssetId()) ? false : super.matches(ref, role, dt, store);
         }
      } else {
         return super.matches(ref, role, dt, store);
      }
   }

   public InfoProvider getSensorInfo() {
      return null;
   }
}
