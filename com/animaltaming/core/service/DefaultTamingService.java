package com.animaltaming.core.service;

import com.animaltaming.api.TamingService;
import com.animaltaming.api.event.TamingEvents;
import com.animaltaming.api.model.TamedAnimal;
import com.animaltaming.api.model.TamingConfig;
import com.animaltaming.api.model.TamingProgress;
import com.animaltaming.core.handler.BehaviorHandler;
import com.animaltaming.core.handler.CalmingHandler;
import com.animaltaming.core.handler.FeedingHandler;
import com.animaltaming.core.registry.TamedAnimalRegistry;
import com.animaltaming.core.registry.TamingConfigRegistry;
import com.animaltaming.system.SystemContext;
import com.animaltaming.util.EventBus;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DefaultTamingService implements TamingService {
   private final TamedAnimalRegistry animalRegistry;
   private final TamingConfigRegistry configRegistry;
   private final PlayerLookupService playerLookup;
   private final EventBus eventBus;
   private final CalmingHandler calmingHandler;
   private final FeedingHandler feedingHandler;
   private final BehaviorHandler behaviorHandler;
   private SystemContext currentContext;

   public DefaultTamingService(TamedAnimalRegistry animalRegistry, TamingConfigRegistry configRegistry, PlayerLookupService playerLookup, EventBus eventBus, CalmingHandler calmingHandler, FeedingHandler feedingHandler, BehaviorHandler behaviorHandler) {
      this.animalRegistry = (TamedAnimalRegistry)Objects.requireNonNull(animalRegistry, "animalRegistry required");
      this.configRegistry = (TamingConfigRegistry)Objects.requireNonNull(configRegistry, "configRegistry required");
      this.playerLookup = (PlayerLookupService)Objects.requireNonNull(playerLookup, "playerLookup required");
      this.eventBus = (EventBus)Objects.requireNonNull(eventBus, "eventBus required");
      this.calmingHandler = (CalmingHandler)Objects.requireNonNull(calmingHandler, "calmingHandler required");
      this.feedingHandler = (FeedingHandler)Objects.requireNonNull(feedingHandler, "feedingHandler required");
      this.behaviorHandler = (BehaviorHandler)Objects.requireNonNull(behaviorHandler, "behaviorHandler required");
   }

   public void setContext(SystemContext context) {
      this.currentContext = context;
   }

   public Optional<TamingProgress> startCalming(long animalEntityId, UUID animalId, String speciesId, UUID playerId) {
      if (!this.configRegistry.contains(speciesId)) {
         return Optional.empty();
      } else if (this.calmingHandler.hasProgress(animalEntityId)) {
         return Optional.empty();
      } else {
         long currentTick = this.currentContext.getCurrentTick();
         TamingProgress progress = TamingProgress.startCalming(animalId, speciesId, playerId, currentTick);
         this.calmingHandler.setProgress(animalEntityId, progress);
         return Optional.of(progress);
      }
   }

   public Optional<TamingProgress> feed(long animalEntityId, UUID playerId, String foodId) {
      long currentTick = this.currentContext.getCurrentTick();
      return this.feedingHandler.feed(this.currentContext, animalEntityId, playerId, foodId, currentTick);
   }

   public Optional<TamedAnimal> completeTaming(long animalEntityId, String ownerName, double x, double y, double z) {
      Optional<TamingProgress> progressOpt = this.calmingHandler.getProgress(animalEntityId);
      if (progressOpt.isEmpty()) {
         return Optional.empty();
      } else {
         TamingProgress progress = (TamingProgress)progressOpt.get();
         Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
         if (configOpt.isEmpty()) {
            return Optional.empty();
         } else {
            TamingConfig config = (TamingConfig)configOpt.get();
            if (progress.trustLevel() < config.requiredTrustLevel()) {
               return Optional.empty();
            } else {
               TamedAnimal tamedAnimal = TamedAnimal.create(progress.animalId(), progress.attemptingPlayerId(), ownerName, progress.speciesId(), x, y, z, config.maxFollowDistance());
               this.animalRegistry.register(tamedAnimal, animalEntityId);
               this.calmingHandler.removeProgress(animalEntityId);
               this.eventBus.publish(new TamingEvents.AnimalTamedEvent(progress.attemptingPlayerId(), ownerName, animalEntityId, tamedAnimal.id(), progress.speciesId()));
               return Optional.of(tamedAnimal);
            }
         }
      }
   }

   public boolean toggleBehaviorMode(UUID animalId, UUID playerId, double x, double y, double z) {
      return this.behaviorHandler.toggleMode(this.currentContext, animalId, playerId, x, y, z);
   }

   public Optional<TamedAnimal> getTamedAnimal(UUID animalId) {
      return this.animalRegistry.getByAnimalId(animalId);
   }

   public Set<TamedAnimal> getAnimalsOwnedBy(UUID playerId) {
      return this.animalRegistry.getByOwnerId(playerId);
   }

   public Optional<TamingProgress> getTamingProgress(long animalEntityId) {
      return this.calmingHandler.getProgress(animalEntityId);
   }

   public void removeTamingProgress(long animalEntityId) {
      this.calmingHandler.removeProgress(animalEntityId);
   }

   public boolean releaseTamedAnimal(UUID animalId, UUID playerId) {
      Optional<TamedAnimal> animalOpt = this.animalRegistry.getByAnimalId(animalId);
      if (animalOpt.isEmpty()) {
         return false;
      } else {
         TamedAnimal animal = (TamedAnimal)animalOpt.get();
         if (!animal.isOwnedBy(playerId)) {
            return false;
         } else {
            this.animalRegistry.unregister(animalId);
            this.eventBus.publish(new TamingEvents.TamedAnimalLostEvent(animalId, playerId, animal.speciesId(), "released"));
            return true;
         }
      }
   }

   public void updateTamedAnimal(TamedAnimal animal) {
      this.animalRegistry.update(animal);
   }

   public boolean canCompleteTaming(long animalEntityId) {
      Optional<TamingProgress> progressOpt = this.calmingHandler.getProgress(animalEntityId);
      if (progressOpt.isEmpty()) {
         return false;
      } else {
         TamingProgress progress = (TamingProgress)progressOpt.get();
         Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
         if (configOpt.isEmpty()) {
            return false;
         } else {
            return progress.trustLevel() >= ((TamingConfig)configOpt.get()).requiredTrustLevel();
         }
      }
   }
}
