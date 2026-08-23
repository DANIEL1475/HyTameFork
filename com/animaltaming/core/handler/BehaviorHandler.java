package com.animaltaming.core.handler;

import com.animaltaming.api.event.TamingEvents;
import com.animaltaming.api.model.BehaviorMode;
import com.animaltaming.api.model.TamedAnimal;
import com.animaltaming.core.registry.TamedAnimalRegistry;
import com.animaltaming.core.service.PlayerLookupService;
import com.animaltaming.system.SystemContext;
import com.animaltaming.util.EventBus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class BehaviorHandler {
   private static final double FOLLOW_ACTIVATION_DISTANCE = (double)6.0F;
   private static final double FOLLOW_TARGET_DISTANCE = (double)3.0F;
   private static final double STAY_WANDER_RADIUS = (double)5.0F;
   private final PlayerLookupService playerLookup;
   private final TamedAnimalRegistry animalRegistry;
   private final EventBus eventBus;

   public BehaviorHandler(PlayerLookupService playerLookup, TamedAnimalRegistry animalRegistry, EventBus eventBus) {
      this.playerLookup = (PlayerLookupService)Objects.requireNonNull(playerLookup, "playerLookup required");
      this.animalRegistry = (TamedAnimalRegistry)Objects.requireNonNull(animalRegistry, "animalRegistry required");
      this.eventBus = (EventBus)Objects.requireNonNull(eventBus, "eventBus required");
   }

   public void process(SystemContext context) {
      for(TamedAnimal animal : this.animalRegistry.getAll()) {
         this.processAnimalBehavior(context, animal);
      }

   }

   private void processAnimalBehavior(SystemContext context, TamedAnimal animal) {
      switch (animal.mode()) {
         case FOLLOW -> this.processFollowBehavior(context, animal);
         case STAY -> this.processStayBehavior(context, animal);
      }

   }

   private void processFollowBehavior(SystemContext context, TamedAnimal animal) {
      Optional<Long> ownerEntityOpt = this.playerLookup.getEntityId(animal.ownerId());
      if (!ownerEntityOpt.isEmpty()) {
         long ownerEntityId = (Long)ownerEntityOpt.get();
         Optional<Long> animalEntityOpt = context.getEntityIdForAnimal(animal.id());
         if (!animalEntityOpt.isEmpty()) {
            long animalEntityId = (Long)animalEntityOpt.get();
            double animalX = context.getEntityX(animalEntityId);
            double animalY = context.getEntityY(animalEntityId);
            double animalZ = context.getEntityZ(animalEntityId);
            double ownerX = context.getEntityX(ownerEntityId);
            double ownerY = context.getEntityY(ownerEntityId);
            double ownerZ = context.getEntityZ(ownerEntityId);
            double distance = Math.sqrt(Math.pow(animalX - ownerX, (double)2.0F) + Math.pow(animalY - ownerY, (double)2.0F) + Math.pow(animalZ - ownerZ, (double)2.0F));
            if (distance > animal.maxFollowDistance()) {
               double teleX = ownerX + (Math.random() - (double)0.5F) * (double)4.0F;
               double teleZ = ownerZ + (Math.random() - (double)0.5F) * (double)4.0F;
               context.teleport(animalEntityId, teleX, ownerY, teleZ);
               this.eventBus.publish(new TamingEvents.AnimalTeleportedEvent(animalEntityId, animal.id(), animal.ownerId(), animalX, animalY, animalZ, teleX, ownerY, teleZ));
               context.spawnParticle(teleX, ownerY + (double)0.5F, teleZ, "portal");
            } else {
               if (distance > (double)3.0F && distance <= (double)6.0F) {
                  double followSpeed = (double)4.0F;
                  context.moveEntityToward(animalEntityId, ownerX, ownerY, ownerZ, followSpeed);
               }

            }
         }
      }
   }

   private void processStayBehavior(SystemContext context, TamedAnimal animal) {
      Optional<Long> animalEntityOpt = context.getEntityIdForAnimal(animal.id());
      if (!animalEntityOpt.isEmpty()) {
         long animalEntityId = (Long)animalEntityOpt.get();
         List<Long> riders = context.getRiders(animalEntityId);
         if (riders.isEmpty()) {
            double animalX = context.getEntityX(animalEntityId);
            double animalY = context.getEntityY(animalEntityId);
            double animalZ = context.getEntityZ(animalEntityId);
            double dx = animalX - animal.homeX();
            double dy = animalY - animal.homeY();
            double dz = animalZ - animal.homeZ();
            double distanceFromHome = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distanceFromHome > (double)0.5F) {
               context.teleport(animalEntityId, animal.homeX(), animal.homeY(), animal.homeZ());
            }

         }
      }
   }

   public boolean toggleMode(SystemContext context, UUID animalId, UUID playerId, double x, double y, double z) {
      Optional<TamedAnimal> animalOpt = this.animalRegistry.getByAnimalId(animalId);
      if (animalOpt.isEmpty()) {
         return false;
      } else {
         TamedAnimal animal = (TamedAnimal)animalOpt.get();
         if (!animal.isOwnedBy(playerId)) {
            Optional<Long> playerEntityOpt = this.playerLookup.getEntityId(playerId);
            if (playerEntityOpt.isPresent()) {
               context.sendMessage((Long)playerEntityOpt.get(), "This is not your pet!");
            }

            return false;
         } else {
            BehaviorMode oldMode = animal.mode();
            BehaviorMode newMode = oldMode.toggle();
            TamedAnimal updated;
            if (newMode == BehaviorMode.STAY) {
               updated = animal.withToggledMode().withHome(x, y, z);
            } else {
               updated = animal.withToggledMode();
            }

            this.animalRegistry.update(updated);
            this.eventBus.publish(new TamingEvents.BehaviorModeChangedEvent(0L, animalId, playerId, oldMode, newMode));
            Optional<Long> playerEntityOpt = this.playerLookup.getEntityId(playerId);
            if (playerEntityOpt.isPresent()) {
               long var10001 = (Long)playerEntityOpt.get();
               String var10002 = newMode.getDisplayName().toLowerCase();
               context.sendMessage(var10001, "Pet is now " + var10002 + ".");
               context.spawnParticle(x, y + (double)1.0F, z, newMode == BehaviorMode.FOLLOW ? "note" : "smoke");
            }

            return true;
         }
      }
   }

   public void handlePetInteraction(SystemContext context, long playerEntityId, long animalEntityId) {
      Optional<UUID> playerUuidOpt = this.playerLookup.getPlayerUUID(playerEntityId);
      if (!playerUuidOpt.isEmpty()) {
         Optional<TamedAnimal> animalOpt = this.animalRegistry.getByEntityId(animalEntityId);
         if (!animalOpt.isEmpty()) {
            TamedAnimal animal = (TamedAnimal)animalOpt.get();
            double x = context.getEntityX(animalEntityId);
            double y = context.getEntityY(animalEntityId);
            double z = context.getEntityZ(animalEntityId);
            context.spawnParticle(x, y + (double)1.0F, z, "heart");
            context.playSound(x, y, z, "purr");
            this.eventBus.publish(new TamingEvents.AnimalPettedEvent(playerEntityId, (UUID)playerUuidOpt.get(), animalEntityId, animal.id(), animal.speciesId()));
         }
      }
   }
}
