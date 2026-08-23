package com.animaltaming.api.model;

import java.util.Objects;
import java.util.UUID;

public record TamedAnimal(UUID id, UUID ownerId, String ownerName, String speciesId, BehaviorMode mode, double homeX, double homeY, double homeZ, double maxFollowDistance, long tamedTimestamp, String customName) {
   public TamedAnimal {
      Objects.requireNonNull(id, "id is required");
      Objects.requireNonNull(ownerId, "ownerId is required");
      Objects.requireNonNull(ownerName, "ownerName is required");
      Objects.requireNonNull(speciesId, "speciesId is required");
      Objects.requireNonNull(mode, "mode is required");
      if (maxFollowDistance <= (double)0.0F) {
         throw new IllegalArgumentException("maxFollowDistance must be positive");
      }
   }

   public static TamedAnimal create(UUID id, UUID ownerId, String ownerName, String speciesId, double x, double y, double z, double maxFollowDistance) {
      return new TamedAnimal(id, ownerId, ownerName, speciesId, BehaviorMode.FOLLOW, x, y, z, maxFollowDistance, System.currentTimeMillis(), (String)null);
   }

   public TamedAnimal withMode(BehaviorMode newMode) {
      return new TamedAnimal(this.id, this.ownerId, this.ownerName, this.speciesId, newMode, this.homeX, this.homeY, this.homeZ, this.maxFollowDistance, this.tamedTimestamp, this.customName);
   }

   public TamedAnimal withToggledMode() {
      return this.withMode(this.mode.toggle());
   }

   public TamedAnimal withHome(double x, double y, double z) {
      return new TamedAnimal(this.id, this.ownerId, this.ownerName, this.speciesId, this.mode, x, y, z, this.maxFollowDistance, this.tamedTimestamp, this.customName);
   }

   public TamedAnimal withCustomName(String name) {
      return new TamedAnimal(this.id, this.ownerId, this.ownerName, this.speciesId, this.mode, this.homeX, this.homeY, this.homeZ, this.maxFollowDistance, this.tamedTimestamp, name);
   }

   public boolean isOwnedBy(UUID playerId) {
      return this.ownerId.equals(playerId);
   }

   public String getDisplayName() {
      return this.customName != null ? this.customName : this.speciesId;
   }

   public boolean isFollowing() {
      return this.mode == BehaviorMode.FOLLOW;
   }

   public boolean isStaying() {
      return this.mode == BehaviorMode.STAY;
   }

   public double[] getHomePosition() {
      return new double[]{this.homeX, this.homeY, this.homeZ};
   }

   public long getTamedDuration() {
      return System.currentTimeMillis() - this.tamedTimestamp;
   }
}
