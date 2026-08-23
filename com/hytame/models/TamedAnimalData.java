package com.hytame.models;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class TamedAnimalData {
   private UUID hytameId;
   private UUID animalUuid;
   private UUID ownerUuid;
   private String ownerName;
   private String customName;
   private AnimalType animalType;
   private String modelAssetId;
   private double lastX;
   private double lastY;
   private double lastZ;
   private float lastRotation;
   private String worldId;
   private long lastBreedTime;
   private long birthTime;
   private GrowthStage growthStage;
   private long tamedTime;
   private boolean isDespawned;
   private boolean isRespawning;
   private boolean isDead;
   private long deathTime;
   private long despawnTime;
   private boolean allowInteraction;
   private boolean isCaptured;
   private transient Ref<EntityStore> entityRef;

   public TamedAnimalData() {
      this.allowInteraction = true;
      this.growthStage = GrowthStage.ADULT;
   }

   public TamedAnimalData(UUID animalUuid, UUID ownerUuid, String customName, AnimalType animalType) {
      this.hytameId = UUID.randomUUID();
      this.animalUuid = animalUuid;
      this.ownerUuid = ownerUuid;
      this.customName = customName;
      this.animalType = animalType;
      this.tamedTime = System.currentTimeMillis();
      this.isDespawned = false;
      this.allowInteraction = true;
      this.growthStage = GrowthStage.ADULT;
      this.worldId = "default";
   }

   public TamedAnimalData(UUID hytameId, UUID animalUuid, UUID ownerUuid, String customName, AnimalType animalType) {
      this.hytameId = hytameId != null ? hytameId : UUID.randomUUID();
      this.animalUuid = animalUuid;
      this.ownerUuid = ownerUuid;
      this.customName = customName;
      this.animalType = animalType;
      this.tamedTime = System.currentTimeMillis();
      this.isDespawned = false;
      this.allowInteraction = true;
      this.growthStage = GrowthStage.ADULT;
      this.worldId = "default";
   }

   public UUID getHytameId() {
      return this.hytameId;
   }

   public void setHytameId(UUID hytameId) {
      this.hytameId = hytameId;
   }

   public void ensureHytameId() {
      if (this.hytameId == null) {
         this.hytameId = UUID.randomUUID();
      }

   }

   public UUID getAnimalUuid() {
      return this.animalUuid;
   }

   public void setAnimalUuid(UUID animalUuid) {
      this.animalUuid = animalUuid;
   }

   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public void setOwnerUuid(UUID ownerUuid) {
      this.ownerUuid = ownerUuid;
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
   }

   public String getCustomName() {
      return this.customName;
   }

   public void setCustomName(String customName) {
      this.customName = customName;
   }

   public AnimalType getAnimalType() {
      return this.animalType;
   }

   public void setAnimalType(AnimalType animalType) {
      this.animalType = animalType;
   }

   public String getModelAssetId() {
      return this.modelAssetId;
   }

   public void setModelAssetId(String modelAssetId) {
      this.modelAssetId = modelAssetId;
   }

   public double getLastX() {
      return this.lastX;
   }

   public double getLastY() {
      return this.lastY;
   }

   public double getLastZ() {
      return this.lastZ;
   }

   public void setLastPosition(double x, double y, double z) {
      this.lastX = x;
      this.lastY = y;
      this.lastZ = z;
   }

   public float getLastRotation() {
      return this.lastRotation;
   }

   public void setLastRotation(float rotation) {
      this.lastRotation = rotation;
   }

   public String getWorldId() {
      return this.worldId;
   }

   public void setWorldId(String worldId) {
      this.worldId = worldId;
   }

   public long getLastBreedTime() {
      return this.lastBreedTime;
   }

   public void setLastBreedTime(long lastBreedTime) {
      this.lastBreedTime = lastBreedTime;
   }

   public long getBirthTime() {
      return this.birthTime;
   }

   public void setBirthTime(long birthTime) {
      this.birthTime = birthTime;
   }

   public GrowthStage getGrowthStage() {
      return this.growthStage;
   }

   public void setGrowthStage(GrowthStage growthStage) {
      this.growthStage = growthStage;
   }

   public long getTamedTime() {
      return this.tamedTime;
   }

   public void setTamedTime(long tamedTime) {
      this.tamedTime = tamedTime;
   }

   public boolean isRespawning() {
      return this.isRespawning;
   }

   public void setRespawnInProgress(boolean respawning) {
      this.isRespawning = respawning;
   }

   public boolean isDespawned() {
      return this.isDespawned;
   }

   public void setDespawned(boolean despawned) {
      this.isDespawned = despawned;
      if (despawned) {
         this.despawnTime = System.currentTimeMillis();
      }

   }

   public boolean isDead() {
      return this.isDead;
   }

   public void setDead(boolean dead) {
      this.isDead = dead;
      if (dead) {
         this.deathTime = System.currentTimeMillis();
      }

   }

   public long getDeathTime() {
      return this.deathTime;
   }

   public long getDespawnTime() {
      return this.despawnTime;
   }

   public boolean isAllowInteraction() {
      return this.allowInteraction;
   }

   public void setAllowInteraction(boolean allowInteraction) {
      this.allowInteraction = allowInteraction;
   }

   public boolean isCaptured() {
      return this.isCaptured;
   }

   public void setCaptured(boolean captured) {
      this.isCaptured = captured;
   }

   public Ref<EntityStore> getEntityRef() {
      return this.entityRef;
   }

   public void setEntityRef(Ref<EntityStore> entityRef) {
      this.entityRef = entityRef;
   }

   public boolean isOwnedBy(UUID playerUuid) {
      return this.ownerUuid != null && this.ownerUuid.equals(playerUuid);
   }

   public boolean canInteract(UUID playerUuid) {
      return this.isOwnedBy(playerUuid) ? true : this.allowInteraction;
   }

   public void copyFromBreedingData(BreedingData data) {
      if (data != null) {
         this.lastBreedTime = data.getLastBreedTime();
         this.birthTime = data.getBirthTime();
         this.growthStage = data.getGrowthStage();
      }

   }

   public void applyToBreedingData(BreedingData data) {
      if (data != null) {
         data.setLastBreedTime(this.lastBreedTime);
         data.setGrowthStage(this.growthStage);
      }

   }

   public String toString() {
      String var10000 = String.valueOf(this.hytameId);
      return "TamedAnimalData{hytameId=" + var10000 + ", name='" + this.customName + "', type=" + String.valueOf(this.animalType) + ", owner=" + String.valueOf(this.ownerUuid) + ", despawned=" + this.isDespawned + "}";
   }
}
