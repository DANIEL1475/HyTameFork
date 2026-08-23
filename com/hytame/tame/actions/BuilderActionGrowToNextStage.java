package com.hytame.tame.actions;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import javax.annotation.Nonnull;

public class BuilderActionGrowToNextStage extends BuilderActionBase {
   @Nonnull
   public String getShortDescription() {
      return "Trigger growth transition for baby animals";
   }

   @Nonnull
   public String getLongDescription() {
      return "Triggers growth transition for baby animals. For livestock (baby NPC roles like Lamb, Calf): despawns baby and spawns adult. For wild animals (scaled babies like Wolf, Bear): updates model scale.";
   }

   @Nonnull
   public BuilderDescriptorState getBuilderDescriptorState() {
      return BuilderDescriptorState.Stable;
   }

   @Nonnull
   public ActionGrowToNextStage build(@Nonnull BuilderSupport builderSupport) {
      return new ActionGrowToNextStage(this, builderSupport);
   }

   public Builder<Action> readConfig(@Nonnull JsonElement data) {
      return super.readConfig(data);
   }
}
