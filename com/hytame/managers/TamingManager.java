package com.hytame.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.integration.SimpleClaimsIntegration;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.NameplateUtil;
import com.hytame.util.TameHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TamingManager {
   private final Map<UUID, TamedAnimalData> tamedAnimalsByUuid = new ConcurrentHashMap();
   private final Map<UUID, TamedAnimalData> tamedByHytameId = new ConcurrentHashMap();
   private PersistenceManager persistenceManager;
   private volatile long initializationTime = 0L;
   private int gracePeriodMs = 15000;
   private int expectedEntityCount = 0;
   private int foundEntityCount = 0;
   private volatile boolean loadingComplete = false;
   private volatile long loadingCompleteTime = 0L;
   private static final int POST_LOAD_BUFFER_MS = 5000;
   private static final int MAX_GRACE_PERIOD_MS = 30000;

   public void setGracePeriodMs(int ms) {
      this.gracePeriodMs = ms;
   }

   public void markInitialized() {
      this.initializationTime = System.currentTimeMillis();
      this.log("TamingManager initialized - grace period of " + this.gracePeriodMs / 1000 + "s started");
   }

   public boolean isInGracePeriod() {
      if (this.initializationTime == 0L) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         if (now - this.initializationTime > 30000L) {
            if (!this.loadingComplete) {
               this.loadingComplete = true;
               this.loadingCompleteTime = now;
               this.log("Grace period max timeout reached - " + this.foundEntityCount + "/" + this.expectedEntityCount + " entities found. Proceeding anyway.");
            }

            return false;
         } else if (!this.loadingComplete) {
            return true;
         } else {
            return now - this.loadingCompleteTime < 5000L;
         }
      }
   }

   public void onEntityLinked(UUID hytameId) {
      if (!this.loadingComplete) {
         ++this.foundEntityCount;
         String var10001 = String.valueOf(hytameId);
         this.log("Entity linked: " + var10001 + " (" + this.foundEntityCount + "/" + this.expectedEntityCount + ")");
         if (this.foundEntityCount >= this.expectedEntityCount && !this.loadingComplete) {
            this.loadingComplete = true;
            this.loadingCompleteTime = System.currentTimeMillis();
            this.log("All " + this.expectedEntityCount + " tamed entities found - 5s buffer started");
         }

      }
   }

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   private void logAlways(String message) {
      ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
   }

   private Ref<EntityStore> findEntityRefByUuid(UUID entityUuid) {
      if (entityUuid == null) {
         return null;
      } else {
         try {
            for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
               World world = (World)entry.getValue();
               if (world != null) {
                  Store<EntityStore> store = world.getEntityStore().getStore();
                  if (store != null) {
                     Ref<EntityStore>[] found = new Ref[1];
                     store.forEachChunk((chunk, buffer) -> {
                        if (found[0] == null) {
                           int size = chunk.size();

                           for(int i = 0; i < size; ++i) {
                              try {
                                 UUIDComponent uuidComp = (UUIDComponent)chunk.getComponent(i, EcsReflectionUtil.UUID_TYPE);
                                 if (uuidComp != null && entityUuid.equals(uuidComp.getUuid())) {
                                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                                    if (ref != null && ref.isValid()) {
                                       found[0] = ref;
                                    }

                                    return;
                                 }
                              } catch (Exception var8) {
                              }
                           }

                        }
                     });
                     if (found[0] != null) {
                        String var10001 = String.valueOf(entityUuid);
                        this.logAlways("findEntityRefByUuid: found " + var10001 + " in world " + (String)entry.getKey());
                        return found[0];
                     }
                  }
               }
            }
         } catch (Exception e) {
            this.logAlways("findEntityRefByUuid: error scanning worlds: " + e.getMessage());
         }

         this.logAlways("findEntityRefByUuid: entity " + String.valueOf(entityUuid) + " not found in any world");
         return null;
      }
   }

   public void setPersistenceManager(PersistenceManager persistenceManager) {
      this.persistenceManager = persistenceManager;
   }

   private void markDirty(boolean saveImmediately) {
      if (this.persistenceManager != null) {
         if (saveImmediately) {
            this.persistenceManager.forceSave(this.getAllTamedAnimals());
         } else {
            this.persistenceManager.markDirty();
         }
      }

   }

   private void markDirty() {
      this.markDirty(false);
   }

   public void saveImmediately() {
      this.markDirty(true);
   }

   public void notifyDataChanged() {
      this.markDirty(true);
   }

   public void loadFromPersistence(List<TamedAnimalData> savedAnimals) {
      this.tamedAnimalsByUuid.clear();
      this.tamedByHytameId.clear();
      int migrated = 0;
      int despawnedCount = 0;
      int capturedCount = 0;
      this.foundEntityCount = 0;
      this.loadingComplete = false;
      this.loadingCompleteTime = 0L;

      for(TamedAnimalData data : savedAnimals) {
         if (data != null && data.getAnimalUuid() != null) {
            if (data.getHytameId() == null) {
               data.ensureHytameId();
               ++migrated;
            }

            data.setEntityRef((Ref)null);
            if (data.isDespawned()) {
               ++despawnedCount;
            }

            if (data.isCaptured()) {
               ++capturedCount;
            }

            this.tamedAnimalsByUuid.put(data.getAnimalUuid(), data);
            this.tamedByHytameId.put(data.getHytameId(), data);
         }
      }

      this.expectedEntityCount = 0;

      for(TamedAnimalData data : this.tamedAnimalsByUuid.values()) {
         if (!data.isDespawned() && !data.isCaptured() && !data.isDead()) {
            ++this.expectedEntityCount;
         }
      }

      this.log("Loaded " + this.tamedAnimalsByUuid.size() + " tamed animals from persistence");
      this.log("  - " + despawnedCount + " marked as despawned");
      this.log("  - " + capturedCount + " marked as captured");
      this.log("  - " + this.expectedEntityCount + " expected to exist in world (for smart loading)");
      if (migrated > 0) {
         this.log("Migrated " + migrated + " animals to new hytameId system");
         this.markDirty(true);
      }

   }

   public Collection<TamedAnimalData> getAllTamedAnimals() {
      return new ArrayList(this.tamedAnimalsByUuid.values());
   }

   public boolean isTamed(UUID animalId) {
      return animalId != null && this.tamedAnimalsByUuid.containsKey(animalId);
   }

   public TamedAnimalData getTamedData(UUID animalId) {
      return animalId != null ? (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId) : null;
   }

   public TamedAnimalData tameAnimal(UUID animalId, UUID ownerUuid, String name, AnimalType type) {
      return this.tameAnimal(animalId, ownerUuid, name, type, (Ref)null);
   }

   public TamedAnimalData tameAnimal(UUID animalId, UUID ownerUuid, String name, AnimalType type, Ref<EntityStore> entityRef) {
      return this.tameAnimal(animalId, ownerUuid, name, type, entityRef, (double)0.0F, (double)0.0F, (double)0.0F);
   }

   public TamedAnimalData tameAnimal(UUID animalId, UUID ownerUuid, String name, AnimalType type, Ref<EntityStore> entityRef, double x, double y, double z) {
      return this.tameAnimal(animalId, ownerUuid, name, type, entityRef, x, y, z, GrowthStage.ADULT);
   }

   public TamedAnimalData tameAnimal(UUID animalId, UUID ownerUuid, String name, AnimalType type, Ref<EntityStore> entityRef, double x, double y, double z, GrowthStage growthStage) {
      return this.tameAnimal((UUID)null, animalId, ownerUuid, name, type, entityRef, x, y, z, growthStage, (String)null);
   }

   public TamedAnimalData tameAnimal(UUID animalId, UUID ownerUuid, String name, AnimalType type, Ref<EntityStore> entityRef, double x, double y, double z, GrowthStage growthStage, String worldName) {
      return this.tameAnimal((UUID)null, animalId, ownerUuid, name, type, entityRef, x, y, z, growthStage, worldName);
   }

   public TamedAnimalData tameAnimal(UUID hytameId, UUID animalId, UUID ownerUuid, String name, AnimalType type, Ref<EntityStore> entityRef, double x, double y, double z, GrowthStage growthStage, String worldName) {
      if (animalId != null && ownerUuid != null && name != null) {
         if (this.tamedAnimalsByUuid.containsKey(animalId)) {
            this.log("Animal already tamed: " + String.valueOf(animalId));
            TamedAnimalData existing = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
            if (entityRef != null && existing.getEntityRef() == null) {
               existing.setEntityRef(entityRef);
            }

            if (x != (double)0.0F || y != (double)0.0F || z != (double)0.0F) {
               existing.setLastPosition(x, y, z);
            }

            if (growthStage != null && existing.getGrowthStage() != growthStage) {
               existing.setGrowthStage(growthStage);
            }

            if (worldName != null && !worldName.isEmpty() && !worldName.equals(existing.getWorldId())) {
               existing.setWorldId(worldName);
            }

            return existing;
         } else if (hytameId != null && this.tamedByHytameId.containsKey(hytameId)) {
            this.log("Found existing data by hytameId: " + String.valueOf(hytameId) + " (updating entity UUID)");
            TamedAnimalData existing = (TamedAnimalData)this.tamedByHytameId.get(hytameId);
            UUID oldUuid = existing.getAnimalUuid();
            if (oldUuid != null) {
               this.tamedAnimalsByUuid.remove(oldUuid);
            }

            existing.setAnimalUuid(animalId);
            existing.setDespawned(false);
            if (entityRef != null) {
               existing.setEntityRef(entityRef);
            }

            if (x != (double)0.0F || y != (double)0.0F || z != (double)0.0F) {
               existing.setLastPosition(x, y, z);
            }

            if (worldName != null && !worldName.isEmpty()) {
               existing.setWorldId(worldName);
            }

            this.tamedAnimalsByUuid.put(animalId, existing);
            this.markDirty(true);
            return existing;
         } else {
            TamedAnimalData data;
            if (hytameId != null) {
               data = new TamedAnimalData(hytameId, animalId, ownerUuid, name, type);
            } else {
               data = new TamedAnimalData(animalId, ownerUuid, name, type);
            }

            if (entityRef != null) {
               data.setEntityRef(entityRef);
               if (type == null) {
                  Store<EntityStore> store = entityRef.getStore();
                  if (store != null) {
                     String modelId = EcsReflectionUtil.getEntityModelAssetId(store, entityRef);
                     if (modelId != null) {
                        data.setModelAssetId(modelId);
                     }
                  }
               }
            }

            if (x != (double)0.0F || y != (double)0.0F || z != (double)0.0F) {
               data.setLastPosition(x, y, z);
            }

            if (growthStage != null) {
               data.setGrowthStage(growthStage);
            }

            if (growthStage == GrowthStage.BABY) {
               data.setBirthTime(System.currentTimeMillis());
            }

            if (worldName != null && !worldName.isEmpty()) {
               data.setWorldId(worldName);
            }

            this.tamedAnimalsByUuid.put(animalId, data);
            this.tamedByHytameId.put(data.getHytameId(), data);
            this.markDirty(true);
            this.log("Tamed animal: " + name + " (" + String.valueOf(type) + ", " + String.valueOf(growthStage) + ") in world=" + (worldName != null ? worldName : "default") + " at (" + String.format("%.1f, %.1f, %.1f", x, y, z) + ") hytameId=" + String.valueOf(data.getHytameId()) + " owned by " + String.valueOf(ownerUuid));
            return data;
         }
      } else {
         return null;
      }
   }

   public boolean untameAnimal(UUID animalId, UUID playerUuid) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data == null) {
         return false;
      } else if (!data.isOwnedBy(playerUuid)) {
         return false;
      } else {
         this.tamedAnimalsByUuid.remove(animalId);
         if (data.getHytameId() != null) {
            this.tamedByHytameId.remove(data.getHytameId());
         }

         this.markDirty(true);
         String var10001 = data.getCustomName();
         this.log("Untamed animal: " + var10001 + " by " + String.valueOf(playerUuid));
         return true;
      }
   }

   public int clearAllTames() {
      int count = this.tamedAnimalsByUuid.size();
      this.tamedAnimalsByUuid.clear();
      this.tamedByHytameId.clear();
      this.markDirty(true);
      this.log("Admin cleared all " + count + " tamed animals");
      return count;
   }

   public int clearPlayerTames(UUID playerUuid) {
      List<UUID> toRemove = (List)this.tamedAnimalsByUuid.entrySet().stream().filter((e) -> ((TamedAnimalData)e.getValue()).isOwnedBy(playerUuid)).map(Map.Entry::getKey).collect(Collectors.toList());

      for(UUID id : toRemove) {
         TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.remove(id);
         if (data != null && data.getHytameId() != null) {
            this.tamedByHytameId.remove(data.getHytameId());
         }
      }

      if (!toRemove.isEmpty()) {
         this.markDirty(true);
      }

      int var10001 = toRemove.size();
      this.log("Admin cleared " + var10001 + " tamed animals for player " + String.valueOf(playerUuid));
      return toRemove.size();
   }

   public int clearPlayerTamesByName(String playerName) {
      List<UUID> toRemove = (List)this.tamedAnimalsByUuid.entrySet().stream().filter((e) -> playerName.equalsIgnoreCase(((TamedAnimalData)e.getValue()).getOwnerName())).map(Map.Entry::getKey).collect(Collectors.toList());

      for(UUID id : toRemove) {
         TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.remove(id);
         if (data != null && data.getHytameId() != null) {
            this.tamedByHytameId.remove(data.getHytameId());
         }
      }

      if (!toRemove.isEmpty()) {
         this.markDirty(true);
      }

      int var10001 = toRemove.size();
      this.log("Admin cleared " + var10001 + " tamed animals for player name '" + playerName + "'");
      return toRemove.size();
   }

   public boolean releaseAnimal(UUID hytameId, UUID playerUuid) {
      TamedAnimalData data = this.findByHytameId(hytameId);
      if (data == null) {
         this.logAlways("releaseAnimal: no data for hytameId " + String.valueOf(hytameId));
         return false;
      } else if (!data.isOwnedBy(playerUuid)) {
         String var6 = String.valueOf(playerUuid);
         this.logAlways("releaseAnimal: player " + var6 + " is not owner of " + String.valueOf(hytameId));
         return false;
      } else {
         Ref<EntityStore> entityRef = data.getEntityRef();
         if (entityRef == null || !entityRef.isValid()) {
            this.logAlways("releaseAnimal: stored entityRef is stale, searching worlds for " + String.valueOf(data.getAnimalUuid()));
            entityRef = this.findEntityRefByUuid(data.getAnimalUuid());
            if (entityRef != null) {
               data.setEntityRef(entityRef);
               this.logAlways("releaseAnimal: refreshed entityRef from world scan");
            }
         }

         String var10001 = data.getCustomName();
         this.logAlways("releaseAnimal: releasing " + var10001 + " (entityRef=" + String.valueOf(entityRef) + ", valid=" + (entityRef != null && entityRef.isValid()) + ", animalType=" + String.valueOf(data.getAnimalType()) + ")");
         TameHelper.releaseAnimalEntity(entityRef, data.getAnimalType());
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getBreedingManager() != null && data.getAnimalUuid() != null) {
            plugin.getBreedingManager().removeData(data.getAnimalUuid());
         }

         return this.untameAnimal(data.getAnimalUuid(), playerUuid);
      }
   }

   public boolean renameAnimal(UUID animalId, UUID playerUuid, String newName) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data == null) {
         return false;
      } else if (!data.isOwnedBy(playerUuid)) {
         return false;
      } else {
         String oldName = data.getCustomName();
         data.setCustomName(newName);
         this.markDirty(true);
         Ref<EntityStore> entityRef = data.getEntityRef();
         if (entityRef == null || !entityRef.isValid()) {
            entityRef = this.findEntityRefByUuid(animalId);
            if (entityRef != null) {
               data.setEntityRef(entityRef);
            }
         }

         if (entityRef != null && entityRef.isValid()) {
            NameplateUtil.setEntityNameplate(entityRef, newName);
         }

         this.log("Renamed animal: " + oldName + " -> " + newName);
         return true;
      }
   }

   public boolean canPlayerInteract(UUID animalId, UUID playerUuid) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      return data == null ? true : data.canInteract(playerUuid);
   }

   public UUID getOwner(UUID animalId) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      return data != null ? data.getOwnerUuid() : null;
   }

   public boolean isOwner(UUID animalId, UUID playerUuid) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      return data != null && data.isOwnedBy(playerUuid);
   }

   public boolean toggleAllowInteraction(UUID ownerUuid) {
      boolean currentState = true;

      for(TamedAnimalData data : this.tamedAnimalsByUuid.values()) {
         if (data.isOwnedBy(ownerUuid)) {
            currentState = data.isAllowInteraction();
            break;
         }
      }

      boolean newState = !currentState;

      for(TamedAnimalData data : this.tamedAnimalsByUuid.values()) {
         if (data.isOwnedBy(ownerUuid)) {
            data.setAllowInteraction(newState);
         }
      }

      this.markDirty();
      String var10001 = String.valueOf(ownerUuid);
      this.log("Player " + var10001 + " set allowInteraction to " + newState);
      return newState;
   }

   public void setAllowInteraction(UUID animalId, boolean allow) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data != null) {
         data.setAllowInteraction(allow);
         this.markDirty();
      }

   }

   public void onTamedAnimalDespawn(UUID animalId, double x, double y, double z) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data != null) {
         data.setLastPosition(x, y, z);
         data.setDespawned(true);
         data.setEntityRef((Ref)null);
         this.markDirty();
         String var10001 = data.getCustomName();
         this.log("Tamed animal despawned: " + var10001 + " at (" + String.format("%.1f, %.1f, %.1f", x, y, z) + ")");
      }

   }

   public void onTamedAnimalDeath(UUID animalId) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data != null) {
         data.setDead(true);
         data.setEntityRef((Ref)null);
         this.markDirty(true);
         this.log("Tamed animal died: " + data.getCustomName() + " (marked as dead, not removed)");
      }

   }

   public void updatePosition(UUID animalId, double x, double y, double z) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data != null && !data.isDespawned()) {
         data.setLastPosition(x, y, z);
      }

   }

   public void updateEntityRef(UUID animalId, Ref<EntityStore> entityRef) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data != null) {
         data.setEntityRef(entityRef);
      }

   }

   public void updateEntityRef(UUID hytameId, Ref<EntityStore> entityRef, boolean isHytameId) {
      if (!isHytameId) {
         this.updateEntityRef(hytameId, entityRef);
      } else {
         for(TamedAnimalData data : this.tamedAnimalsByUuid.values()) {
            if (hytameId.equals(data.getHytameId())) {
               data.setEntityRef(entityRef);
               this.log("Updated entity ref for hytameId: " + String.valueOf(hytameId));
               return;
            }
         }

      }
   }

   public void updateEntityAfterGrowth(UUID hytameId, UUID newEntityUuid, Ref<EntityStore> newEntityRef) {
      TamedAnimalData data = (TamedAnimalData)this.tamedByHytameId.get(hytameId);
      if (data == null) {
         this.log("updateEntityAfterGrowth: No data for hytameId " + String.valueOf(hytameId));
      } else {
         UUID oldUuid = data.getAnimalUuid();
         if (oldUuid != null) {
            this.tamedAnimalsByUuid.remove(oldUuid);
         }

         data.setAnimalUuid(newEntityUuid);
         data.setEntityRef(newEntityRef);
         data.setGrowthStage(GrowthStage.ADULT);
         this.tamedAnimalsByUuid.put(newEntityUuid, data);
         this.markDirty();
         String var10001 = String.valueOf(oldUuid);
         this.log("updateEntityAfterGrowth: Re-keyed " + var10001 + " → " + String.valueOf(newEntityUuid));
      }
   }

   public void updateGrowthStage(UUID hytameId, GrowthStage newStage) {
      for(TamedAnimalData data : this.tamedAnimalsByUuid.values()) {
         if (hytameId.equals(data.getHytameId())) {
            data.setGrowthStage(newStage);
            String var10001 = String.valueOf(hytameId);
            this.log("Updated growth stage for hytameId " + var10001 + " to " + String.valueOf(newStage));
            return;
         }
      }

      this.log("Could not find animal with hytameId: " + String.valueOf(hytameId) + " for growth stage update");
   }

   public List<TamedAnimalData> getDespawnedAnimalsInRegion(double x, double z, double radius) {
      double radiusSq = radius * radius;
      return (List)this.tamedAnimalsByUuid.values().stream().filter(TamedAnimalData::isDespawned).filter((data) -> !data.isDead()).filter((data) -> {
         double dx = data.getLastX() - x;
         double dz = data.getLastZ() - z;
         return dx * dx + dz * dz <= radiusSq;
      }).collect(Collectors.toList());
   }

   public void markRespawned(UUID oldUuid, UUID newUuid, Ref<EntityStore> entityRef) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.remove(oldUuid);
      if (data != null) {
         data.setAnimalUuid(newUuid);
         data.setDespawned(false);
         data.setEntityRef(entityRef);
         this.tamedAnimalsByUuid.put(newUuid, data);
         this.markDirty();
         String var10001 = data.getCustomName();
         this.log("Respawned tamed animal: " + var10001 + " (new UUID: " + String.valueOf(newUuid) + ")");
      }

   }

   public void syncFromBreedingData(UUID animalId, BreedingData breedingData) {
      TamedAnimalData data = (TamedAnimalData)this.tamedAnimalsByUuid.get(animalId);
      if (data != null && breedingData != null) {
         data.copyFromBreedingData(breedingData);
      }

   }

   public int getTamedCount() {
      return this.tamedAnimalsByUuid.size();
   }

   public int getDespawnedCount() {
      return (int)this.tamedAnimalsByUuid.values().stream().filter(TamedAnimalData::isDespawned).count();
   }

   public int getPlayerTamedCount(UUID playerUuid) {
      return (int)this.tamedAnimalsByUuid.values().stream().filter((data) -> data.isOwnedBy(playerUuid)).count();
   }

   public int getPlayerActiveTameCount(UUID playerUuid) {
      return (int)this.tamedAnimalsByUuid.values().stream().filter((data) -> data.isOwnedBy(playerUuid)).filter((data) -> !data.isDead() && !data.isCaptured()).count();
   }

   public String checkTameLimit(UUID ownerUuid, double x, double z, String worldName) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin == null) {
         return null;
      } else {
         ConfigManager config = plugin.getConfigManager();
         if (config == null) {
            return null;
         } else {
            if (config.isUsePerPlayerLimit()) {
               int current = this.getPlayerActiveTameCount(ownerUuid);
               int limit = config.getPerPlayerTameLimit();
               if (current >= limit) {
                  return "You have reached the tame limit (" + current + "/" + limit + ")";
               }
            }

            if (config.isUsePerClaimLimit() && plugin.isSimpleClaimsInstalled()) {
               SimpleClaimsIntegration sc = plugin.getSimpleClaimsIntegration();
               if (sc != null && worldName != null) {
                  UUID partyId = sc.getPartyIdAtPosition(worldName, (int)x, (int)z);
                  if (partyId != null) {
                     int current = sc.countTamedAnimalsInParty(partyId, worldName, this);
                     int limit = config.getPerClaimTameLimit();
                     if (current >= limit) {
                        return "This claim has reached the tame limit (" + current + "/" + limit + ")";
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public List<TamedAnimalData> getPlayerAnimals(UUID playerUuid) {
      return (List)this.tamedAnimalsByUuid.values().stream().filter((data) -> data.isOwnedBy(playerUuid)).collect(Collectors.toList());
   }

   public TamedAnimalData findByName(UUID ownerUuid, String name) {
      return (TamedAnimalData)this.tamedAnimalsByUuid.values().stream().filter((data) -> ownerUuid == null || data.isOwnedBy(ownerUuid)).filter((data) -> data.getCustomName() != null && data.getCustomName().equalsIgnoreCase(name)).findFirst().orElse((Object)null);
   }

   public int cleanupStaleData(long maxDespawnedAgeMillis) {
      long now = System.currentTimeMillis();
      List<UUID> toRemove = new ArrayList();

      for(Map.Entry<UUID, TamedAnimalData> entry : this.tamedAnimalsByUuid.entrySet()) {
         TamedAnimalData data = (TamedAnimalData)entry.getValue();
         if (data.isDespawned()) {
         }
      }

      for(UUID uuid : toRemove) {
         TamedAnimalData removed = (TamedAnimalData)this.tamedAnimalsByUuid.remove(uuid);
         if (removed != null && removed.getHytameId() != null) {
            this.tamedByHytameId.remove(removed.getHytameId());
         }
      }

      if (!toRemove.isEmpty()) {
         this.markDirty();
         this.log("Cleaned up " + toRemove.size() + " stale tamed animal entries");
      }

      return toRemove.size();
   }

   public TamedAnimalData findByHytameId(UUID hytameId) {
      return hytameId != null ? (TamedAnimalData)this.tamedByHytameId.get(hytameId) : null;
   }

   public TamedAnimalData findCapturedByType(AnimalType type) {
      if (type == null) {
         return null;
      } else {
         for(TamedAnimalData data : this.tamedByHytameId.values()) {
            if (data.isCaptured() && data.getAnimalType() == type) {
               String var10001 = String.valueOf(data.getHytameId());
               this.log("Found captured animal: hytameId=" + var10001 + " name=" + data.getCustomName());
               return data;
            }
         }

         return null;
      }
   }

   public Map<UUID, TamedAnimalData> getTamedByHytameId() {
      return new HashMap(this.tamedByHytameId);
   }

   public SyncResult syncEntity(UUID entityUuid, UUID hytameId, boolean isTamed, UUID tamerUuid, String tamerName, Ref<EntityStore> entityRef, double x, double y, double z) {
      if (isTamed && hytameId != null) {
         TamedAnimalData jsonData = this.findByHytameId(hytameId);
         if (jsonData != null) {
            if (jsonData.isDead()) {
               this.log("WARN: Entity with hytameId=" + String.valueOf(hytameId) + " exists but JSON says dead");
               return TamingManager.SyncResult.SHOULD_REMOVE;
            } else {
               UUID oldUuid = jsonData.getAnimalUuid();
               if (!entityUuid.equals(oldUuid)) {
                  if (oldUuid != null) {
                     this.tamedAnimalsByUuid.remove(oldUuid);
                  }

                  jsonData.setAnimalUuid(entityUuid);
                  this.tamedAnimalsByUuid.put(entityUuid, jsonData);
               }

               jsonData.setEntityRef(entityRef);
               jsonData.setDespawned(false);
               jsonData.setLastPosition(x, y, z);
               this.onEntityLinked(hytameId);
               String var10001 = String.valueOf(entityUuid);
               this.log("Synced entity " + var10001 + " with hytameId=" + String.valueOf(hytameId) + " (name: " + jsonData.getCustomName() + ")");
               return TamingManager.SyncResult.SYNCED;
            }
         } else {
            TamedAnimalData newData = this.tameAnimal(hytameId, entityUuid, tamerUuid, "_UNDEFINED", (AnimalType)null, entityRef, x, y, z, GrowthStage.ADULT, (String)null);
            if (newData != null) {
               if (tamerName != null) {
                  newData.setOwnerName(tamerName);
               }

               this.log("Created JSON entry for existing tamed entity hytameId=" + String.valueOf(hytameId));
               return TamingManager.SyncResult.CREATED;
            } else {
               return TamingManager.SyncResult.NO_ACTION;
            }
         }
      } else {
         return TamingManager.SyncResult.NO_ACTION;
      }
   }

   public List<TamedAnimalData> getAnimalsNeedingRespawn() {
      return (List)this.tamedAnimalsByUuid.values().stream().filter((data) -> data.getEntityRef() == null).filter((data) -> !data.isDead()).filter((data) -> data.isDespawned()).collect(Collectors.toList());
   }

   public int cleanupOrphanedFlags() {
      int cleaned = 0;

      for(TamedAnimalData data : this.tamedByHytameId.values()) {
         if (data.isCaptured() && data.getEntityRef() == null && !data.isDespawned()) {
            String var10001 = String.valueOf(data.getHytameId());
            this.log("Clearing orphaned isCaptured flag for: " + var10001 + " (" + data.getCustomName() + ")");
            data.setCaptured(false);
            ++cleaned;
         }
      }

      if (cleaned > 0) {
         this.markDirty(true);
         this.log("Cleaned up " + cleaned + " orphaned captured flags");
      }

      return cleaned;
   }

   public void clearAllEntityRefs() {
      for(TamedAnimalData data : this.tamedAnimalsByUuid.values()) {
         if (data.getEntityRef() != null) {
            data.setEntityRef((Ref)null);
            data.setDespawned(true);
         }
      }

      this.markDirty();
      this.log("Cleared all entity references (world unload/shutdown)");
   }

   public static enum SyncResult {
      SYNCED,
      CREATED,
      NEEDS_RESPAWN,
      SHOULD_REMOVE,
      NO_ACTION;

      // $FF: synthetic method
      private static SyncResult[] $values() {
         return new SyncResult[]{SYNCED, CREATED, NEEDS_RESPAWN, SHOULD_REMOVE, NO_ACTION};
      }
   }
}
