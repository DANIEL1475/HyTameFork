package com.hytame.tame.sensors;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import javax.annotation.Nonnull;

public class BuilderSensorGrowthReady extends BuilderSensorBase {
   @Nonnull
   public String getShortDescription() {
      return "Check if entity can grow (not adult)";
   }

   @Nonnull
   public String getLongDescription() {
      return "Returns true if the entity's growth stage is BABY or JUVENILE (not yet ADULT). Used with alarm sensor to trigger growth transitions.";
   }

   @Nonnull
   public Sensor build(@Nonnull BuilderSupport builderSupport) {
      return new SensorGrowthReady(this, builderSupport);
   }

   @Nonnull
   public Builder<Sensor> readConfig(@Nonnull JsonElement data) {
      return this;
   }

   @Nonnull
   public BuilderDescriptorState getBuilderDescriptorState() {
      return BuilderDescriptorState.Stable;
   }
}
