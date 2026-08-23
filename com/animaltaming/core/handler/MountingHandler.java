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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MountingHandler {
   private final PlayerLookupService playerLookup;
   private final TamingConfigRegistry configRegistry;
   private final EventBus eventBus;
   private final CalmingHandler calmingHandler;
   private final Map<Long, Integer> lastProcessedSecond = new HashMap();

   public MountingHandler(PlayerLookupService playerLookup, TamingConfigRegistry configRegistry, EventBus eventBus, CalmingHandler calmingHandler) {
      this.playerLookup = (PlayerLookupService)Objects.requireNonNull(playerLookup, "playerLookup required");
      this.configRegistry = (TamingConfigRegistry)Objects.requireNonNull(configRegistry, "configRegistry required");
      this.eventBus = (EventBus)Objects.requireNonNull(eventBus, "eventBus required");
      this.calmingHandler = (CalmingHandler)Objects.requireNonNull(calmingHandler, "calmingHandler required");
   }

   public void process(SystemContext context, long currentTick) {
      int tickRate = context.getTickRate();

      for(SystemContext.TameableAnimalInfo animal : context.getTameableAnimals()) {
         Optional<TamingProgress> progressOpt = this.calmingHandler.getProgress(animal.entityId());
         if (!progressOpt.isEmpty()) {
            TamingProgress progress = (TamingProgress)progressOpt.get();
            this.processMountProgress(context, animal.entityId(), progress, currentTick, tickRate);
         }
      }

   }

   private void processMountProgress(SystemContext context, long entityId, TamingProgress progress, long currentTick, int tickRate) {
      TamingState state = progress.state();
      if (state == TamingState.CALMED) {
         this.checkForNewMount(context, entityId, progress, currentTick);
      } else {
         if (state == TamingState.BONDING_MOUNT) {
            this.processBondingMount(context, entityId, progress, currentTick, tickRate);
         }

      }
   }

   private void checkForNewMount(SystemContext context, long entityId, TamingProgress progress, long currentTick) {
      Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
      if (!configOpt.isEmpty() && ((TamingConfig)configOpt.get()).canBeMounted()) {
         List<Long> riders = context.getRiders(entityId);
         if (!riders.isEmpty()) {
            for(long riderId : riders) {
               Optional<UUID> riderUuidOpt = this.playerLookup.getPlayerUUID(riderId);
               if (!riderUuidOpt.isEmpty() && ((UUID)riderUuidOpt.get()).equals(progress.attemptingPlayerId())) {
                  TamingProgress updated = progress.withBondingMount(currentTick);
                  this.calmingHandler.setProgress(entityId, updated);
                  this.lastProcessedSecond.put(entityId, 0);
                  this.eventBus.publish(new TamingEvents.TamingStateChangedEvent(entityId, progress.animalId(), TamingState.CALMED, TamingState.BONDING_MOUNT));
                  context.sendMessage(riderId, "You begin bonding with the " + progress.speciesId() + " while riding...");
                  return;
               }
            }

         }
      }
   }

   private void processBondingMount(SystemContext context, long entityId, TamingProgress progress, long currentTick, int tickRate) {
      List<Long> riders = context.getRiders(entityId);
      boolean validRiderFound = false;

      for(long riderId : riders) {
         Optional<UUID> riderUuidOpt = this.playerLookup.getPlayerUUID(riderId);
         if (riderUuidOpt.isPresent() && ((UUID)riderUuidOpt.get()).equals(progress.attemptingPlayerId())) {
            validRiderFound = true;
            break;
         }
      }

      if (!validRiderFound) {
         TamingProgress updated = progress.withMountReset();
         this.calmingHandler.setProgress(entityId, updated);
         this.lastProcessedSecond.remove(entityId);
         this.eventBus.publish(new TamingEvents.TamingStateChangedEvent(entityId, progress.animalId(), TamingState.BONDING_MOUNT, TamingState.CALMED));
      } else {
         double secondsRidden = progress.getMountDurationSeconds(currentTick, tickRate);
         int wholeSeconds = (int)secondsRidden;
         int lastSecond = (Integer)this.lastProcessedSecond.getOrDefault(entityId, 0);
         if (wholeSeconds > lastSecond) {
            int newSeconds = wholeSeconds - lastSecond;
            Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
            if (configOpt.isEmpty()) {
               return;
            }

            TamingConfig config = (TamingConfig)configOpt.get();
            int trustGain = newSeconds * config.trustPerMountSecond();
            if (trustGain > 0) {
               int oldTrust = progress.trustLevel();
               TamingProgress updated = progress.withTrustGain(trustGain, currentTick);
               this.calmingHandler.setProgress(entityId, updated);
               this.lastProcessedSecond.put(entityId, wholeSeconds);
               this.eventBus.publish(new TamingEvents.TrustChangedEvent(entityId, progress.animalId(), progress.speciesId(), oldTrust, updated.trustLevel(), "mounting"));
               if (wholeSeconds % 5 == 0) {
                  Optional<Long> playerIdOpt = this.playerLookup.getEntityId(progress.attemptingPlayerId());
                  if (playerIdOpt.isPresent()) {
                     int remaining = config.requiredTrustLevel() - updated.trustLevel();
                     if (remaining > 0) {
                        long var10001 = (Long)playerIdOpt.get();
                        int var10002 = updated.trustLevel();
                        context.sendMessage(var10001, "Trust: " + var10002 + "/" + config.requiredTrustLevel());
                     }
                  }
               }
            }
         }

      }
   }

   public void cleanupEntity(long entityId) {
      this.lastProcessedSecond.remove(entityId);
   }
}
