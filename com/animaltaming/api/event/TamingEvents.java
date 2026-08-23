package com.animaltaming.api.event;

import com.animaltaming.api.model.BehaviorMode;
import com.animaltaming.api.model.TamingState;
import java.util.UUID;

public final class TamingEvents {
   private TamingEvents() {
   }

   public static record TamingStartedEvent(long playerEntityId, UUID playerId, long animalEntityId, UUID animalId, String speciesId) {
   }

   public static record AnimalCalmedEvent(long playerEntityId, UUID playerId, long animalEntityId, UUID animalId, String speciesId) {
   }

   public static record CalmExpiredEvent(long animalEntityId, UUID animalId, String speciesId) {
   }

   public static record TrustChangedEvent(long animalEntityId, UUID animalId, String speciesId, int oldTrust, int newTrust, String reason) {
   }

   public static record TamingStateChangedEvent(long animalEntityId, UUID animalId, TamingState oldState, TamingState newState) {
   }

   public static record AnimalTamedEvent(UUID playerId, String ownerName, long animalEntityId, UUID animalId, String speciesId) {
   }

   public static record BehaviorModeChangedEvent(long animalEntityId, UUID animalId, UUID ownerId, BehaviorMode oldMode, BehaviorMode newMode) {
   }

   public static record AnimalTeleportedEvent(long animalEntityId, UUID animalId, UUID ownerId, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
   }

   public static record TamedAnimalLostEvent(UUID animalId, UUID ownerId, String speciesId, String reason) {
   }

   public static record AnimalPettedEvent(long playerEntityId, UUID playerId, long animalEntityId, UUID animalId, String speciesId) {
   }
}
