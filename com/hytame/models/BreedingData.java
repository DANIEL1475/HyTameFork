package com.hytame.models;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class BreedingData {
   private final UUID animalId;
   private final AnimalType animalType;
   private long lastBreedTime;
   private boolean isPregnant;
   private long pregnancyStartTime;
   private GrowthStage growthStage;
   private long birthTime;
   private boolean inLove;
   private long loveStartTime;
   private Ref<EntityStore> entityRef;
   private String worldName;
   private boolean tamed;
   private UUID ownerUuid;
   private String customName;
   private long tamedTime;
   private boolean allowInteraction = true;

   public BreedingData(UUID animalId, AnimalType animalType) {
      this.animalId = animalId;
      this.animalType = animalType;
      this.lastBreedTime = 0L;
      this.isPregnant = false;
      this.pregnancyStartTime = 0L;
      this.growthStage = GrowthStage.ADULT;
      this.birthTime = 0L;
      this.inLove = false;
      this.loveStartTime = 0L;
   }

   public static BreedingData createBaby(UUID animalId, AnimalType animalType) {
      BreedingData data = new BreedingData(animalId, animalType);
      data.growthStage = GrowthStage.BABY;
      data.birthTime = System.currentTimeMillis();
      return data;
   }

   public UUID getAnimalId() {
      return this.animalId;
   }

   public AnimalType getAnimalType() {
      return this.animalType;
   }

   public long getLastBreedTime() {
      return this.lastBreedTime;
   }

   public void setLastBreedTime(long lastBreedTime) {
      this.lastBreedTime = lastBreedTime;
   }

   public boolean isPregnant() {
      return this.isPregnant;
   }

   public void setPregnant(boolean pregnant) {
      this.isPregnant = pregnant;
      if (pregnant) {
         this.pregnancyStartTime = System.currentTimeMillis();
      }

   }

   public long getPregnancyStartTime() {
      return this.pregnancyStartTime;
   }

   public GrowthStage getGrowthStage() {
      return this.growthStage;
   }

   public void setGrowthStage(GrowthStage growthStage) {
      this.growthStage = growthStage;
   }

   public long getBirthTime() {
      return this.birthTime;
   }

   public boolean isInLove() {
      return this.inLove;
   }

   public void setInLove(boolean inLove) {
      this.inLove = inLove;
      if (inLove) {
         this.loveStartTime = System.currentTimeMillis();
      }

   }

   public long getLoveStartTime() {
      return this.loveStartTime;
   }

   public boolean canBreed(long cooldownMillis) {
      if (!this.growthStage.canBreed()) {
         return false;
      } else if (this.isPregnant) {
         return false;
      } else {
         long timeSinceLastBreed = System.currentTimeMillis() - this.lastBreedTime;
         return timeSinceLastBreed >= cooldownMillis;
      }
   }

   public long getCooldownRemaining(long cooldownMillis) {
      long timeSinceLastBreed = System.currentTimeMillis() - this.lastBreedTime;
      long remaining = cooldownMillis - timeSinceLastBreed;
      return Math.max(0L, remaining);
   }

   public long getGestationRemaining(long gestationMillis) {
      if (!this.isPregnant) {
         return 0L;
      } else {
         long timeSincePregnancy = System.currentTimeMillis() - this.pregnancyStartTime;
         long remaining = gestationMillis - timeSincePregnancy;
         return Math.max(0L, remaining);
      }
   }

   public boolean isReadyToGiveBirth(long gestationMillis) {
      if (!this.isPregnant) {
         return false;
      } else {
         return this.getGestationRemaining(gestationMillis) == 0L;
      }
   }

   public long getAge() {
      return this.birthTime == 0L ? 0L : System.currentTimeMillis() - this.birthTime;
   }

   public void resetLove() {
      this.inLove = false;
      this.loveStartTime = 0L;
   }

   public void completeBreeding() {
      this.lastBreedTime = System.currentTimeMillis();
      this.isPregnant = false;
      this.pregnancyStartTime = 0L;
      this.resetLove();
   }

   public Ref<EntityStore> getEntityRef() {
      return this.entityRef;
   }

   public void setEntityRef(Ref<EntityStore> entityRef) {
      this.entityRef = entityRef;
   }

   public String getWorldName() {
      return this.worldName;
   }

   public void setWorldName(String worldName) {
      this.worldName = worldName;
   }

   public boolean isTamed() {
      return this.tamed;
   }

   public void setTamed(boolean tamed, UUID owner) {
      this.tamed = tamed;
      this.ownerUuid = owner;
      if (tamed && this.tamedTime == 0L) {
         this.tamedTime = System.currentTimeMillis();
      }

      if (!tamed) {
         this.ownerUuid = null;
         this.customName = null;
         this.tamedTime = 0L;
      }

   }

   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public String getCustomName() {
      return this.customName;
   }

   public void setCustomName(String customName) {
      this.customName = customName;
   }

   public long getTamedTime() {
      return this.tamedTime;
   }

   public boolean isOwnedBy(UUID playerUuid) {
      return this.ownerUuid != null && this.ownerUuid.equals(playerUuid);
   }

   public boolean isAllowInteraction() {
      return this.allowInteraction;
   }

   public void setAllowInteraction(boolean allowInteraction) {
      this.allowInteraction = allowInteraction;
   }

   public boolean canInteract(UUID playerUuid) {
      if (!this.tamed) {
         return true;
      } else {
         return this.isOwnedBy(playerUuid) ? true : this.allowInteraction;
      }
   }
}
