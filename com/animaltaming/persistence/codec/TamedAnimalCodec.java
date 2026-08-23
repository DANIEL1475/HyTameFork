package com.animaltaming.persistence.codec;

import com.animaltaming.api.model.BehaviorMode;
import com.animaltaming.api.model.TamedAnimal;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;

public class TamedAnimalCodec {
   private final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();

   public String encode(TamedAnimal animal) {
      JsonObject obj = new JsonObject();
      obj.addProperty("id", animal.id().toString());
      obj.addProperty("ownerId", animal.ownerId().toString());
      obj.addProperty("ownerName", animal.ownerName());
      obj.addProperty("speciesId", animal.speciesId());
      obj.addProperty("mode", animal.mode().name());
      obj.addProperty("homeX", animal.homeX());
      obj.addProperty("homeY", animal.homeY());
      obj.addProperty("homeZ", animal.homeZ());
      obj.addProperty("maxFollowDistance", animal.maxFollowDistance());
      obj.addProperty("tamedTimestamp", animal.tamedTimestamp());
      if (animal.customName() != null) {
         obj.addProperty("customName", animal.customName());
      }

      return this.gson.toJson(obj);
   }

   public JsonElement encodeToElement(TamedAnimal animal) {
      return JsonParser.parseString(this.encode(animal));
   }

   public TamedAnimal decode(String json) {
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      return this.decodeFromElement(obj);
   }

   public TamedAnimal decodeFromElement(JsonElement element) {
      if (!element.isJsonObject()) {
         throw new IllegalArgumentException("Expected JSON object");
      } else {
         JsonObject obj = element.getAsJsonObject();
         UUID id = this.getRequiredUUID(obj, "id");
         UUID ownerId = this.getRequiredUUID(obj, "ownerId");
         String ownerName = this.getRequiredString(obj, "ownerName");
         String speciesId = this.getRequiredString(obj, "speciesId");
         BehaviorMode mode = (BehaviorMode)this.getRequiredEnum(obj, "mode", BehaviorMode.class);
         double homeX = this.getRequiredDouble(obj, "homeX");
         double homeY = this.getRequiredDouble(obj, "homeY");
         double homeZ = this.getRequiredDouble(obj, "homeZ");
         double maxFollowDistance = this.getRequiredDouble(obj, "maxFollowDistance");
         long tamedTimestamp = this.getRequiredLong(obj, "tamedTimestamp");
         String customName = obj.has("customName") && !obj.get("customName").isJsonNull() ? obj.get("customName").getAsString() : null;
         return new TamedAnimal(id, ownerId, ownerName, speciesId, mode, homeX, homeY, homeZ, maxFollowDistance, tamedTimestamp, customName);
      }
   }

   private UUID getRequiredUUID(JsonObject obj, String field) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         try {
            return UUID.fromString(obj.get(field).getAsString());
         } catch (IllegalArgumentException var4) {
            throw new IllegalArgumentException("Invalid UUID for field: " + field);
         }
      } else {
         throw new IllegalArgumentException("Missing required field: " + field);
      }
   }

   private String getRequiredString(JsonObject obj, String field) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsString();
      } else {
         throw new IllegalArgumentException("Missing required field: " + field);
      }
   }

   private double getRequiredDouble(JsonObject obj, String field) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsDouble();
      } else {
         throw new IllegalArgumentException("Missing required field: " + field);
      }
   }

   private long getRequiredLong(JsonObject obj, String field) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsLong();
      } else {
         throw new IllegalArgumentException("Missing required field: " + field);
      }
   }

   private <T extends Enum<T>> T getRequiredEnum(JsonObject obj, String field, Class<T> enumClass) {
      String value = this.getRequiredString(obj, field);

      try {
         return (T)Enum.valueOf(enumClass, value);
      } catch (IllegalArgumentException var6) {
         throw new IllegalArgumentException("Invalid value '" + value + "' for enum field: " + field);
      }
   }
}
