package com.hytame.models;

import java.util.ArrayList;
import java.util.List;

public class CustomAnimalConfig {
   private final String modelAssetId;
   private final String displayName;
   private final List<String> breedingFoods;
   private final double growthTimeMinutes;
   private final double breedCooldownMinutes;
   private final String babyNpcRoleId;
   private final String adultNpcRoleId;
   private final boolean mountable;
   private final boolean breedingEnabled;
   private final boolean tamingEnabled;
   private final String npcRolePath;

   public CustomAnimalConfig(String modelAssetId, String displayName, List<String> breedingFoods, double growthTimeMinutes, double breedCooldownMinutes, String babyNpcRoleId, String adultNpcRoleId, boolean mountable, boolean breedingEnabled, boolean tamingEnabled, String npcRolePath) {
      this.modelAssetId = modelAssetId;
      this.displayName = displayName != null ? displayName : modelAssetId;
      this.breedingFoods = breedingFoods != null ? new ArrayList(breedingFoods) : new ArrayList();
      this.growthTimeMinutes = growthTimeMinutes > (double)0.0F ? growthTimeMinutes : (double)30.0F;
      this.breedCooldownMinutes = breedCooldownMinutes > (double)0.0F ? breedCooldownMinutes : (double)5.0F;
      this.babyNpcRoleId = babyNpcRoleId;
      this.adultNpcRoleId = adultNpcRoleId != null ? adultNpcRoleId : modelAssetId;
      this.mountable = mountable;
      this.breedingEnabled = breedingEnabled;
      this.tamingEnabled = tamingEnabled;
      this.npcRolePath = npcRolePath;
   }

   public CustomAnimalConfig(String modelAssetId, String displayName, List<String> breedingFoods, double growthTimeMinutes, double breedCooldownMinutes, String babyNpcRoleId, String adultNpcRoleId, boolean mountable, boolean enabled) {
      this(modelAssetId, displayName, breedingFoods, growthTimeMinutes, breedCooldownMinutes, babyNpcRoleId, adultNpcRoleId, mountable, enabled, enabled, (String)null);
   }

   public String getModelAssetId() {
      return this.modelAssetId;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getId() {
      return this.modelAssetId.toLowerCase();
   }

   public List<String> getBreedingFoods() {
      return new ArrayList(this.breedingFoods);
   }

   public double getGrowthTimeMinutes() {
      return this.growthTimeMinutes;
   }

   public double getBreedCooldownMinutes() {
      return this.breedCooldownMinutes;
   }

   public String getBabyNpcRoleId() {
      return this.babyNpcRoleId;
   }

   public String getAdultNpcRoleId() {
      return this.adultNpcRoleId;
   }

   public String getNpcRolePath() {
      return this.npcRolePath;
   }

   public boolean hasBabyVariant() {
      return this.babyNpcRoleId != null && !this.babyNpcRoleId.isEmpty();
   }

   public boolean isMountable() {
      return this.mountable;
   }

   public boolean isBreedingEnabled() {
      return this.breedingEnabled;
   }

   public boolean isTamingEnabled() {
      return this.tamingEnabled;
   }

   /** @deprecated */
   @Deprecated
   public boolean isEnabled() {
      return this.breedingEnabled;
   }

   public boolean isBreedingFood(String itemId) {
      if (itemId != null && !this.breedingFoods.isEmpty()) {
         for(String food : this.breedingFoods) {
            if (food.equalsIgnoreCase(itemId)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public float getBabyScale() {
      return 0.4F;
   }

   public String toString() {
      String var10000 = this.modelAssetId;
      return "CustomAnimal{" + var10000 + ", foods=" + this.breedingFoods.size() + "}";
   }
}
