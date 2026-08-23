package com.animaltaming.api.model;

public enum BehaviorMode {
   FOLLOW("Following"),
   STAY("Staying");

   private final String displayName;

   private BehaviorMode(String displayName) {
      this.displayName = displayName;
   }

   public BehaviorMode toggle() {
      return this == FOLLOW ? STAY : FOLLOW;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   // $FF: synthetic method
   private static BehaviorMode[] $values() {
      return new BehaviorMode[]{FOLLOW, STAY};
   }
}
