package com.hytame.tame;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.models.GrowthStage;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HyTameComponent implements Component<EntityStore> {
   public static final BuilderCodec<HyTameComponent> CODEC;
   private Boolean isTamed = false;
   private UUID tamerUUID = null;
   private String tamerName = null;
   private UUID hytameId = null;
   private Boolean actionReady = true;
   private Integer growthStageOrdinal;
   private Boolean needsBreedCooldown;
   private Integer originalAttitudeOrdinal;
   private Integer originalSpawnConfig;

   public HyTameComponent() {
      this.growthStageOrdinal = GrowthStage.ADULT.ordinal();
      this.needsBreedCooldown = false;
      this.originalAttitudeOrdinal = null;
      this.originalSpawnConfig = null;
   }

   public static ComponentType<EntityStore, HyTameComponent> getComponentType() {
      return HyTamePlugin.getInstance().getHyTameComponentType();
   }

   public boolean isTamed() {
      return Boolean.TRUE.equals(this.isTamed);
   }

   public UUID getTamerUUID() {
      return this.tamerUUID;
   }

   public String getTamerName() {
      return this.tamerName;
   }

   public UUID getHytameId() {
      return this.hytameId;
   }

   public void setHytameId(UUID hytameId) {
      this.hytameId = hytameId;
   }

   public boolean isActionReady() {
      return Boolean.TRUE.equals(this.actionReady);
   }

   public void setActionReady(boolean ready) {
      this.actionReady = ready;
   }

   public GrowthStage getGrowthStage() {
      if (this.growthStageOrdinal == null) {
         return GrowthStage.ADULT;
      } else {
         GrowthStage[] values = GrowthStage.values();
         int ordinal = this.growthStageOrdinal;
         return ordinal >= 0 && ordinal < values.length ? values[ordinal] : GrowthStage.ADULT;
      }
   }

   public void setGrowthStage(GrowthStage stage) {
      this.growthStageOrdinal = stage != null ? stage.ordinal() : GrowthStage.ADULT.ordinal();
   }

   public boolean canGrow() {
      return this.getGrowthStage() != GrowthStage.ADULT;
   }

   public boolean growToNextStage() {
      GrowthStage current = this.getGrowthStage();
      if (current.hasNextStage()) {
         this.setGrowthStage(current.getNextStage());
         return true;
      } else {
         return false;
      }
   }

   public boolean isNeedsBreedCooldown() {
      return Boolean.TRUE.equals(this.needsBreedCooldown);
   }

   public void setNeedsBreedCooldown(boolean needsCooldown) {
      this.needsBreedCooldown = needsCooldown;
   }

   public Integer getOriginalAttitudeOrdinal() {
      return this.originalAttitudeOrdinal;
   }

   public void setOriginalAttitudeOrdinal(Integer ordinal) {
      this.originalAttitudeOrdinal = ordinal;
   }

   public Integer getOriginalSpawnConfig() {
      return this.originalSpawnConfig;
   }

   public void setOriginalSpawnConfig(Integer config) {
      this.originalSpawnConfig = config;
   }

   public void clearTaming() {
      this.isTamed = false;
      this.tamerUUID = null;
      this.tamerName = null;
      this.hytameId = null;
      this.originalAttitudeOrdinal = null;
      this.originalSpawnConfig = null;
   }

   public void setTamed(@Nonnull UUID player, @Nonnull String playerName) {
      this.isTamed = true;
      this.tamerUUID = player;
      this.tamerName = playerName;
      if (this.hytameId == null) {
         this.hytameId = UUID.randomUUID();
      }

   }

   @Nullable
   public Component<EntityStore> clone() {
      HyTameComponent component = new HyTameComponent();
      component.isTamed = this.isTamed;
      component.tamerUUID = this.tamerUUID;
      component.tamerName = this.tamerName;
      component.hytameId = this.hytameId;
      component.actionReady = this.actionReady;
      component.growthStageOrdinal = this.growthStageOrdinal;
      component.needsBreedCooldown = this.needsBreedCooldown;
      component.originalAttitudeOrdinal = this.originalAttitudeOrdinal;
      component.originalSpawnConfig = this.originalSpawnConfig;
      return component;
   }

   static {
      CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(HyTameComponent.class, HyTameComponent::new).append(new KeyedCodec("IsTamed", Codec.BOOLEAN), (data, value) -> data.isTamed = value, (data) -> data.isTamed).add()).append(new KeyedCodec("TamerUUID", Codec.UUID_BINARY), (data, value) -> data.tamerUUID = value, (data) -> data.tamerUUID).add()).append(new KeyedCodec("TamerName", Codec.STRING), (data, value) -> data.tamerName = value, (data) -> data.tamerName).add()).append(new KeyedCodec("HytameId", Codec.UUID_BINARY), (data, value) -> data.hytameId = value, (data) -> data.hytameId).add()).append(new KeyedCodec("ActionReady", Codec.BOOLEAN), (data, value) -> data.actionReady = value, (data) -> data.actionReady).add()).append(new KeyedCodec("GrowthStageOrdinal", Codec.INTEGER), (data, value) -> data.growthStageOrdinal = value, (data) -> data.growthStageOrdinal).add()).append(new KeyedCodec("NeedsBreedCooldown", Codec.BOOLEAN), (data, value) -> data.needsBreedCooldown = value, (data) -> data.needsBreedCooldown).add()).append(new KeyedCodec("OriginalAttitudeOrdinal", Codec.INTEGER), (data, value) -> data.originalAttitudeOrdinal = value, (data) -> data.originalAttitudeOrdinal).add()).append(new KeyedCodec("OriginalSpawnConfig", Codec.INTEGER), (data, value) -> data.originalSpawnConfig = value, (data) -> data.originalSpawnConfig).add()).build();
   }
}
