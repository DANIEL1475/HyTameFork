package com.animaltaming.system;

import com.animaltaming.api.model.TamingProgress;
import com.animaltaming.core.handler.BehaviorHandler;
import com.animaltaming.core.handler.CalmingHandler;
import com.animaltaming.core.handler.FeedingHandler;
import com.animaltaming.core.handler.MountingHandler;
import com.animaltaming.core.service.DefaultTamingService;
import com.animaltaming.core.service.PlayerLookupService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TamingTickSystem implements GameSystem {
   private final PlayerLookupService playerLookup;
   private final CalmingHandler calmingHandler;
   private final FeedingHandler feedingHandler;
   private final MountingHandler mountingHandler;
   private final BehaviorHandler behaviorHandler;
   private final DefaultTamingService tamingService;
   private boolean enabled = true;

   public TamingTickSystem(PlayerLookupService playerLookup, CalmingHandler calmingHandler, FeedingHandler feedingHandler, MountingHandler mountingHandler, BehaviorHandler behaviorHandler, DefaultTamingService tamingService) {
      this.playerLookup = (PlayerLookupService)Objects.requireNonNull(playerLookup, "playerLookup required");
      this.calmingHandler = (CalmingHandler)Objects.requireNonNull(calmingHandler, "calmingHandler required");
      this.feedingHandler = (FeedingHandler)Objects.requireNonNull(feedingHandler, "feedingHandler required");
      this.mountingHandler = (MountingHandler)Objects.requireNonNull(mountingHandler, "mountingHandler required");
      this.behaviorHandler = (BehaviorHandler)Objects.requireNonNull(behaviorHandler, "behaviorHandler required");
      this.tamingService = (DefaultTamingService)Objects.requireNonNull(tamingService, "tamingService required");
   }

   public void update(SystemContext context, float deltaTime) {
      long currentTick = context.getCurrentTick();
      this.playerLookup.refreshCache(context);
      this.tamingService.setContext(context);
      this.calmingHandler.process(context, currentTick);
      this.feedingHandler.process(context, currentTick);
      this.mountingHandler.process(context, currentTick);
      this.checkTamingCompletion(context, currentTick);
      this.behaviorHandler.process(context);
   }

   private void checkTamingCompletion(SystemContext context, long currentTick) {
      for(SystemContext.TameableAnimalInfo animal : context.getTameableAnimals()) {
         if (this.tamingService.canCompleteTaming(animal.entityId())) {
            Optional<TamingProgress> progressOpt = this.tamingService.getTamingProgress(animal.entityId());
            if (!progressOpt.isEmpty()) {
               TamingProgress progress = (TamingProgress)progressOpt.get();
               Optional<Long> playerEntityOpt = this.playerLookup.getEntityId(progress.attemptingPlayerId());
               if (!playerEntityOpt.isEmpty()) {
                  String ownerName = this.getPlayerName(context, progress.attemptingPlayerId());
                  double x = context.getEntityX(animal.entityId());
                  double y = context.getEntityY(animal.entityId());
                  double z = context.getEntityZ(animal.entityId());
                  this.tamingService.completeTaming(animal.entityId(), ownerName, x, y, z);
                  context.sendMessage((Long)playerEntityOpt.get(), "Congratulations! You have tamed the " + animal.speciesId() + "!");
                  context.spawnParticle(x, y + (double)1.0F, z, "firework");
               }
            }
         }
      }

   }

   private String getPlayerName(SystemContext context, UUID playerId) {
      for(SystemContext.PlayerInfo player : context.getAllPlayers()) {
         if (player.uuid().equals(playerId)) {
            return player.name();
         }
      }

      return "Unknown";
   }

   public int priority() {
      return 10;
   }

   public String getName() {
      return "TamingTickSystem";
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
