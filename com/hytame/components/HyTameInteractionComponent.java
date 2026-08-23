package com.hytame.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import javax.annotation.Nullable;

public class HyTameInteractionComponent implements Component<EntityStore> {
   public static final BuilderCodec<HyTameInteractionComponent> CODEC;
   private String originalInteractionId;
   private String originalHint;
   private Boolean captured = false;

   public static ComponentType<EntityStore, HyTameInteractionComponent> getComponentType() {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      return plugin != null ? plugin.getHyTameInteractionComponentType() : null;
   }

   public String getOriginalInteractionId() {
      return this.originalInteractionId;
   }

   public void setOriginalInteractionId(String originalInteractionId) {
      this.originalInteractionId = originalInteractionId;
   }

   public String getOriginalHint() {
      return this.originalHint;
   }

   public void setOriginalHint(String originalHint) {
      this.originalHint = originalHint;
   }

   public boolean isCaptured() {
      return Boolean.TRUE.equals(this.captured);
   }

   public void setCaptured(boolean captured) {
      this.captured = captured;
   }

   public boolean hasStoredInteraction() {
      return this.isCaptured();
   }

   @Nullable
   public Component<EntityStore> clone() {
      HyTameInteractionComponent component = new HyTameInteractionComponent();
      component.originalInteractionId = this.originalInteractionId;
      component.originalHint = this.originalHint;
      component.captured = this.captured;
      return component;
   }

   static {
      CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(HyTameInteractionComponent.class, HyTameInteractionComponent::new).append(new KeyedCodec("InteractionId", Codec.STRING), (data, value) -> data.originalInteractionId = value, (data) -> data.originalInteractionId).add()).append(new KeyedCodec("Hint", Codec.STRING), (data, value) -> data.originalHint = value, (data) -> data.originalHint).add()).append(new KeyedCodec("Captured", Codec.BOOLEAN), (data, value) -> data.captured = value, (data) -> data.captured).add()).build();
   }
}
