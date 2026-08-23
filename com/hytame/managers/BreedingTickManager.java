package com.hytame.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.joml.Vector3d;

public class BreedingTickManager {
   private static final long LOVE_DURATION = 30000L;
   private static final double BREEDING_DISTANCE = (double)5.0F;
   private final BreedingManager breedingManager;
   private final ConfigManager configManager;
   private final Map<AnimalType, List<BreedingData>> tickLoveByType = new HashMap();
   private final List<Object> tickLoveEntityRefs = new ArrayList();
   private BiConsumer<AnimalType, BreedingData[]> onBreedingComplete;
   private BiConsumer<String, BreedingManager.CustomAnimalLoveData[]> onCustomBreedingComplete;
   private Consumer<Object> heartParticleSpawner;
   private BiConsumer<Vector3d, Store<EntityStore>> heartParticlePositionSpawner;
   private boolean verboseLogging = false;

   public BreedingTickManager(BreedingManager breedingManager, ConfigManager configManager) {
      this.breedingManager = breedingManager;
      this.configManager = configManager;
   }

   private void logVerbose(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   public void setOnBreedingComplete(BiConsumer<AnimalType, BreedingData[]> callback) {
      this.onBreedingComplete = callback;
   }

   public void setOnCustomBreedingComplete(BiConsumer<String, BreedingManager.CustomAnimalLoveData[]> callback) {
      this.onCustomBreedingComplete = callback;
   }

   public void setHeartParticleSpawner(Consumer<Object> spawner) {
      this.heartParticleSpawner = spawner;
   }

   public void setHeartParticlePositionSpawner(BiConsumer<Vector3d, Store<EntityStore>> spawner) {
      this.heartParticlePositionSpawner = spawner;
   }

   public void setVerboseLogging(boolean verbose) {
      this.verboseLogging = verbose;
   }

   public void tick() {
      int trackedCount = this.breedingManager.getTrackedCount();
      int inLoveTotal = this.breedingManager.getInLoveCount();
      if (inLoveTotal > 0 && this.verboseLogging) {
         this.logVerbose("[TickLove] Running: tracked=" + trackedCount + ", inLove=" + inLoveTotal);
      }

      if (trackedCount != 0) {
         long now = System.currentTimeMillis();
         this.tickLoveByType.values().forEach(List::clear);
         this.tickLoveByType.clear();
         this.tickLoveEntityRefs.clear();
         int inLoveCount = 0;
         List<BreedingData> animalsInLove = new ArrayList();

         for(BreedingData data : this.breedingManager.getAllBreedingData()) {
            if (data.isInLove()) {
               if (now - data.getLoveStartTime() > 30000L) {
                  data.resetLove();
               } else {
                  animalsInLove.add(data);
                  if (!data.isPregnant() && data.getGrowthStage().canBreed()) {
                     ((List)this.tickLoveByType.computeIfAbsent(data.getAnimalType(), (k) -> new ArrayList())).add(data);
                     ++inLoveCount;
                  }
               }
            }
         }

         this.breedingManager.tickCustomAnimalLove();

         for(BreedingManager.CustomAnimalLoveData customData : this.breedingManager.getCustomAnimalsInLove()) {
            if (customData.getEntityRef() != null) {
               this.tickLoveEntityRefs.add(customData.getEntityRef());
            }
         }

         Map<String, List<BreedingData>> animalsByWorld = new HashMap();

         for(BreedingData data : animalsInLove) {
            String worldName = data.getWorldName();
            if (worldName == null) {
               worldName = "__default__";
            }

            ((List)animalsByWorld.computeIfAbsent(worldName, (k) -> new ArrayList())).add(data);
         }

         if (!animalsInLove.isEmpty() && this.heartParticlePositionSpawner != null) {
            for(Map.Entry<String, List<BreedingData>> worldEntry : animalsByWorld.entrySet()) {
               String worldName = (String)worldEntry.getKey();
               List<BreedingData> worldAnimals = (List)worldEntry.getValue();
               World world = "__default__".equals(worldName) ? Universe.get().getDefaultWorld() : Universe.get().getWorld(worldName);
               if (world == null) {
                  world = Universe.get().getDefaultWorld();
               }

               if (world != null) {
                  world.execute(() -> {
                     try {
                        Store<EntityStore> store = world.getEntityStore().getStore();

                        for(BreedingData data : worldAnimals) {
                           Vector3d pos = this.findEntityPositionByUuid(store, data);
                           if (pos != null) {
                              this.heartParticlePositionSpawner.accept(pos, store);
                           }
                        }
                     } catch (Exception var7) {
                     }

                  });
               }
            }
         }

         if (!this.tickLoveEntityRefs.isEmpty() && this.heartParticleSpawner != null) {
            for(Object entityRef : this.tickLoveEntityRefs) {
               this.heartParticleSpawner.accept(entityRef);
            }
         }

         if (inLoveCount >= 2) {
            for(Map.Entry<AnimalType, List<BreedingData>> entry : this.tickLoveByType.entrySet()) {
               List<BreedingData> animalsOfType = (List)entry.getValue();
               if (animalsOfType.size() >= 2) {
                  Map<String, List<BreedingData>> typeByWorld = new HashMap();

                  for(BreedingData data : animalsOfType) {
                     if (data.getEntityRef() != null) {
                        String worldName = data.getWorldName();
                        if (worldName == null) {
                           worldName = "__default__";
                        }

                        ((List)typeByWorld.computeIfAbsent(worldName, (k) -> new ArrayList())).add(data);
                     }
                  }

                  for(Map.Entry<String, List<BreedingData>> worldEntry : typeByWorld.entrySet()) {
                     List<BreedingData> worldAnimals = (List)worldEntry.getValue();
                     if (worldAnimals.size() >= 2) {
                        String worldName = (String)worldEntry.getKey();
                        World world = "__default__".equals(worldName) ? Universe.get().getDefaultWorld() : Universe.get().getWorld(worldName);
                        if (world == null) {
                           world = Universe.get().getDefaultWorld();
                        }

                        if (world != null) {
                           BreedingData animal1 = (BreedingData)worldAnimals.get(0);
                           BreedingData animal2 = (BreedingData)worldAnimals.get(1);
                           this.checkBreedingDistance(world, (AnimalType)entry.getKey(), animal1, animal2);
                        }
                     }
                  }
               }
            }

            this.checkCustomAnimalBreeding();
         }
      }
   }

   private void checkBreedingDistance(World world, AnimalType animalType, BreedingData animal1, BreedingData animal2) {
      world.execute(() -> {
         try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Vector3d pos1 = this.findEntityPositionByUuid(store, animal1);
            Vector3d pos2 = this.findEntityPositionByUuid(store, animal2);
            if (pos1 == null || pos2 == null) {
               this.logVerbose("[Breeding] Position lookup failed - pos1: " + (pos1 != null) + ", pos2: " + (pos2 != null));
               return;
            }

            double distance = this.calculateDistance(pos1, pos2);
            if (distance <= (double)5.0F && this.onBreedingComplete != null) {
               animal1.completeBreeding();
               animal2.completeBreeding();
               this.onBreedingComplete.accept(animalType, new BreedingData[]{animal1, animal2});
            }
         } catch (Exception var10) {
         }

      });
   }

   private void checkCustomAnimalBreeding() {
      Map<String, List<BreedingManager.CustomAnimalLoveData>> byTypeAndWorld = new HashMap();

      for(BreedingManager.CustomAnimalLoveData data : this.breedingManager.getCustomAnimalsInLove()) {
         if (data.isInLove() && data.getEntityRef() != null) {
            String worldName = data.getWorldName();
            if (worldName == null) {
               worldName = "__default__";
            }

            String key = worldName + ":" + data.getModelAssetId();
            ((List)byTypeAndWorld.computeIfAbsent(key, (k) -> new ArrayList())).add(data);
         }
      }

      for(Map.Entry<String, List<BreedingManager.CustomAnimalLoveData>> entry : byTypeAndWorld.entrySet()) {
         List<BreedingManager.CustomAnimalLoveData> animalsOfType = (List)entry.getValue();
         if (animalsOfType.size() >= 2) {
            BreedingManager.CustomAnimalLoveData animal1 = (BreedingManager.CustomAnimalLoveData)animalsOfType.get(0);
            BreedingManager.CustomAnimalLoveData animal2 = (BreedingManager.CustomAnimalLoveData)animalsOfType.get(1);
            if (animal1.getEntityRef() != null && animal2.getEntityRef() != null) {
               String worldName = animal1.getWorldName();
               World world = worldName != null && !"__default__".equals(worldName) ? Universe.get().getWorld(worldName) : Universe.get().getDefaultWorld();
               if (world == null) {
                  world = Universe.get().getDefaultWorld();
               }

               if (world != null) {
                  String modelAssetId = animal1.getModelAssetId();
                  world.execute(() -> {
                     try {
                        Store<EntityStore> store = world.getEntityStore().getStore();
                        Vector3d pos1 = this.getPositionFromRef(store, animal1.getEntityRef());
                        Vector3d pos2 = this.getPositionFromRef(store, animal2.getEntityRef());
                        if (pos1 == null || pos2 == null) {
                           return;
                        }

                        double distance = this.calculateDistance(pos1, pos2);
                        if (distance <= (double)5.0F) {
                           this.logVerbose("[CustomBreed] Breeding " + modelAssetId + " at distance " + String.format("%.1f", distance));
                           animal1.completeBreeding();
                           animal2.completeBreeding();
                           if (this.onCustomBreedingComplete != null) {
                              this.onCustomBreedingComplete.accept(modelAssetId, new BreedingManager.CustomAnimalLoveData[]{animal1, animal2});
                           }
                        }
                     } catch (Exception var10) {
                     }

                  });
               }
            }
         }
      }

   }

   private Vector3d getPositionFromRef(Store<EntityStore> store, Object entityRef) {
      if (entityRef == null) {
         return null;
      } else {
         try {
            if (entityRef instanceof Ref) {
               Ref<EntityStore> ref = (Ref)entityRef;
               Store<EntityStore> refStore = ref.getStore();
               if (refStore == null) {
                  refStore = store;
               }

               TransformComponent transform = (TransformComponent)refStore.getComponent(ref, TransformComponent.getComponentType());
               if (transform != null) {
                  return transform.getPosition();
               }
            }
         } catch (Exception var6) {
         }

         return null;
      }
   }

   private double calculateDistance(Vector3d pos1, Vector3d pos2) {
      double dx = pos2.x() - pos1.x();
      double dy = pos2.y() - pos1.y();
      double dz = pos2.z() - pos1.z();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private Vector3d findEntityPositionByUuid(Store<EntityStore> store, BreedingData data) {
      UUID animalId = data.getAnimalId();
      if (animalId != null && store != null) {
         Ref<EntityStore>[] foundRef = new Ref[1];
         Vector3d[] foundPos = new Vector3d[1];

         try {
            store.forEachChunk((chunk, buffer) -> {
               if (foundRef[0] == null) {
                  int chunkSize = chunk.size();

                  for(int i = 0; i < chunkSize; ++i) {
                     try {
                        UUIDComponent uuidComp = (UUIDComponent)chunk.getComponent(i, EcsReflectionUtil.UUID_TYPE);
                        if (uuidComp != null && animalId.equals(uuidComp.getUuid())) {
                           Ref<EntityStore> ref = chunk.getReferenceTo(i);
                           if (ref != null && ref.isValid()) {
                              TransformComponent transform = (TransformComponent)chunk.getComponent(i, EcsReflectionUtil.TRANSFORM_TYPE);
                              if (transform != null) {
                                 foundRef[0] = ref;
                                 foundPos[0] = transform.getPosition();
                              }
                           }

                           return;
                        }
                     } catch (Exception var10) {
                     }
                  }

               }
            });
            if (foundRef[0] != null && foundPos[0] != null) {
               data.setEntityRef(foundRef[0]);
               return foundPos[0];
            }
         } catch (Exception var7) {
         }

         return null;
      } else {
         return null;
      }
   }

   public static double getBreedingDistance() {
      return (double)5.0F;
   }

   public static long getLoveDuration() {
      return 30000L;
   }
}
