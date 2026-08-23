package com.hytame.managers;

import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.util.ConfigManager;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GrowthManager {
   private final ConfigManager config;
   private final BreedingManager breedingManager;
   private Consumer<GrowthEvent> onGrowthCallback;
   private Consumer<String> debugLogger;

   public GrowthManager(ConfigManager config, BreedingManager breedingManager) {
      this.config = config;
      this.breedingManager = breedingManager;
   }

   public void tickGrowth() {
      if (this.config.isGrowthEnabled()) {
         for(UUID animalId : this.getTrackedAnimalIds()) {
            BreedingData data = this.breedingManager.getData(animalId);
            if (data != null && data.getGrowthStage().hasNextStage()) {
               this.checkAndUpdateGrowth(data);
            }
         }

      }
   }

   private void checkAndUpdateGrowth(BreedingData data) {
      GrowthStage currentStage = data.getGrowthStage();
      if (currentStage.hasNextStage()) {
         long age = data.getAge();
         long timeToNextStage = this.getTimeToReachNextStage(data.getAnimalType(), currentStage);
         if (age >= timeToNextStage) {
            GrowthStage nextStage = currentStage.getNextStage();
            data.setGrowthStage(nextStage);
            String var10001 = String.valueOf(data.getAnimalId());
            this.debug("Animal " + var10001 + " grew from " + String.valueOf(currentStage) + " to " + String.valueOf(nextStage));
            if (this.onGrowthCallback != null) {
               GrowthEvent event = new GrowthEvent(data.getAnimalId(), data.getAnimalType(), currentStage, nextStage);
               this.onGrowthCallback.accept(event);
            }
         }

      }
   }

   private long getTimeToReachNextStage(AnimalType animalType, GrowthStage currentStage) {
      long totalTime = 0L;

      for(GrowthStage stage : GrowthStage.values()) {
         totalTime += this.config.getGrowthStageDuration(animalType, stage);
         if (stage == currentStage) {
            break;
         }
      }

      return totalTime;
   }

   public float getGrowthProgress(UUID animalId) {
      BreedingData data = this.breedingManager.getData(animalId);
      if (data == null) {
         return 1.0F;
      } else if (data.getGrowthStage() == GrowthStage.ADULT) {
         return 1.0F;
      } else {
         long age = data.getAge();
         long totalTimeToAdult = this.config.getGrowthTime(data.getAnimalType());
         return totalTimeToAdult == 0L ? 1.0F : Math.min(1.0F, (float)age / (float)totalTimeToAdult);
      }
   }

   public float getSizeMultiplier(UUID animalId) {
      BreedingData data = this.breedingManager.getData(animalId);
      return data == null ? 1.0F : data.getGrowthStage().getSizeMultiplier();
   }

   public boolean isFullyGrown(UUID animalId) {
      BreedingData data = this.breedingManager.getData(animalId);
      return data == null || data.getGrowthStage() == GrowthStage.ADULT;
   }

   public boolean forceGrowth(UUID animalId) {
      BreedingData data = this.breedingManager.getData(animalId);
      if (data == null) {
         return false;
      } else {
         GrowthStage currentStage = data.getGrowthStage();
         if (!currentStage.hasNextStage()) {
            return false;
         } else {
            GrowthStage nextStage = currentStage.getNextStage();
            data.setGrowthStage(nextStage);
            String var10001 = String.valueOf(animalId);
            this.debug("Force growth: " + var10001 + " grew from " + String.valueOf(currentStage) + " to " + String.valueOf(nextStage));
            if (this.onGrowthCallback != null) {
               GrowthEvent event = new GrowthEvent(animalId, data.getAnimalType(), currentStage, nextStage);
               this.onGrowthCallback.accept(event);
            }

            return true;
         }
      }
   }

   public long getTimeToAdult(UUID animalId) {
      BreedingData data = this.breedingManager.getData(animalId);
      if (data != null && data.getGrowthStage() != GrowthStage.ADULT) {
         long age = data.getAge();
         long totalTimeToAdult = this.config.getGrowthTime(data.getAnimalType());
         return Math.max(0L, totalTimeToAdult - age);
      } else {
         return 0L;
      }
   }

   private Iterable<UUID> getTrackedAnimalIds() {
      return this.breedingManager.getTrackedAnimalIds();
   }

   public void setOnGrowthCallback(Consumer<GrowthEvent> callback) {
      this.onGrowthCallback = callback;
   }

   public void setDebugLogger(Consumer<String> logger) {
      this.debugLogger = logger;
   }

   private void debug(String message) {
      if (this.config.isDebugMode() && this.debugLogger != null) {
         this.debugLogger.accept("[Growth] " + message);
      }

   }

   public Map<GrowthStage, Integer> getGrowthStageCounts() {
      Map<GrowthStage, Integer> counts = new EnumMap(GrowthStage.class);

      for(GrowthStage stage : GrowthStage.values()) {
         counts.put(stage, 0);
      }

      for(UUID animalId : this.getTrackedAnimalIds()) {
         BreedingData data = this.breedingManager.getData(animalId);
         if (data != null) {
            GrowthStage stage = data.getGrowthStage();
            counts.put(stage, (Integer)counts.get(stage) + 1);
         }
      }

      return counts;
   }

   public static class GrowthEvent {
      private final UUID animalId;
      private final AnimalType animalType;
      private final GrowthStage previousStage;
      private final GrowthStage newStage;

      public GrowthEvent(UUID animalId, AnimalType animalType, GrowthStage previousStage, GrowthStage newStage) {
         this.animalId = animalId;
         this.animalType = animalType;
         this.previousStage = previousStage;
         this.newStage = newStage;
      }

      public UUID getAnimalId() {
         return this.animalId;
      }

      public AnimalType getAnimalType() {
         return this.animalType;
      }

      public GrowthStage getPreviousStage() {
         return this.previousStage;
      }

      public GrowthStage getNewStage() {
         return this.newStage;
      }

      public boolean usesScaling() {
         return !this.animalType.hasBabyVariant();
      }

      public float getTargetScale() {
         return this.animalType.getScaleForStage(this.newStage);
      }
   }
}
