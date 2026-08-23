package com.hytame.listeners;

import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.util.EcsReflectionUtil;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

public class NewAnimalSpawnDetector extends RefSystem<EntityStore> {
   private static final ComponentType<EntityStore, ModelComponent> MODEL_TYPE;
   private static final ComponentType<EntityStore, UUIDComponent> UUID_TYPE;
   private final Map<String, Long> processedEntities = new ConcurrentHashMap();
   private static final long CACHE_TTL_MS = 300000L;
   private static final int MAX_CACHE_SIZE = 10000;
   private volatile Set<UUID> playerUuids = ConcurrentHashMap.newKeySet();
   private static int detectedCount;
   private static long lastDetectionTime;
   private static String lastDetectedAnimal;

   public NewAnimalSpawnDetector() {
      this.log("NewAnimalSpawnDetector initialized (RefSystem pattern)");
   }

   public static int getDetectedCount() {
      return detectedCount;
   }

   public static long getLastDetectionTime() {
      return lastDetectionTime;
   }

   public static String getLastDetectedAnimal() {
      return lastDetectedAnimal;
   }

   public void updatePlayerUuids(Set<UUID> uuids) {
      this.playerUuids = (Set<UUID>)(uuids != null ? uuids : ConcurrentHashMap.newKeySet());
   }

   public Query<EntityStore> getQuery() {
      return MODEL_TYPE;
   }

   public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
      try {
         if (ref == null || !ref.isValid()) {
            return;
         }

         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUID_TYPE);
         String dedupeKey = uuidComp != null && uuidComp.getUuid() != null ? uuidComp.getUuid().toString() : ref.toString();
         if (this.processedEntities.containsKey(dedupeKey)) {
            return;
         }

         if (uuidComp != null && uuidComp.getUuid() != null && this.playerUuids.contains(uuidComp.getUuid())) {
            return;
         }

         ModelComponent modelComp = (ModelComponent)store.getComponent(ref, MODEL_TYPE);
         if (modelComp == null) {
            return;
         }

         String modelAssetId = this.extractModelAssetId(modelComp);
         if (modelAssetId == null) {
            return;
         }

         AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
         boolean isCustomAnimal = false;
         if (animalType == null) {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
               isCustomAnimal = plugin.getConfigManager().isCustomAnimal(modelAssetId);
            }
         }

         if (animalType == null && !isCustomAnimal) {
            return;
         }

         this.processedEntities.put(dedupeKey, System.currentTimeMillis());
         if (this.processedEntities.size() > 10000) {
            this.cleanupExpiredEntries();
         }

         ++detectedCount;
         lastDetectionTime = System.currentTimeMillis();
         lastDetectedAnimal = modelAssetId;
         String typeDesc = animalType != null ? animalType.toString() : "CUSTOM";
         this.log("Detected new animal (RefSystem): " + modelAssetId + " (" + typeDesc + ")");
         World world = ((EntityStore)commandBuffer.getExternalData()).getWorld();
         world.execute(() -> {
            try {
               if (!ref.isValid()) {
                  return;
               }

               Store<EntityStore> s = ref.getStore();
               if (s == null) {
                  return;
               }

               CoopResidentComponent coopComp = (CoopResidentComponent)s.getComponent(ref, CoopResidentComponent.getComponentType());
               if (coopComp != null) {
                  Vector3i coopPos = coopComp.getCoopLocation();
                  if (coopPos != null) {
                     CoopResidentTracker.restoreFromCoop(ref, coopPos, world);
                  }
               }
            } catch (Exception var5) {
            }

         });
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            plugin.onNewAnimalDetected(store, ref, modelAssetId, animalType, world);
         }
      } catch (Exception var15) {
      }

   }

   public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
      try {
         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUID_TYPE);
         if (uuidComp != null && uuidComp.getUuid() != null) {
            this.processedEntities.remove(uuidComp.getUuid().toString());
         } else {
            this.processedEntities.remove(ref.toString());
         }
      } catch (Exception var6) {
      }

   }

   private String extractModelAssetId(ModelComponent modelComp) {
      try {
         Model model = modelComp.getModel();
         return model == null ? null : model.getModelAssetId();
      } catch (Exception var3) {
         return null;
      }
   }

   public void clearProcessedCache() {
      this.processedEntities.clear();
   }

   public int cleanupExpiredEntries() {
      long now = System.currentTimeMillis();
      int removed = 0;
      Iterator<Map.Entry<String, Long>> it = this.processedEntities.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry<String, Long> entry = (Map.Entry)it.next();
         if (now - (Long)entry.getValue() > 300000L) {
            it.remove();
            ++removed;
         }
      }

      if (removed > 0) {
         this.log("Cleaned up " + removed + " expired entries from spawn detector cache");
      }

      return removed;
   }

   public int getProcessedCacheSize() {
      return this.processedEntities.size();
   }

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         System.out.println("[NewAnimalSpawnDetector] " + message);
      }

   }

   static {
      MODEL_TYPE = EcsReflectionUtil.MODEL_TYPE;
      UUID_TYPE = EcsReflectionUtil.UUID_TYPE;
      detectedCount = 0;
      lastDetectionTime = 0L;
      lastDetectedAnimal = "none";
   }
}
