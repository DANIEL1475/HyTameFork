package com.hytame.coop;

import com.hytame.models.AnimalType;
import com.hytame.models.TamedAnimalData;
import com.hytame.tame.HyTameComponent;
import java.util.UUID;

public class HyTameCoopData {
   private UUID hytameId;
   private UUID ownerUuid;
   private String ownerName;
   private String customName;
   private String animalType;
   private boolean tamed;

   public HyTameCoopData() {
   }

   public HyTameCoopData(UUID hytameId, UUID ownerUuid, String ownerName, String customName, String animalType, boolean tamed) {
      this.hytameId = hytameId;
      this.ownerUuid = ownerUuid;
      this.ownerName = ownerName;
      this.customName = customName;
      this.animalType = animalType;
      this.tamed = tamed;
   }

   public static HyTameCoopData from(HyTameComponent comp, TamedAnimalData tamedData) {
      HyTameCoopData data = new HyTameCoopData();
      data.tamed = comp.isTamed();
      data.hytameId = comp.getHytameId();
      data.ownerUuid = comp.getTamerUUID();
      data.ownerName = comp.getTamerName();
      if (tamedData != null) {
         data.customName = tamedData.getCustomName();
         AnimalType type = tamedData.getAnimalType();
         data.animalType = type != null ? type.name() : null;
      }

      return data;
   }

   public void setHytameIdStr(String value) {
      this.hytameId = value != null ? UUID.fromString(value) : null;
   }

   public String getHytameIdStr() {
      return this.hytameId != null ? this.hytameId.toString() : null;
   }

   public void setOwnerUuidStr(String value) {
      this.ownerUuid = value != null ? UUID.fromString(value) : null;
   }

   public String getOwnerUuidStr() {
      return this.ownerUuid != null ? this.ownerUuid.toString() : null;
   }

   public UUID getHytameId() {
      return this.hytameId;
   }

   public void setHytameId(UUID hytameId) {
      this.hytameId = hytameId;
   }

   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public void setOwnerUuid(UUID ownerUuid) {
      this.ownerUuid = ownerUuid;
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
   }

   public String getCustomName() {
      return this.customName;
   }

   public void setCustomName(String customName) {
      this.customName = customName;
   }

   public String getAnimalType() {
      return this.animalType;
   }

   public void setAnimalType(String animalType) {
      this.animalType = animalType;
   }

   public boolean isTamed() {
      return this.tamed;
   }

   public void setTamed(boolean tamed) {
      this.tamed = tamed;
   }

   public String toString() {
      String var10000 = String.valueOf(this.hytameId);
      return "HyTameCoopData{hytameId=" + var10000 + ", owner=" + this.ownerName + ", name=" + this.customName + ", type=" + this.animalType + ", tamed=" + this.tamed + "}";
   }
}
