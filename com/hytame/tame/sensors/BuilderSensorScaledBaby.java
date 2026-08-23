package com.hytame.tame.sensors;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import javax.annotation.Nonnull;

public class BuilderSensorScaledBaby extends BuilderSensorBase {
   @Nonnull
   public String getShortDescription() {
      return "Check if entity is a scaled baby";
   }

   @Nonnull
   public String getLongDescription() {
      return "Returns true if the entity is a 'scaled baby' - an animal that uses model scaling for growth instead of a dedicated baby NPC variant. Examples: wolves, bears, foxes (use scaling). Counter-examples: sheep lambs, cow calves (use baby NPC roles).";
   }

   @Nonnull
   public Sensor build(@Nonnull BuilderSupport builderSupport) {
      return new SensorScaledBaby(this, builderSupport);
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
