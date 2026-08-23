package com.animaltaming.persistence;

import com.animaltaming.api.model.TamedAnimal;
import com.animaltaming.persistence.codec.TamedAnimalCodec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class JsonTamingRepository implements TamingRepository {
   private final Path dataFolder;
   private final Path animalsFolder;
   private final Path ownersFolder;
   private final TamedAnimalCodec codec;
   private final Gson gson;
   private final Map<UUID, TamedAnimal> cache = new ConcurrentHashMap();
   private final Map<UUID, Set<UUID>> ownerIndex = new ConcurrentHashMap();

   public JsonTamingRepository(Path pluginFolder, TamedAnimalCodec codec) {
      this.dataFolder = pluginFolder.resolve("data");
      this.animalsFolder = this.dataFolder.resolve("animals");
      this.ownersFolder = this.dataFolder.resolve("owners");
      this.codec = (TamedAnimalCodec)Objects.requireNonNull(codec, "codec required");
      this.gson = (new GsonBuilder()).setPrettyPrinting().create();
      this.initializeFolders();
   }

   private void initializeFolders() {
      try {
         Files.createDirectories(this.animalsFolder);
         Files.createDirectories(this.ownersFolder);
      } catch (IOException e) {
         throw new RuntimeException("Failed to create data folders", e);
      }
   }

   public void save(TamedAnimal animal) {
      Objects.requireNonNull(animal, "animal required");
      Path targetFile = this.animalsFolder.resolve(String.valueOf(animal.id()) + ".json");
      Path tempFile = this.animalsFolder.resolve(String.valueOf(animal.id()) + ".json.tmp");

      try {
         String json = this.codec.encode(animal);
         Files.writeString(tempFile, json, StandardCharsets.UTF_8);
         Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         this.cache.put(animal.id(), animal);
         this.updateOwnerIndex(animal.ownerId(), animal.id(), true);
      } catch (IOException e) {
         try {
            Files.deleteIfExists(tempFile);
         } catch (IOException var6) {
         }

         throw new RuntimeException("Failed to save animal " + String.valueOf(animal.id()), e);
      }
   }

   public Optional<TamedAnimal> load(UUID animalId) {
      Objects.requireNonNull(animalId, "animalId required");
      if (this.cache.containsKey(animalId)) {
         return Optional.of((TamedAnimal)this.cache.get(animalId));
      } else {
         Path animalFile = this.animalsFolder.resolve(String.valueOf(animalId) + ".json");
         if (!Files.exists(animalFile, new LinkOption[0])) {
            return Optional.empty();
         } else {
            try {
               String json = Files.readString(animalFile, StandardCharsets.UTF_8);
               TamedAnimal animal = this.codec.decode(json);
               this.cache.put(animalId, animal);
               return Optional.of(animal);
            } catch (IllegalArgumentException | IOException e) {
               PrintStream var10000 = System.err;
               String var10001 = String.valueOf(animalId);
               var10000.println("[TamingRepository] Failed to load animal " + var10001 + ": " + ((Exception)e).getMessage());
               return Optional.empty();
            }
         }
      }
   }

   public boolean delete(UUID animalId) {
      Objects.requireNonNull(animalId, "animalId required");
      TamedAnimal animal = (TamedAnimal)this.cache.remove(animalId);
      Path animalFile = this.animalsFolder.resolve(String.valueOf(animalId) + ".json");

      try {
         boolean deleted = Files.deleteIfExists(animalFile);
         if (animal != null) {
            this.updateOwnerIndex(animal.ownerId(), animalId, false);
         }

         return deleted;
      } catch (IOException e) {
         PrintStream var10000 = System.err;
         String var10001 = String.valueOf(animalId);
         var10000.println("[TamingRepository] Failed to delete animal " + var10001 + ": " + e.getMessage());
         return false;
      }
   }

   public Set<UUID> findByOwner(UUID ownerId) {
      Objects.requireNonNull(ownerId, "ownerId required");
      if (this.ownerIndex.containsKey(ownerId)) {
         return new HashSet((Collection)this.ownerIndex.get(ownerId));
      } else {
         Path ownerFile = this.ownersFolder.resolve(String.valueOf(ownerId) + ".json");
         if (!Files.exists(ownerFile, new LinkOption[0])) {
            return Set.of();
         } else {
            try {
               String json = Files.readString(ownerFile, StandardCharsets.UTF_8);
               JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
               JsonArray animalsArray = obj.getAsJsonArray("animals");
               Set<UUID> animals = ConcurrentHashMap.newKeySet();

               for(JsonElement element : animalsArray) {
                  try {
                     animals.add(UUID.fromString(element.getAsString()));
                  } catch (IllegalArgumentException var10) {
                  }
               }

               this.ownerIndex.put(ownerId, animals);
               return new HashSet(animals);
            } catch (JsonSyntaxException | IOException e) {
               PrintStream var10000 = System.err;
               String var10001 = String.valueOf(ownerId);
               var10000.println("[TamingRepository] Failed to load owner index " + var10001 + ": " + ((Exception)e).getMessage());
               return Set.of();
            }
         }
      }
   }

   public Collection<TamedAnimal> loadAll() {
      List<TamedAnimal> animals = new ArrayList();

      try {
         Stream<Path> stream = Files.list(this.animalsFolder);

         try {
            stream.filter((p) -> p.toString().endsWith(".json")).filter((p) -> !p.toString().endsWith(".tmp")).forEach((path) -> {
               String filename = path.getFileName().toString();
               String uuidStr = filename.substring(0, filename.length() - 5);

               try {
                  UUID animalId = UUID.fromString(uuidStr);
                  Optional var10000 = this.load(animalId);
                  Objects.requireNonNull(animals);
                  var10000.ifPresent(animals::add);
               } catch (IllegalArgumentException var6) {
                  System.err.println("[TamingRepository] Invalid animal file: " + filename);
               }

            });
         } catch (Throwable var6) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (IOException e) {
         System.err.println("[TamingRepository] Failed to list animals: " + e.getMessage());
      }

      return animals;
   }

   public boolean exists(UUID animalId) {
      return this.cache.containsKey(animalId) ? true : Files.exists(this.animalsFolder.resolve(String.valueOf(animalId) + ".json"), new LinkOption[0]);
   }

   public int count() {
      try {
         Stream<Path> stream = Files.list(this.animalsFolder);

         int var2;
         try {
            var2 = (int)stream.filter((p) -> p.toString().endsWith(".json")).filter((p) -> !p.toString().endsWith(".tmp")).count();
         } catch (Throwable var5) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (stream != null) {
            stream.close();
         }

         return var2;
      } catch (IOException var6) {
         return this.cache.size();
      }
   }

   private void updateOwnerIndex(UUID ownerId, UUID animalId, boolean add) {
      Set<UUID> animals = (Set)this.ownerIndex.computeIfAbsent(ownerId, (k) -> ConcurrentHashMap.newKeySet());
      if (add) {
         animals.add(animalId);
      } else {
         animals.remove(animalId);
      }

      this.saveOwnerIndex(ownerId);
   }

   private void saveOwnerIndex(UUID ownerId) {
      Set<UUID> animals = (Set)this.ownerIndex.get(ownerId);
      if (animals != null) {
         JsonObject obj = new JsonObject();
         JsonArray animalsArray = new JsonArray();

         for(UUID animalId : animals) {
            animalsArray.add(animalId.toString());
         }

         obj.add("animals", animalsArray);
         Path targetFile = this.ownersFolder.resolve(String.valueOf(ownerId) + ".json");
         Path tempFile = this.ownersFolder.resolve(String.valueOf(ownerId) + ".json.tmp");

         try {
            Files.writeString(tempFile, this.gson.toJson(obj), StandardCharsets.UTF_8);
            Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            try {
               Files.deleteIfExists(tempFile);
            } catch (IOException var9) {
            }

            PrintStream var10000 = System.err;
            String var10001 = String.valueOf(ownerId);
            var10000.println("[TamingRepository] Failed to save owner index " + var10001 + ": " + e.getMessage());
         }

      }
   }

   public void clearCache() {
      this.cache.clear();
      this.ownerIndex.clear();
   }
}
