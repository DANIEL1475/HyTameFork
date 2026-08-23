package com.hytame.tame.actions;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import javax.annotation.Nonnull;

public class BuilderActionHyTameFeedInteraction extends BuilderActionBase {
   @Nonnull
   public String getShortDescription() {
      return "Route feeding to taming or breeding based on state";
   }

   @Nonnull
   public String getLongDescription() {
      return "Routes feeding interactions: if wild animal + taming food -> tame; if tamed + breeding food -> breed";
   }

   @Nonnull
   public BuilderDescriptorState getBuilderDescriptorState() {
      return BuilderDescriptorState.Stable;
   }

   @Nonnull
   public ActionHyTameFeedInteraction build(@Nonnull BuilderSupport builderSupport) {
      return new ActionHyTameFeedInteraction(this, builderSupport);
   }

   public Builder<Action> readConfig(@Nonnull JsonElement data) {
      return super.readConfig(data);
   }
}
