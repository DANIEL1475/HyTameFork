package com.animaltaming.system;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemContext {
   long getCurrentTick();

   int getTickRate();

   boolean entityExists(long var1);

   double getEntityX(long var1);

   double getEntityY(long var1);

   double getEntityZ(long var1);

   default double getDistance(long entityA, long entityB) {
      double dx = this.getEntityX(entityA) - this.getEntityX(entityB);
      double dy = this.getEntityY(entityA) - this.getEntityY(entityB);
      double dz = this.getEntityZ(entityA) - this.getEntityZ(entityB);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   List<PlayerInfo> getAllPlayers();

   List<Long> getPlayersInRadius(double var1, double var3, double var5, double var7);

   default boolean isPlayerSneaking(long playerId) {
      return false;
   }

   default Optional<String> getHeldItemId(long playerId) {
      return Optional.empty();
   }

   default boolean consumeHeldItem(long playerId) {
      return false;
   }

   default List<Long> getRiders(long entityId) {
      return List.of();
   }

   List<TameableAnimalInfo> getTameableAnimals();

   Optional<Long> getEntityIdForAnimal(UUID var1);

   List<InteractionEvent> getPendingInteractions();

   void teleport(long var1, double var3, double var5, double var7);

   default void moveEntityToward(long entityId, double targetX, double targetY, double targetZ, double speed) {
   }

   void spawnParticle(double var1, double var3, double var5, String var7);

   void playSound(double var1, double var3, double var5, String var7);

   void sendMessage(long var1, String var3);

   public static record PlayerInfo(long entityId, UUID uuid, String name) {
   }

   public static record TameableAnimalInfo(long entityId, UUID animalId, String speciesId) {
   }

   public static record InteractionEvent(long playerEntityId, long targetEntityId, String type) {
   }
}
