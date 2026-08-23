package com.animaltaming.api.model;

import java.util.List;
import java.util.Objects;

public record TamingConfig(String speciesId, DietType dietType, List<String> preferredFoods, boolean canBeMounted, double calmingDistance, int calmingTimeTicks, int calmDurationTicks, int trustPerFeed, int trustPerMountSecond, int requiredTrustLevel, double maxFollowDistance) {
   public TamingConfig(String speciesId, DietType dietType, List<String> preferredFoods, boolean canBeMounted, double calmingDistance, int calmingTimeTicks, int calmDurationTicks, int trustPerFeed, int trustPerMountSecond, int requiredTrustLevel, double maxFollowDistance) {
      Objects.requireNonNull(speciesId, "speciesId is required");
      Objects.requireNonNull(dietType, "dietType is required");
      Objects.requireNonNull(preferredFoods, "preferredFoods is required");
      if (speciesId.isBlank()) {
         throw new IllegalArgumentException("speciesId cannot be blank");
      } else if (preferredFoods.isEmpty()) {
         throw new IllegalArgumentException("preferredFoods cannot be empty");
      } else if (calmingDistance <= (double)0.0F) {
         throw new IllegalArgumentException("calmingDistance must be positive");
      } else if (calmingTimeTicks <= 0) {
         throw new IllegalArgumentException("calmingTimeTicks must be positive");
      } else if (calmDurationTicks <= 0) {
         throw new IllegalArgumentException("calmDurationTicks must be positive");
      } else if (trustPerFeed < 0) {
         throw new IllegalArgumentException("trustPerFeed cannot be negative");
      } else if (trustPerMountSecond < 0) {
         throw new IllegalArgumentException("trustPerMountSecond cannot be negative");
      } else if (requiredTrustLevel <= 0) {
         throw new IllegalArgumentException("requiredTrustLevel must be positive");
      } else if (maxFollowDistance <= (double)0.0F) {
         throw new IllegalArgumentException("maxFollowDistance must be positive");
      } else if (canBeMounted && trustPerMountSecond <= 0) {
         throw new IllegalArgumentException("mountable species must have positive trustPerMountSecond");
      } else if (!canBeMounted && trustPerMountSecond > 0) {
         throw new IllegalArgumentException("non-mountable species cannot have trustPerMountSecond");
      } else {
         preferredFoods = List.copyOf(preferredFoods);
         this.speciesId = speciesId;
         this.dietType = dietType;
         this.preferredFoods = preferredFoods;
         this.canBeMounted = canBeMounted;
         this.calmingDistance = calmingDistance;
         this.calmingTimeTicks = calmingTimeTicks;
         this.calmDurationTicks = calmDurationTicks;
         this.trustPerFeed = trustPerFeed;
         this.trustPerMountSecond = trustPerMountSecond;
         this.requiredTrustLevel = requiredTrustLevel;
         this.maxFollowDistance = maxFollowDistance;
      }
   }

   public boolean acceptsFood(String foodId) {
      return this.preferredFoods.contains(foodId);
   }
}
