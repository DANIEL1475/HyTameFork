package com.animaltaming.api.model;

public enum TamingState {
   WILD,
   CALMING,
   CALMED,
   BONDING_FEED,
   BONDING_MOUNT,
   TAMED;

   // $FF: synthetic method
   private static TamingState[] $values() {
      return new TamingState[]{WILD, CALMING, CALMED, BONDING_FEED, BONDING_MOUNT, TAMED};
   }
}
