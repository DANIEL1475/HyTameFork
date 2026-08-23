package com.animaltaming.config;

import com.animaltaming.api.model.DietType;
import com.animaltaming.api.model.TamingConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConfigLoader {
   private final Gson gson = new Gson();

   public List<TamingConfig> loadFromResources(String resourcePath) {
      List<TamingConfig> configs = new ArrayList();

      try {
         URL resourceUrl = this.getClass().getClassLoader().getResource(resourcePath);
         if (resourceUrl == null) {
            System.err.println("[ConfigLoader] Resource path not found: " + resourcePath);
            return configs;
         }

         if (resourceUrl.getProtocol().equals("file")) {
            Path resourceDir = Paths.get(resourceUrl.toURI());
            this.loadFromDirectory(resourceDir, configs);
         } else {
            this.loadKnownResources(resourcePath, configs);
         }
      } catch (RuntimeException | URISyntaxException e) {
         System.err.println("[ConfigLoader] Failed to load resources: " + ((Exception)e).getMessage());
      }

      return configs;
   }

   public List<TamingConfig> loadFromDirectory(Path directory) {
      List<TamingConfig> configs = new ArrayList();
      this.loadFromDirectory(directory, configs);
      return configs;
   }

   private void loadFromDirectory(Path directory, List<TamingConfig> configs) {
      if (Files.exists(directory, new LinkOption[0]) && Files.isDirectory(directory, new LinkOption[0])) {
         try {
            Stream<Path> stream = Files.list(directory);

            try {
               stream.filter((p) -> p.toString().endsWith(".json")).forEach((path) -> {
                  try {
                     TamingConfig config = this.loadFromFile(path);
                     configs.add(config);
                  } catch (RuntimeException e) {
                     throw new RuntimeException("Failed to load config " + String.valueOf(path) + ": " + e.getMessage(), e);
                  }
               });
            } catch (Throwable var7) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stream != null) {
               stream.close();
            }

         } catch (IOException e) {
            throw new RuntimeException("Failed to list config directory: " + String.valueOf(directory), e);
         }
      }
   }

   private void loadKnownResources(String resourcePath, List<TamingConfig> configs) {
      String[] knownSpecies = new String[]{"wolf", "horse", "cat", "bear", "trork"};

      for(String species : knownSpecies) {
         String fullPath = resourcePath + "/" + species + ".json";

         try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(fullPath);

            try {
               if (is != null) {
                  TamingConfig config = this.loadFromStream(is, species + ".json");
                  configs.add(config);
               }
            } catch (Throwable var13) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (is != null) {
               is.close();
            }
         } catch (IOException e) {
            System.err.println("[ConfigLoader] Failed to load resource " + fullPath + ": " + e.getMessage());
         }
      }

   }

   public TamingConfig loadFromFile(Path path) {
      try {
         String json = Files.readString(path, StandardCharsets.UTF_8);
         return this.parseAndValidate(json, path.getFileName().toString());
      } catch (IOException e) {
         throw new RuntimeException("Failed to read config file: " + String.valueOf(path), e);
      }
   }

   public TamingConfig loadFromStream(InputStream inputStream, String sourceName) {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

         TamingConfig var6;
         try {
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
               sb.append(line);
            }

            var6 = this.parseAndValidate(sb.toString(), sourceName);
         } catch (Throwable var8) {
            try {
               reader.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         reader.close();
         return var6;
      } catch (IOException e) {
         throw new RuntimeException("Failed to read config from stream: " + sourceName, e);
      }
   }

   public TamingConfig parseAndValidate(String json, String sourceName) {
      try {
         JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
         String speciesId = this.getRequiredString(obj, "speciesId", sourceName);
         DietType dietType = (DietType)this.getRequiredEnum(obj, "dietType", DietType.class, sourceName);
         List<String> preferredFoods = this.getRequiredStringList(obj, "preferredFoods", sourceName);
         boolean canBeMounted = this.getRequiredBoolean(obj, "canBeMounted", sourceName);
         double calmingDistance = this.getRequiredDouble(obj, "calmingDistance", sourceName);
         int calmingTimeTicks = this.getRequiredInt(obj, "calmingTimeTicks", sourceName);
         int calmDurationTicks = this.getRequiredInt(obj, "calmDurationTicks", sourceName);
         int trustPerFeed = this.getRequiredInt(obj, "trustPerFeed", sourceName);
         int trustPerMountSecond = this.getRequiredInt(obj, "trustPerMountSecond", sourceName);
         int requiredTrustLevel = this.getRequiredInt(obj, "requiredTrustLevel", sourceName);
         double maxFollowDistance = this.getRequiredDouble(obj, "maxFollowDistance", sourceName);
         return new TamingConfig(speciesId, dietType, preferredFoods, canBeMounted, calmingDistance, calmingTimeTicks, calmDurationTicks, trustPerFeed, trustPerMountSecond, requiredTrustLevel, maxFollowDistance);
      } catch (JsonSyntaxException e) {
         throw new RuntimeException("Invalid JSON in " + sourceName + ": " + e.getMessage(), e);
      } catch (IllegalArgumentException e) {
         throw new RuntimeException("Validation failed for " + sourceName + ": " + e.getMessage(), e);
      }
   }

   private String getRequiredString(JsonObject obj, String field, String source) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsString();
      } else {
         throw new IllegalArgumentException("Missing required field '" + field + "' in " + source);
      }
   }

   private int getRequiredInt(JsonObject obj, String field, String source) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsInt();
      } else {
         throw new IllegalArgumentException("Missing required field '" + field + "' in " + source);
      }
   }

   private double getRequiredDouble(JsonObject obj, String field, String source) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsDouble();
      } else {
         throw new IllegalArgumentException("Missing required field '" + field + "' in " + source);
      }
   }

   private boolean getRequiredBoolean(JsonObject obj, String field, String source) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         return obj.get(field).getAsBoolean();
      } else {
         throw new IllegalArgumentException("Missing required field '" + field + "' in " + source);
      }
   }

   private <T extends Enum<T>> T getRequiredEnum(JsonObject obj, String field, Class<T> enumClass, String source) {
      String value = this.getRequiredString(obj, field, source);

      try {
         return (T)Enum.valueOf(enumClass, value);
      } catch (IllegalArgumentException var7) {
         throw new IllegalArgumentException("Invalid value '" + value + "' for field '" + field + "' in " + source);
      }
   }

   private List<String> getRequiredStringList(JsonObject obj, String field, String source) {
      if (obj.has(field) && !obj.get(field).isJsonNull()) {
         JsonArray array = obj.getAsJsonArray(field);
         List<String> list = new ArrayList();

         for(JsonElement element : array) {
            list.add(element.getAsString());
         }

         return list;
      } else {
         throw new IllegalArgumentException("Missing required field '" + field + "' in " + source);
      }
   }
}
