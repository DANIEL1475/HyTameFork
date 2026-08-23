package com.hytame.models;

public enum GrowthStage {
   BABY(0.5F, 0),
   JUVENILE(0.75F, 1),
   ADULT(1.0F, 2);

   private final float sizeMultiplier;
   private final int stageIndex;

   private GrowthStage(float sizeMultiplier, int stageIndex) {
      this.sizeMultiplier = sizeMultiplier;
      this.stageIndex = stageIndex;
   }

   public float getSizeMultiplier() {
      return this.sizeMultiplier;
   }

   public int getStageIndex() {
      return this.stageIndex;
   }

   public boolean canBreed() {
      return this == ADULT;
   }

   public GrowthStage getNextStage() {
      GrowthStage var10000;
      switch (this.ordinal()) {
         case 0 -> var10000 = JUVENILE;
         case 1 -> var10000 = ADULT;
         case 2 -> var10000 = ADULT;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public boolean hasNextStage() {
      return this != ADULT;
   }

   public String getDisplayName() {
      char var10000 = this.name().charAt(0);
      return var10000 + this.name().substring(1).toLowerCase();
   }

   // $FF: synthetic method
   private static GrowthStage[] $values() {
      return new GrowthStage[]{BABY, JUVENILE, ADULT};
   }
}
