package com.animaltaming.api.model;

import java.util.Objects;
import java.util.UUID;

public record TamingProgress(UUID animalId, String speciesId, UUID attemptingPlayerId, TamingState state, long calmingStartTick, long calmExpirationTick, int trustLevel, long lastTrustGainTick, long mountStartTick) {
   public TamingProgress {
      Objects.requireNonNull(animalId, "animalId is required");
      Objects.requireNonNull(speciesId, "speciesId is required");
      Objects.requireNonNull(state, "state is required");
   }

   public static TamingProgress startCalming(UUID animalId, String speciesId, UUID playerId, long currentTick) {
      return new TamingProgress(animalId, speciesId, playerId, TamingState.CALMING, currentTick, 0L, 0, 0L, 0L);
   }

   public TamingProgress withCalmed(long currentTick, int calmDurationTicks) {
      return new TamingProgress(this.animalId, this.speciesId, this.attemptingPlayerId, TamingState.CALMED, this.calmingStartTick, currentTick + (long)calmDurationTicks, this.trustLevel, this.lastTrustGainTick, 0L);
   }

   public TamingProgress withBondingFeed() {
      return new TamingProgress(this.animalId, this.speciesId, this.attemptingPlayerId, TamingState.BONDING_FEED, this.calmingStartTick, this.calmExpirationTick, this.trustLevel, this.lastTrustGainTick, 0L);
   }

   public TamingProgress withBondingMount(long currentTick) {
      return new TamingProgress(this.animalId, this.speciesId, this.attemptingPlayerId, TamingState.BONDING_MOUNT, this.calmingStartTick, this.calmExpirationTick, this.trustLevel, this.lastTrustGainTick, currentTick);
   }

   public TamingProgress withTrustGain(int amount, long currentTick) {
      return new TamingProgress(this.animalId, this.speciesId, this.attemptingPlayerId, this.state, this.calmingStartTick, this.calmExpirationTick, this.trustLevel + amount, currentTick, this.mountStartTick);
   }

   public TamingProgress withTrustLevel(int newTrustLevel, long currentTick) {
      return new TamingProgress(this.animalId, this.speciesId, this.attemptingPlayerId, this.state, this.calmingStartTick, this.calmExpirationTick, newTrustLevel, currentTick, this.mountStartTick);
   }

   public TamingProgress withMountReset() {
      return new TamingProgress(this.animalId, this.speciesId, this.attemptingPlayerId, this.state == TamingState.BONDING_MOUNT ? TamingState.CALMED : this.state, this.calmingStartTick, this.calmExpirationTick, this.trustLevel, this.lastTrustGainTick, 0L);
   }

   public boolean isCalmExpired(long currentTick) {
      return this.calmExpirationTick > 0L && currentTick >= this.calmExpirationTick;
   }

   public double getMountDurationSeconds(long currentTick, int tickRate) {
      if (this.mountStartTick <= 0L) {
         return (double)0.0F;
      } else {
         long ticksRiding = currentTick - this.mountStartTick;
         return (double)ticksRiding / (double)tickRate;
      }
   }
}
