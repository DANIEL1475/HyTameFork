package com.animaltaming.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class HytaleEntityAdapter {
   private final Map<Long, Entity> entityMap = new ConcurrentHashMap();
   private final Map<Entity, Long> reverseMap = new ConcurrentHashMap();
   private final Map<UUID, Long> uuidToId = new ConcurrentHashMap();
   private final AtomicLong nextId = new AtomicLong(1L);

   public long registerEntity(Entity entity) {
      if (entity == null) {
         throw new IllegalArgumentException("Entity cannot be null");
      } else {
         Long existingId = (Long)this.reverseMap.get(entity);
         if (existingId != null) {
            return existingId;
         } else {
            long id = this.nextId.getAndIncrement();
            this.entityMap.put(id, entity);
            this.reverseMap.put(entity, id);
            UUID uuid = entity.getUuid();
            if (uuid != null) {
               this.uuidToId.put(uuid, id);
            }

            return id;
         }
      }
   }

   public Optional<Entity> getEntity(long entityId) {
      return Optional.ofNullable((Entity)this.entityMap.get(entityId));
   }

   public Optional<LivingEntity> getLivingEntity(long entityId) {
      return this.getEntity(entityId).filter((e) -> e instanceof LivingEntity).map((e) -> (LivingEntity)e);
   }

   public Optional<Player> getPlayer(long entityId) {
      return this.getEntity(entityId).filter((e) -> e instanceof Player).map((e) -> (Player)e);
   }

   public Optional<Long> getEntityId(Entity entity) {
      return Optional.ofNullable((Long)this.reverseMap.get(entity));
   }

   public Optional<Long> getEntityIdByUuid(UUID uuid) {
      return Optional.ofNullable((Long)this.uuidToId.get(uuid));
   }

   public void unregisterEntity(long entityId) {
      Entity entity = (Entity)this.entityMap.remove(entityId);
      if (entity != null) {
         this.reverseMap.remove(entity);
         UUID uuid = entity.getUuid();
         if (uuid != null) {
            this.uuidToId.remove(uuid);
         }
      }

   }

   public void unregisterEntity(Entity entity) {
      Long id = (Long)this.reverseMap.remove(entity);
      if (id != null) {
         this.entityMap.remove(id);
         UUID uuid = entity.getUuid();
         if (uuid != null) {
            this.uuidToId.remove(uuid);
         }
      }

   }

   public boolean isRegistered(long entityId) {
      return this.entityMap.containsKey(entityId);
   }

   public Set<Long> getAllEntityIds() {
      return Set.copyOf(this.entityMap.keySet());
   }

   public Set<Map.Entry<Long, Entity>> getAllMappings() {
      return Set.copyOf(this.entityMap.entrySet());
   }

   public Optional<Long> findEntityIdByRef(Ref<EntityStore> targetRef) {
      if (targetRef != null && targetRef.isValid()) {
         for(Map.Entry<Long, Entity> entry : this.entityMap.entrySet()) {
            Entity entity = (Entity)entry.getValue();
            Ref<EntityStore> ref = entity.getReference();
            if (ref != null && ref.equals(targetRef)) {
               return Optional.of((Long)entry.getKey());
            }
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   public int size() {
      return this.entityMap.size();
   }

   public void clear() {
      this.entityMap.clear();
      this.reverseMap.clear();
      this.uuidToId.clear();
   }

   public int cleanupStaleEntities() {
      int removed = 0;
      Iterator<Map.Entry<Long, Entity>> iterator = this.entityMap.entrySet().iterator();

      while(iterator.hasNext()) {
         Map.Entry<Long, Entity> entry = (Map.Entry)iterator.next();
         Entity entity = (Entity)entry.getValue();

         try {
            Ref<EntityStore> ref = entity.getReference();
            if (ref == null || !ref.isValid()) {
               iterator.remove();
               this.reverseMap.remove(entity);
               UUID uuid = entity.getUuid();
               if (uuid != null) {
                  this.uuidToId.remove(uuid);
               }

               ++removed;
            }
         } catch (Exception var7) {
            iterator.remove();
            this.reverseMap.remove(entity);
            ++removed;
         }
      }

      return removed;
   }
}
