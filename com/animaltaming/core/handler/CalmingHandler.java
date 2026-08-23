package com.animaltaming.core.handler;

import com.animaltaming.api.event.TamingEvents;
import com.animaltaming.api.model.TamingConfig;
import com.animaltaming.api.model.TamingProgress;
import com.animaltaming.api.model.TamingState;
import com.animaltaming.core.registry.TamingConfigRegistry;
import com.animaltaming.core.service.PlayerLookupService;
import com.animaltaming.system.SystemContext;
import com.animaltaming.util.EventBus;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CalmingHandler {
   private final PlayerLookupService playerLookup;
   private final TamingConfigRegistry configRegistry;
   private final EventBus eventBus;
   private final Map<Long, TamingProgress> progressByEntityId = new HashMap();

   public CalmingHandler(PlayerLookupService playerLookup, TamingConfigRegistry configRegistry, EventBus eventBus) {
      this.playerLookup = (PlayerLookupService)Objects.requireNonNull(playerLookup, "playerLookup required");
      this.configRegistry = (TamingConfigRegistry)Objects.requireNonNull(configRegistry, "configRegistry required");
      this.eventBus = (EventBus)Objects.requireNonNull(eventBus, "eventBus required");
   }

   public void process(SystemContext context, long currentTick) {
      this.processExistingProgress(context, currentTick);
      this.checkForNewCalmingAttempts(context, currentTick);
      this.cleanupExpiredCalm(context, currentTick);
   }

   private void processExistingProgress(SystemContext context, long currentTick) {
      Iterator<Map.Entry<Long, TamingProgress>> iter = this.progressByEntityId.entrySet().iterator();

      while(iter.hasNext()) {
         Map.Entry<Long, TamingProgress> entry = (Map.Entry)iter.next();
         long entityId = (Long)entry.getKey();
         TamingProgress progress = (TamingProgress)entry.getValue();
         if (!context.entityExists(entityId)) {
            iter.remove();
         } else if (progress.state() == TamingState.CALMING) {
            this.processCalmingState(context, entityId, progress, currentTick, iter);
         }
      }

   }

   private void processCalmingState(SystemContext context, long entityId, TamingProgress progress, long currentTick, Iterator<Map.Entry<Long, TamingProgress>> iter) {
      UUID attemptingPlayer = progress.attemptingPlayerId();
      if (attemptingPlayer == null) {
         iter.remove();
      } else {
         Optional<Long> playerEntityOpt = this.playerLookup.getEntityId(attemptingPlayer);
         if (playerEntityOpt.isEmpty()) {
            this.interruptCalming(context, entityId, progress, "Player offline", iter);
         } else {
            long playerId = (Long)playerEntityOpt.get();
            if (!context.isPlayerSneaking(playerId)) {
               this.interruptCalming(context, entityId, progress, "Player stopped sneaking", iter);
            } else {
               Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
               if (configOpt.isEmpty()) {
                  iter.remove();
               } else {
                  TamingConfig config = (TamingConfig)configOpt.get();
                  double distance = context.getDistance(playerId, entityId);
                  if (distance > config.calmingDistance()) {
                     this.interruptCalming(context, entityId, progress, "Player too far", iter);
                  } else {
                     long calmingDuration = currentTick - progress.calmingStartTick();
                     if (calmingDuration >= (long)config.calmingTimeTicks()) {
                        TamingProgress updated = progress.withCalmed(currentTick, config.calmDurationTicks());
                        this.progressByEntityId.put(entityId, updated);
                        this.eventBus.publish(new TamingEvents.AnimalCalmedEvent(playerId, attemptingPlayer, entityId, progress.animalId(), progress.speciesId()));
                        context.sendMessage(playerId, "The " + config.speciesId() + " has calmed down!");
                        context.spawnParticle(context.getEntityX(entityId), context.getEntityY(entityId) + (double)1.0F, context.getEntityZ(entityId), "heart");
                     }

                  }
               }
            }
         }
      }
   }

   private void interruptCalming(SystemContext context, long entityId, TamingProgress progress, String reason, Iterator<Map.Entry<Long, TamingProgress>> iter) {
      iter.remove();
      this.eventBus.publish(new TamingEvents.TamingStateChangedEvent(entityId, progress.animalId(), progress.state(), TamingState.WILD));
   }

   private void checkForNewCalmingAttempts(SystemContext context, long currentTick) {
      for(SystemContext.TameableAnimalInfo animal : context.getTameableAnimals()) {
         if (!this.progressByEntityId.containsKey(animal.entityId())) {
            Optional<TamingConfig> configOpt = this.configRegistry.get(animal.speciesId());
            if (!configOpt.isEmpty()) {
               TamingConfig config = (TamingConfig)configOpt.get();
               this.checkForSneakingPlayers(context, animal, config, currentTick);
            }
         }
      }

   }

   private void checkForSneakingPlayers(SystemContext context, SystemContext.TameableAnimalInfo animal, TamingConfig config, long currentTick) {
      double animalX = context.getEntityX(animal.entityId());
      double animalY = context.getEntityY(animal.entityId());
      double animalZ = context.getEntityZ(animal.entityId());

      for(long playerId : context.getPlayersInRadius(animalX, animalY, animalZ, config.calmingDistance())) {
         if (context.isPlayerSneaking(playerId)) {
            Optional<UUID> playerUuidOpt = this.playerLookup.getPlayerUUID(playerId);
            if (!playerUuidOpt.isEmpty()) {
               UUID playerUuid = (UUID)playerUuidOpt.get();
               TamingProgress progress = TamingProgress.startCalming(animal.animalId(), animal.speciesId(), playerUuid, currentTick);
               this.progressByEntityId.put(animal.entityId(), progress);
               this.eventBus.publish(new TamingEvents.TamingStartedEvent(playerId, playerUuid, animal.entityId(), animal.animalId(), animal.speciesId()));
               context.sendMessage(playerId, "You begin calming the " + config.speciesId() + "...");
               break;
            }
         }
      }

   }

   private void cleanupExpiredCalm(SystemContext context, long currentTick) {
      Iterator<Map.Entry<Long, TamingProgress>> iter = this.progressByEntityId.entrySet().iterator();

      while(iter.hasNext()) {
         Map.Entry<Long, TamingProgress> entry = (Map.Entry)iter.next();
         TamingProgress progress = (TamingProgress)entry.getValue();
         if (progress.state() == TamingState.CALMED && progress.isCalmExpired(currentTick)) {
            this.eventBus.publish(new TamingEvents.CalmExpiredEvent((Long)entry.getKey(), progress.animalId(), progress.speciesId()));
            this.eventBus.publish(new TamingEvents.TamingStateChangedEvent((Long)entry.getKey(), progress.animalId(), TamingState.CALMED, TamingState.WILD));
            iter.remove();
         }
      }

   }

   public Optional<TamingProgress> getProgress(long entityId) {
      return Optional.ofNullable((TamingProgress)this.progressByEntityId.get(entityId));
   }

   public void setProgress(long entityId, TamingProgress progress) {
      if (progress == null) {
         this.progressByEntityId.remove(entityId);
      } else {
         this.progressByEntityId.put(entityId, progress);
      }

   }

   public void removeProgress(long entityId) {
      this.progressByEntityId.remove(entityId);
   }

   public boolean hasProgress(long entityId) {
      return this.progressByEntityId.containsKey(entityId);
   }
}
