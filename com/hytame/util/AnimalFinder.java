package com.hytame.util;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.models.AnimalType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AnimalFinder {
   private static final ComponentType<EntityStore, ModelComponent> MODEL_COMPONENT_TYPE;

   public static void findFarmAnimals(World world, Consumer<List<FoundAnimal>> callback) {
      findAnimals(world, true, callback);
   }

   public static void findAnimals(World world, boolean farmOnly, Consumer<List<FoundAnimal>> callback) {
      if (world == null) {
         callback.accept(new ArrayList());
      } else {
         EntityStore entityStore = world.getEntityStore();
         if (entityStore == null) {
            callback.accept(new ArrayList());
         } else {
            Store<EntityStore> store = entityStore.getStore();
            if (store == null) {
               callback.accept(new ArrayList());
            } else {
               List<FoundAnimal> results = new ArrayList();
               world.execute(() -> {
                  try {
                     scanEntities(store, farmOnly, results);
                  } catch (Exception var5) {
                  }

                  callback.accept(results);
               });
            }
         }
      }
   }

   private static void scanEntities(Store<EntityStore> store, boolean farmOnly, List<FoundAnimal> results) {
      store.forEachChunk((chunk, buffer) -> processChunk(chunk, farmOnly, results));
   }

   private static void processChunk(ArchetypeChunk<EntityStore> chunk, boolean farmOnly, List<FoundAnimal> results) {
      int chunkSize = chunk.size();
      if (chunkSize != 0) {
         for(int i = 0; i < chunkSize; ++i) {
            try {
               ModelComponent modelComp = (ModelComponent)chunk.getComponent(i, MODEL_COMPONENT_TYPE);
               if (modelComp != null) {
                  String assetId = extractModelAssetId(modelComp);
                  if (assetId != null) {
                     if (farmOnly) {
                        AnimalType type = AnimalType.fromModelAssetId(assetId);
                        if (type == null) {
                           continue;
                        }
                     }

                     Ref<EntityStore> entityRef = chunk.getReferenceTo(i);
                     if (entityRef != null) {
                        results.add(new FoundAnimal(entityRef, assetId));
                     }
                  }
               }
            } catch (Exception var8) {
            }
         }

      }
   }

   private static String extractModelAssetId(ModelComponent modelComp) {
      try {
         Model model = modelComp.getModel();
         return model == null ? null : model.getModelAssetId();
      } catch (Exception var2) {
         return null;
      }
   }

   static {
      MODEL_COMPONENT_TYPE = EcsReflectionUtil.MODEL_TYPE;
   }

   public static class FoundAnimal {
      private final Ref<EntityStore> entityRef;
      private final String modelAssetId;
      private final AnimalType animalType;
      private final boolean isBaby;

      public FoundAnimal(Ref<EntityStore> entityRef, String modelAssetId) {
         this.entityRef = entityRef;
         this.modelAssetId = modelAssetId;
         this.animalType = AnimalType.fromModelAssetId(modelAssetId);
         this.isBaby = AnimalType.isBabyVariant(modelAssetId);
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

      public boolean isBaby() {
         return this.isBaby;
      }

      public boolean isFarmAnimal() {
         return this.animalType != null;
      }

      public String toString() {
         String var10000 = this.modelAssetId;
         return var10000 + (this.isBaby ? " (baby)" : "") + (this.animalType != null ? " [" + String.valueOf(this.animalType) + "]" : "");
      }
   }
}
