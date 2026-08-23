package com.animaltaming.core.handler;

import com.animaltaming.api.event.TamingEvents;
import com.animaltaming.api.model.TamingConfig;
import com.animaltaming.api.model.TamingProgress;
import com.animaltaming.api.model.TamingState;
import com.animaltaming.core.registry.TamingConfigRegistry;
import com.animaltaming.core.service.PlayerLookupService;
import com.animaltaming.system.SystemContext;
import com.animaltaming.util.EventBus;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class FeedingHandler {
   private final PlayerLookupService playerLookup;
   private final TamingConfigRegistry configRegistry;
   private final EventBus eventBus;
   private final CalmingHandler calmingHandler;

   public FeedingHandler(PlayerLookupService playerLookup, TamingConfigRegistry configRegistry, EventBus eventBus, CalmingHandler calmingHandler) {
      this.playerLookup = (PlayerLookupService)Objects.requireNonNull(playerLookup, "playerLookup required");
      this.configRegistry = (TamingConfigRegistry)Objects.requireNonNull(configRegistry, "configRegistry required");
      this.eventBus = (EventBus)Objects.requireNonNull(eventBus, "eventBus required");
      this.calmingHandler = (CalmingHandler)Objects.requireNonNull(calmingHandler, "calmingHandler required");
   }

   public void process(SystemContext context, long currentTick) {
      for(SystemContext.InteractionEvent interaction : context.getPendingInteractions()) {
         this.processInteraction(context, interaction, currentTick);
      }

   }

   private void processInteraction(SystemContext context, SystemContext.InteractionEvent interaction, long currentTick) {
      long playerId = interaction.playerEntityId();
      long targetId = interaction.targetEntityId();
      Optional<TamingProgress> progressOpt = this.calmingHandler.getProgress(targetId);
      if (!progressOpt.isEmpty()) {
         TamingProgress progress = (TamingProgress)progressOpt.get();
         TamingState state = progress.state();
         if (state == TamingState.CALMED || state == TamingState.BONDING_FEED) {
            Optional<UUID> playerUuidOpt = this.playerLookup.getPlayerUUID(playerId);
            if (!playerUuidOpt.isEmpty() && ((UUID)playerUuidOpt.get()).equals(progress.attemptingPlayerId())) {
               Optional<String> heldItemOpt = context.getHeldItemId(playerId);
               if (!heldItemOpt.isEmpty()) {
                  String heldItem = (String)heldItemOpt.get();
                  Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
                  if (!configOpt.isEmpty()) {
                     TamingConfig config = (TamingConfig)configOpt.get();
                     if (!config.acceptsFood(heldItem)) {
                        context.sendMessage(playerId, "The " + config.speciesId() + " doesn't want that food.");
                     } else if (context.consumeHeldItem(playerId)) {
                        int oldTrust = progress.trustLevel();
                        int trustGain = config.trustPerFeed();
                        TamingProgress updated;
                        if (state == TamingState.CALMED) {
                           updated = progress.withBondingFeed().withTrustGain(trustGain, currentTick);
                        } else {
                           updated = progress.withTrustGain(trustGain, currentTick);
                        }

                        this.calmingHandler.setProgress(targetId, updated);
                        this.eventBus.publish(new TamingEvents.TrustChangedEvent(targetId, progress.animalId(), progress.speciesId(), oldTrust, updated.trustLevel(), "feeding"));
                        double x = context.getEntityX(targetId);
                        double y = context.getEntityY(targetId);
                        double z = context.getEntityZ(targetId);
                        context.spawnParticle(x, y + (double)1.0F, z, "heart");
                        context.playSound(x, y, z, "eat");
                        int remaining = config.requiredTrustLevel() - updated.trustLevel();
                        if (remaining > 0) {
                           int var10002 = updated.trustLevel();
                           context.sendMessage(playerId, "Trust: " + var10002 + "/" + config.requiredTrustLevel());
                        }

                     }
                  }
               }
            } else {
               context.sendMessage(playerId, "This animal is being calmed by another player!");
            }
         }
      }
   }

   public Optional<TamingProgress> feed(SystemContext context, long animalEntityId, UUID playerId, String foodId, long currentTick) {
      Optional<TamingProgress> progressOpt = this.calmingHandler.getProgress(animalEntityId);
      if (progressOpt.isEmpty()) {
         return Optional.empty();
      } else {
         TamingProgress progress = (TamingProgress)progressOpt.get();
         if (!playerId.equals(progress.attemptingPlayerId())) {
            return Optional.empty();
         } else {
            TamingState state = progress.state();
            if (state != TamingState.CALMED && state != TamingState.BONDING_FEED) {
               return Optional.empty();
            } else {
               Optional<TamingConfig> configOpt = this.configRegistry.get(progress.speciesId());
               if (!configOpt.isEmpty() && ((TamingConfig)configOpt.get()).acceptsFood(foodId)) {
                  TamingConfig config = (TamingConfig)configOpt.get();
                  int oldTrust = progress.trustLevel();
                  TamingProgress updated;
                  if (state == TamingState.CALMED) {
                     updated = progress.withBondingFeed().withTrustGain(config.trustPerFeed(), currentTick);
                  } else {
                     updated = progress.withTrustGain(config.trustPerFeed(), currentTick);
                  }

                  this.calmingHandler.setProgress(animalEntityId, updated);
                  this.eventBus.publish(new TamingEvents.TrustChangedEvent(animalEntityId, progress.animalId(), progress.speciesId(), oldTrust, updated.trustLevel(), "feeding"));
                  return Optional.of(updated);
               } else {
                  return Optional.empty();
               }
            }
         }
      }
   }
}
