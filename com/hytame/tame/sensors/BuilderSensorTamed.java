package com.hytame.tame.sensors;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import javax.annotation.Nonnull;

public class BuilderSensorTamed extends BuilderSensorBase {
   protected final BooleanHolder value = new BooleanHolder();

   @Nonnull
   public String getShortDescription() {
      return "Test if an entity is tamed or not";
   }

   @Nonnull
   public String getLongDescription() {
      return this.getShortDescription();
   }

   @Nonnull
   public Sensor build(@Nonnull BuilderSupport builderSupport) {
      return new SensorTamed(this, builderSupport);
   }

   @Nonnull
   public Builder<Sensor> readConfig(@Nonnull JsonElement data) {
      this.getBoolean(data, "Set", this.value, true, BuilderDescriptorState.Stable, "Whether the entity is tamed or not", (String)null);
      return this;
   }

   @Nonnull
   public BuilderDescriptorState getBuilderDescriptorState() {
      return BuilderDescriptorState.Stable;
   }

   public boolean getValue(@Nonnull BuilderSupport support) {
      return this.value.get(support.getExecutionContext());
   }
}
