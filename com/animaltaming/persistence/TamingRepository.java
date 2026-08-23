package com.animaltaming.persistence;

import com.animaltaming.api.model.TamedAnimal;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TamingRepository {
   void save(TamedAnimal var1);

   Optional<TamedAnimal> load(UUID var1);

   boolean delete(UUID var1);

   Set<UUID> findByOwner(UUID var1);

   Collection<TamedAnimal> loadAll();

   default void saveAll(Collection<TamedAnimal> animals) {
      for(TamedAnimal animal : animals) {
         this.save(animal);
      }

   }

   boolean exists(UUID var1);

   int count();
}
