package com.animaltaming.core.registry;

import com.animaltaming.api.model.TamedAnimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TamedAnimalRegistry {
   private final Map<UUID, TamedAnimal> byAnimalId = new ConcurrentHashMap();
   private final Map<UUID, Set<UUID>> byOwnerId = new ConcurrentHashMap();
   private final Map<Long, UUID> entityToAnimalId = new ConcurrentHashMap();

   public void register(TamedAnimal animal, long entityId) {
      Objects.requireNonNull(animal, "animal is required");
      this.byAnimalId.put(animal.id(), animal);
      this.entityToAnimalId.put(entityId, animal.id());
      ((Set)this.byOwnerId.computeIfAbsent(animal.ownerId(), (k) -> ConcurrentHashMap.newKeySet())).add(animal.id());
   }

   public void update(TamedAnimal animal) {
      Objects.requireNonNull(animal, "animal is required");
      TamedAnimal existing = (TamedAnimal)this.byAnimalId.get(animal.id());
      if (existing == null) {
         throw new IllegalStateException("Animal not registered: " + String.valueOf(animal.id()));
      } else {
         if (!existing.ownerId().equals(animal.ownerId())) {
            Set<UUID> oldOwnerAnimals = (Set)this.byOwnerId.get(existing.ownerId());
            if (oldOwnerAnimals != null) {
               oldOwnerAnimals.remove(animal.id());
            }

            ((Set)this.byOwnerId.computeIfAbsent(animal.ownerId(), (k) -> ConcurrentHashMap.newKeySet())).add(animal.id());
         }

         this.byAnimalId.put(animal.id(), animal);
      }
   }

   public void unregister(UUID animalId) {
      TamedAnimal animal = (TamedAnimal)this.byAnimalId.remove(animalId);
      if (animal != null) {
         Set<UUID> ownerAnimals = (Set)this.byOwnerId.get(animal.ownerId());
         if (ownerAnimals != null) {
            ownerAnimals.remove(animalId);
         }
      }

      this.entityToAnimalId.entrySet().removeIf((entry) -> ((UUID)entry.getValue()).equals(animalId));
   }

   public void updateEntityId(UUID animalId, long newEntityId) {
      this.entityToAnimalId.entrySet().removeIf((entry) -> ((UUID)entry.getValue()).equals(animalId));
      this.entityToAnimalId.put(newEntityId, animalId);
   }

   public Optional<TamedAnimal> getByAnimalId(UUID animalId) {
      return Optional.ofNullable((TamedAnimal)this.byAnimalId.get(animalId));
   }

   public Optional<TamedAnimal> getByEntityId(long entityId) {
      UUID animalId = (UUID)this.entityToAnimalId.get(entityId);
      return animalId == null ? Optional.empty() : this.getByAnimalId(animalId);
   }

   public Optional<UUID> getAnimalIdForEntity(long entityId) {
      return Optional.ofNullable((UUID)this.entityToAnimalId.get(entityId));
   }

   public Set<TamedAnimal> getByOwnerId(UUID ownerId) {
      Set<UUID> animalIds = (Set)this.byOwnerId.get(ownerId);
      if (animalIds != null && !animalIds.isEmpty()) {
         Set<TamedAnimal> result = new HashSet();

         for(UUID animalId : animalIds) {
            TamedAnimal animal = (TamedAnimal)this.byAnimalId.get(animalId);
            if (animal != null) {
               result.add(animal);
            }
         }

         return result;
      } else {
         return Set.of();
      }
   }

   public boolean isTamedAnimal(long entityId) {
      return this.entityToAnimalId.containsKey(entityId);
   }

   public Collection<TamedAnimal> getAll() {
      return Collections.unmodifiableCollection(this.byAnimalId.values());
   }

   public int size() {
      return this.byAnimalId.size();
   }

   public void clear() {
      this.byAnimalId.clear();
      this.byOwnerId.clear();
      this.entityToAnimalId.clear();
   }
}
