package com.hytame.models;

public class OriginalInteractionState {
   private final String interactionId;
   private final String hint;

   public OriginalInteractionState(String interactionId, String hint) {
      this.interactionId = interactionId;
      this.hint = hint;
   }

   public String getInteractionId() {
      return this.interactionId;
   }

   public String getHint() {
      return this.hint;
   }

   public boolean hasInteraction() {
      return this.interactionId != null && !this.interactionId.isEmpty();
   }

   public boolean hasHint() {
      return this.hint != null && !this.hint.isEmpty();
   }

   public String toString() {
      return "OriginalInteractionState{interactionId='" + this.interactionId + "', hint='" + this.hint + "'}";
   }
}
