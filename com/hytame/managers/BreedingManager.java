package com.hytame.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.util.ConfigManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BreedingManager {
   private final ConfigManager config;
   private final Map<UUID, BreedingData> breedingDataMap = new ConcurrentHashMap();
   private final Map<UUID, CustomAnimalLoveData> customAnimalsInLove = new ConcurrentHashMap();
   private Consumer<BirthEvent> onBirthCallback;
   private Consumer<CustomBirthEvent> onCustomBirthCallback;
   private Consumer<String> debugLogger;

   public BreedingManager(ConfigManager config) {
      this.config = config;
   }

   public BreedingData getOrCreateData(UUID animalId, AnimalType animalType) {
      return (BreedingData)this.breedingDataMap.computeIfAbsent(animalId, (id) -> new BreedingData(id, animalType));
   }

   public BreedingData getData(UUID animalId) {
      return (BreedingData)this.breedingDataMap.get(animalId);
   }

   public BreedingData findBabyByRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         for(BreedingData data : this.breedingDataMap.values()) {
            if (data.getGrowthStage() != GrowthStage.ADULT) {
               Ref<EntityStore> storedRef = data.getEntityRef();
               if (storedRef != null && this.refsMatch(ref, storedRef)) {
                  this.debug("Found baby by ref match: " + String.valueOf(data.getAnimalId()));
                  return data;
               }
            }
         }

         return null;
      }
   }

   private boolean refsMatch(Ref<EntityStore> ref1, Ref<EntityStore> ref2) {
      if (!(ref2 instanceof Ref)) {
         return false;
      } else {
         try {
            Integer index1 = ref1.getIndex();
            Integer index2 = ref2.getIndex();
            return index1 != null && index1.equals(index2);
         } catch (Exception var6) {
            return false;
         }
      }
   }

   public void removeData(UUID animalId) {
      this.breedingDataMap.remove(animalId);
   }

   public void clearAll() {
      this.breedingDataMap.clear();
      this.customAnimalsInLove.clear();
   }

   public String getRegisteredBabiesDebug() {
      StringBuilder sb = new StringBuilder();
      sb.append("Registered babies: ");
      int count = 0;

      for(Map.Entry<UUID, BreedingData> entry : this.breedingDataMap.entrySet()) {
         if (((BreedingData)entry.getValue()).getGrowthStage() != GrowthStage.ADULT) {
            if (count > 0) {
               sb.append(", ");
            }

            sb.append(((UUID)entry.getKey()).toString().substring(0, 8)).append("...(").append(((BreedingData)entry.getValue()).getGrowthStage()).append(")");
            ++count;
         }
      }

      if (count == 0) {
         sb.append("none");
      }

      return sb.toString();
   }

   public int cleanupStaleEntries() {
      int removed = 0;
      Iterator<Map.Entry<UUID, BreedingData>> it = this.breedingDataMap.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry<UUID, BreedingData> entry = (Map.Entry)it.next();
         BreedingData data = (BreedingData)entry.getValue();
         Ref<EntityStore> entityRef = data.getEntityRef();
         if (entityRef != null) {
            try {
               Store<EntityStore> store = entityRef.getStore();
               if (store == null) {
                  it.remove();
                  ++removed;
                  this.debug("Removed stale breeding entry: " + String.valueOf(entry.getKey()));
               }
            } catch (Exception var7) {
               it.remove();
               ++removed;
               this.debug("Removed stale breeding entry (exception): " + String.valueOf(entry.getKey()));
            }
         }
      }

      removed += this.cleanupStaleCustomAnimals();
      return removed;
   }

   private int cleanupStaleCustomAnimals() {
      int removed = 0;
      Iterator<Map.Entry<UUID, CustomAnimalLoveData>> it = this.customAnimalsInLove.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry<UUID, CustomAnimalLoveData> entry = (Map.Entry)it.next();
         CustomAnimalLoveData data = (CustomAnimalLoveData)entry.getValue();
         Ref<EntityStore> entityRef = data.getEntityRef();
         if (entityRef != null) {
            try {
               Store<EntityStore> store = entityRef.getStore();
               if (store == null) {
                  it.remove();
                  ++removed;
                  this.debug("Removed stale custom animal entry: " + String.valueOf(entry.getKey()));
               }
            } catch (Exception var7) {
               it.remove();
               ++removed;
               this.debug("Removed stale custom animal entry (exception): " + String.valueOf(entry.getKey()));
            }
         }
      }

      return removed;
   }

   public BreedingData registerBaby(UUID babyId, AnimalType animalType, Ref<EntityStore> entityRef) {
      BreedingData babyData = BreedingData.createBaby(babyId, animalType);
      babyData.setEntityRef(entityRef);
      this.breedingDataMap.put(babyId, babyData);
      String var10001 = String.valueOf(babyId);
      this.debug("Registered baby " + var10001 + " (" + String.valueOf(animalType) + ") for growth tracking");
      return babyData;
   }

   public boolean canBreed(UUID animalId, AnimalType animalType) {
      if (HyTamePlugin.isAlarmBasedBreedCooldown()) {
         return true;
      } else {
         BreedingData data = this.getOrCreateData(animalId, animalType);
         long cooldown = this.config.getBreedingCooldown(animalType);
         return data.canBreed(cooldown);
      }
   }

   public FeedResult tryFeed(UUID animalId, AnimalType animalType, String foodItemId) {
      return this.tryFeed(animalId, animalType, foodItemId, (Ref)null);
   }

   public FeedResult tryFeed(UUID animalId, AnimalType animalType, String foodItemId, Ref<EntityStore> entityRef) {
      return this.tryFeed(animalId, animalType, foodItemId, entityRef, (String)null);
   }

   public FeedResult tryFeed(UUID animalId, AnimalType animalType, String foodItemId, Ref<EntityStore> entityRef, String worldName) {
      if (!this.config.isBreedingEnabled(animalType)) {
         return BreedingManager.FeedResult.DISABLED;
      } else if (!this.config.isBreedingFood(animalType, foodItemId)) {
         return BreedingManager.FeedResult.WRONG_FOOD;
      } else {
         BreedingData data = this.getOrCreateData(animalId, animalType);
         if (entityRef != null) {
            data.setEntityRef(entityRef);
            String var10001 = String.valueOf(animalId);
            this.debug("Stored entityRef for " + var10001 + " (" + String.valueOf(animalType) + "): " + String.valueOf(entityRef));
         } else {
            String var9 = String.valueOf(animalId);
            this.debug("WARNING: entityRef is null for " + var9 + " (" + String.valueOf(animalType) + ")");
         }

         if (worldName != null) {
            data.setWorldName(worldName);
            String var10 = String.valueOf(animalId);
            this.debug("[MultiWorld] Stored worldName for " + var10 + ": " + worldName);
         } else {
            this.debug("[MultiWorld] WARNING: worldName is null for " + String.valueOf(animalId) + " - will use default world");
         }

         if (!data.getGrowthStage().canBreed()) {
            return BreedingManager.FeedResult.NOT_ADULT;
         } else {
            if (!HyTamePlugin.isAlarmBasedBreedCooldown()) {
               long cooldown = this.config.getBreedingCooldown(animalType);
               if (!data.canBreed(cooldown)) {
                  return BreedingManager.FeedResult.ON_COOLDOWN;
               }
            }

            if (data.isInLove()) {
               return BreedingManager.FeedResult.ALREADY_IN_LOVE;
            } else {
               data.setInLove(true);
               if (this.debugLogger != null) {
                  Consumer var10000 = this.debugLogger;
                  String var11 = String.valueOf(animalId);
                  var10000.accept("[Breeding] Animal " + var11 + " (" + String.valueOf(animalType) + ") is now in love! entityRef=" + (entityRef != null ? "present" : "NULL"));
               }

               return BreedingManager.FeedResult.SUCCESS;
            }
         }
      }
   }

   public boolean tryBreed(UUID animal1Id, UUID animal2Id, AnimalType animalType) {
      BreedingData data1 = this.getData(animal1Id);
      BreedingData data2 = this.getData(animal2Id);
      if (data1 != null && data2 != null) {
         if (data1.isInLove() && data2.isInLove()) {
            if (!data1.isPregnant() && !data2.isPregnant()) {
               data1.setPregnant(true);
               data1.resetLove();
               data2.resetLove();
               String var10001 = String.valueOf(animal1Id);
               this.debug("Breeding started! " + var10001 + " is now pregnant with " + String.valueOf(animalType));
               return true;
            } else {
               this.debug("Cannot breed: one or both animals already pregnant");
               return false;
            }
         } else {
            this.debug("Cannot breed: both animals must be in love");
            return false;
         }
      } else {
         this.debug("Cannot breed: one or both animals not tracked");
         return false;
      }
   }

   public boolean forceBreed(UUID animal1Id, UUID animal2Id, AnimalType animalType) {
      BreedingData data1 = this.getOrCreateData(animal1Id, animalType);
      BreedingData data2 = this.getOrCreateData(animal2Id, animalType);
      data1.setPregnant(true);
      data1.resetLove();
      data2.resetLove();
      this.debug("Force breeding: " + String.valueOf(animal1Id) + " is now pregnant");
      return true;
   }

   public void tickPregnancies() {
      for(BreedingData data : this.breedingDataMap.values()) {
         if (data.isPregnant()) {
            long gestationTime = this.config.getGestationPeriod(data.getAnimalType());
            if (data.isReadyToGiveBirth(gestationTime)) {
               this.handleBirth(data);
            }
         }
      }

   }

   private void handleBirth(BreedingData motherData) {
      UUID motherId = motherData.getAnimalId();
      AnimalType animalType = motherData.getAnimalType();
      UUID babyId = UUID.randomUUID();
      BreedingData babyData = BreedingData.createBaby(babyId, animalType);
      this.breedingDataMap.put(babyId, babyData);
      motherData.completeBreeding();
      String var10001 = String.valueOf(motherId);
      this.debug("Birth! Mother " + var10001 + " gave birth to baby " + String.valueOf(babyId));
      if (this.onBirthCallback != null) {
         BirthEvent event = new BirthEvent(motherId, babyId, animalType);
         this.onBirthCallback.accept(event);
      }

   }

   public void setOnBirthCallback(Consumer<BirthEvent> callback) {
      this.onBirthCallback = callback;
   }

   public void setDebugLogger(Consumer<String> logger) {
      this.debugLogger = logger;
   }

   private void debug(String message) {
      if (this.config.isDebugMode() && this.debugLogger != null) {
         this.debugLogger.accept("[Breeding] " + message);
      }

   }

   public int getTrackedCount() {
      return this.breedingDataMap.size();
   }

   public int getPregnantCount() {
      return (int)this.breedingDataMap.values().stream().filter(BreedingData::isPregnant).count();
   }

   public int getInLoveCount() {
      return (int)this.breedingDataMap.values().stream().filter(BreedingData::isInLove).count();
   }

   public Iterable<UUID> getTrackedAnimalIds() {
      return this.breedingDataMap.keySet().stream().toList();
   }

   public Iterable<BreedingData> getAllBreedingData() {
      return this.breedingDataMap.values();
   }

   public FeedResult tryFeedCustomAnimal(UUID animalId, String modelAssetId, Ref<EntityStore> entityRef) {
      return this.tryFeedCustomAnimal(animalId, modelAssetId, entityRef, (String)null);
   }

   public FeedResult tryFeedCustomAnimal(UUID animalId, String modelAssetId, Ref<EntityStore> entityRef, String worldName) {
      CustomAnimalLoveData existing = (CustomAnimalLoveData)this.customAnimalsInLove.get(animalId);
      if (existing != null && existing.isInLove()) {
         return BreedingManager.FeedResult.ALREADY_IN_LOVE;
      } else if (existing != null && !existing.canBreed(this.config.getCustomAnimalBreedingCooldown(modelAssetId))) {
         return BreedingManager.FeedResult.ON_COOLDOWN;
      } else {
         CustomAnimalLoveData loveData = new CustomAnimalLoveData(animalId, modelAssetId, entityRef);
         loveData.setInLove(true);
         if (worldName != null) {
            loveData.setWorldName(worldName);
         }

         this.customAnimalsInLove.put(animalId, loveData);
         String var10001 = String.valueOf(animalId);
         this.debug("Custom animal " + var10001 + " (" + modelAssetId + ") is now in love!");
         return BreedingManager.FeedResult.SUCCESS;
      }
   }

   public boolean isCustomAnimalInLove(UUID animalId) {
      CustomAnimalLoveData data = (CustomAnimalLoveData)this.customAnimalsInLove.get(animalId);
      return data != null && data.isInLove();
   }

   public CustomAnimalLoveData getCustomAnimalLoveData(UUID animalId) {
      return (CustomAnimalLoveData)this.customAnimalsInLove.get(animalId);
   }

   public Iterable<CustomAnimalLoveData> getCustomAnimalsInLove() {
      return this.customAnimalsInLove.values().stream().filter(CustomAnimalLoveData::isInLove).toList();
   }

   public boolean tryBreedCustomAnimals(UUID animal1Id, UUID animal2Id, String modelAssetId) {
      CustomAnimalLoveData data1 = (CustomAnimalLoveData)this.customAnimalsInLove.get(animal1Id);
      CustomAnimalLoveData data2 = (CustomAnimalLoveData)this.customAnimalsInLove.get(animal2Id);
      if (data1 != null && data2 != null) {
         if (data1.isInLove() && data2.isInLove()) {
            if (!data1.getModelAssetId().equals(data2.getModelAssetId())) {
               this.debug("Cannot breed custom animals: different types");
               return false;
            } else {
               data1.completeBreeding();
               data2.completeBreeding();
               this.debug("Custom animal breeding complete! " + modelAssetId);
               if (this.onCustomBirthCallback != null) {
                  UUID babyId = UUID.randomUUID();
                  CustomBirthEvent event = new CustomBirthEvent(animal1Id, animal2Id, babyId, modelAssetId, data1.getEntityRef(), data2.getEntityRef());
                  this.onCustomBirthCallback.accept(event);
               }

               return true;
            }
         } else {
            this.debug("Cannot breed custom animals: both must be in love");
            return false;
         }
      } else {
         this.debug("Cannot breed custom animals: one or both not tracked");
         return false;
      }
   }

   public void setOnCustomBirthCallback(Consumer<CustomBirthEvent> callback) {
      this.onCustomBirthCallback = callback;
   }

   public void tickCustomAnimalLove() {
      long now = System.currentTimeMillis();
      long loveDuration = 30000L;

      for(CustomAnimalLoveData data : this.customAnimalsInLove.values()) {
         if (data.isInLove() && now - data.getLoveStartTime() > loveDuration) {
            data.setInLove(false);
            this.debug("Custom animal " + String.valueOf(data.getAnimalId()) + " love mode expired");
         }
      }

   }

   public Set<UUID> getTrackedBabyUuids() {
      Set<UUID> babyUuids = new HashSet();

      for(Map.Entry<UUID, BreedingData> entry : this.breedingDataMap.entrySet()) {
         if (((BreedingData)entry.getValue()).getGrowthStage() != GrowthStage.ADULT) {
            babyUuids.add((UUID)entry.getKey());
         }
      }

      return babyUuids;
   }

   public boolean isBabyTracked(Object ref, UUID refUuid) {
      BreedingData data = (BreedingData)this.breedingDataMap.get(refUuid);
      if (data != null && data.getGrowthStage() != GrowthStage.ADULT) {
         return true;
      } else if (ref instanceof Ref) {
         Ref<EntityStore> typedRef = (Ref)ref;
         return this.findBabyByRef(typedRef) != null;
      } else {
         return false;
      }
   }

   public static enum FeedResult {
      SUCCESS,
      WRONG_FOOD,
      NOT_ADULT,
      ON_COOLDOWN,
      ALREADY_IN_LOVE,
      DISABLED;

      // $FF: synthetic method
      private static FeedResult[] $values() {
         return new FeedResult[]{SUCCESS, WRONG_FOOD, NOT_ADULT, ON_COOLDOWN, ALREADY_IN_LOVE, DISABLED};
      }
   }

   public static class BirthEvent {
      private final UUID motherId;
      private final UUID babyId;
      private final AnimalType animalType;

      public BirthEvent(UUID motherId, UUID babyId, AnimalType animalType) {
         this.motherId = motherId;
         this.babyId = babyId;
         this.animalType = animalType;
      }

      public UUID getMotherId() {
         return this.motherId;
      }

      public UUID getBabyId() {
         return this.babyId;
      }

      public AnimalType getAnimalType() {
         return this.animalType;
      }
   }

   public static class CustomAnimalLoveData {
      private final UUID animalId;
      private final String modelAssetId;
      private Ref<EntityStore> entityRef;
      private String worldName;
      private boolean inLove;
      private long loveStartTime;
      private long lastBreedTime;

      public CustomAnimalLoveData(UUID animalId, String modelAssetId, Ref<EntityStore> entityRef) {
         this.animalId = animalId;
         this.modelAssetId = modelAssetId;
         this.entityRef = entityRef;
         this.inLove = false;
         this.loveStartTime = 0L;
         this.lastBreedTime = 0L;
      }

      public UUID getAnimalId() {
         return this.animalId;
      }

      public String getModelAssetId() {
         return this.modelAssetId;
      }

      public Ref<EntityStore> getEntityRef() {
         return this.entityRef;
      }

      public void setEntityRef(Ref<EntityStore> ref) {
         this.entityRef = ref;
      }

      public String getWorldName() {
         return this.worldName;
      }

      public void setWorldName(String worldName) {
         this.worldName = worldName;
      }

      public boolean isInLove() {
         return this.inLove;
      }

      public long getLoveStartTime() {
         return this.loveStartTime;
      }

      public void setInLove(boolean inLove) {
         this.inLove = inLove;
         if (inLove) {
            this.loveStartTime = System.currentTimeMillis();
         }

      }

      public boolean canBreed(long cooldownMs) {
         return System.currentTimeMillis() - this.lastBreedTime >= cooldownMs;
      }

      public void completeBreeding() {
         this.inLove = false;
         this.lastBreedTime = System.currentTimeMillis();
      }
   }

   public static class CustomBirthEvent {
      private final UUID parent1Id;
      private final UUID parent2Id;
      private final UUID babyId;
      private final String modelAssetId;
      private final Ref<EntityStore> parent1EntityRef;
      private final Ref<EntityStore> parent2EntityRef;

      public CustomBirthEvent(UUID parent1Id, UUID parent2Id, UUID babyId, String modelAssetId, Ref<EntityStore> parent1EntityRef, Ref<EntityStore> parent2EntityRef) {
         this.parent1Id = parent1Id;
         this.parent2Id = parent2Id;
         this.babyId = babyId;
         this.modelAssetId = modelAssetId;
         this.parent1EntityRef = parent1EntityRef;
         this.parent2EntityRef = parent2EntityRef;
      }

      public UUID getParent1Id() {
         return this.parent1Id;
      }

      public UUID getParent2Id() {
         return this.parent2Id;
      }

      public UUID getBabyId() {
         return this.babyId;
      }

      public String getModelAssetId() {
         return this.modelAssetId;
      }

      public Ref<EntityStore> getParent1EntityRef() {
         return this.parent1EntityRef;
      }

      public Ref<EntityStore> getParent2EntityRef() {
         return this.parent2EntityRef;
      }
   }

   public static class UntrackedBaby {
      private final Ref<EntityStore> entityRef;
      private final String modelAssetId;
      private final AnimalType animalType;

      public UntrackedBaby(Ref<EntityStore> entityRef, String modelAssetId, AnimalType animalType) {
         this.entityRef = entityRef;
         this.modelAssetId = modelAssetId;
         this.animalType = animalType;
      }

      public Ref<EntityStore> getEntityRef() {
         return this.entityRef;
      }

      public String getModelAssetId() {
         return this.modelAssetId;
      }

      public AnimalType getAnimalType() {
         return this.animalType;
      }
   }
}
