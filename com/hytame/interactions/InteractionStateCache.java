package com.hytame.interactions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.models.AnimalType;
import com.hytame.models.OriginalInteractionState;
import com.hytame.util.EcsReflectionUtil;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionStateCache {
   private static final InteractionStateCache INSTANCE = new InteractionStateCache();
   private final Map<String, OriginalInteractionState> originalStates = new ConcurrentHashMap();

   private InteractionStateCache() {
   }

   public static InteractionStateCache getInstance() {
      return INSTANCE;
   }

   public String getOriginalInteractionId(Ref<EntityStore> entityRef) {
      return this.getOriginalInteractionId(entityRef, (AnimalType)null);
   }

   public String getOriginalInteractionId(Ref<EntityStore> entityRef, AnimalType animalType) {
      String key = EcsReflectionUtil.getStableEntityKey(entityRef);
      if (key != null) {
         OriginalInteractionState stored = (OriginalInteractionState)this.originalStates.get(key);
         if (stored != null && stored.hasInteraction()) {
            return stored.getInteractionId();
         }
      }

      return null;
   }

   public OriginalInteractionState getOriginalState(Ref<EntityStore> entityRef) {
      String key = EcsReflectionUtil.getStableEntityKey(entityRef);
      return key != null ? (OriginalInteractionState)this.originalStates.get(key) : null;
   }

   public void storeOriginalState(Ref<EntityStore> entityRef, String interactionId, String hint, AnimalType animalType) {
      String key = EcsReflectionUtil.getStableEntityKey(entityRef);
      if (key != null) {
         this.originalStates.put(key, new OriginalInteractionState(interactionId, hint));
      }
   }

   public void storeOriginalInteractionId(Ref<EntityStore> entityRef, String interactionId, AnimalType animalType) {
      this.storeOriginalState(entityRef, interactionId, (String)null, animalType);
   }

   public void storeOriginalInteractionId(Ref<EntityStore> entityRef, String interactionId) {
      this.storeOriginalState(entityRef, interactionId, (String)null, (AnimalType)null);
   }

   public int cleanupStaleEntries() {
      int removed = 0;
      Iterator<Map.Entry<String, OriginalInteractionState>> it = this.originalStates.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry<String, OriginalInteractionState> entry = (Map.Entry)it.next();
         String key = (String)entry.getKey();
         if (key.startsWith("idx:")) {
            it.remove();
            ++removed;
         }
      }

      return removed;
   }

   public OriginalInteractionState remove(Ref<EntityStore> entityRef) {
      String key = EcsReflectionUtil.getStableEntityKey(entityRef);
      return key != null ? (OriginalInteractionState)this.originalStates.remove(key) : null;
   }

   public OriginalInteractionState removeByKey(String key) {
      return key != null ? (OriginalInteractionState)this.originalStates.remove(key) : null;
   }

   public int getCacheSize() {
      return this.originalStates.size();
   }

   public void clear() {
      this.originalStates.clear();
   }
}
