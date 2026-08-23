package com.hytame.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hytame.HyTamePlugin;
import com.hytame.models.TamedAnimalData;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PersistenceManager {
   private static final int CURRENT_VERSION = 1;
   private static final String SAVE_FILE_NAME = "tamed_animals.json";
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final Gson GSON_STREAM = (new GsonBuilder()).create();
   private Path saveFilePath;
   private final Object saveLock = new Object();
   private final AtomicBoolean saveInProgress = new AtomicBoolean(false);

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   private void logWarning(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atWarning()).log(message);
      }

   }

   public void initialize(Path dataDirectory) {
      this.saveFilePath = dataDirectory.resolve("tamed_animals.json");
      this.cleanupTempAndBackups(dataDirectory);
      Path oldDir = dataDirectory.getParent().resolve("Lait_AnimalBreeding");
      if (Files.exists(oldDir, new LinkOption[0])) {
         this.cleanupTempAndBackups(oldDir);

         try {
            Path oldFile = oldDir.resolve("tamed_animals.json");
            if (Files.deleteIfExists(oldFile)) {
               this.log("Deleted legacy tamed_animals.json from Lait_AnimalBreeding");
            }
         } catch (IOException e) {
            this.logWarning("Failed to delete legacy file from old dir: " + e.getMessage());
         }
      }

   }

   private void cleanupTempAndBackups(Path directory) {
      try {
         Files.deleteIfExists(directory.resolve("tamed_animals.json.tmp"));
      } catch (IOException var7) {
      }

      try {
         if (Files.exists(directory, new LinkOption[0])) {
            for(Path backup : Files.list(directory).filter((p) -> {
               String name = p.getFileName().toString();
               return name.startsWith("tamed_animals") && name.endsWith(".json") && name.contains("_backup_");
            }).toList()) {
               try {
                  Files.delete(backup);
               } catch (IOException var6) {
                  this.logWarning("Failed to delete backup: " + String.valueOf(backup.getFileName()));
               }
            }
         }
      } catch (IOException e) {
         this.logWarning("Failed to list backups: " + e.getMessage());
      }

   }

   public List<TamedAnimalData> loadData() {
      List<TamedAnimalData> result = new ArrayList();
      if (this.saveFilePath != null && Files.exists(this.saveFilePath, new LinkOption[0])) {
         int deadCount = 0;

         try {
            String json = Files.readString(this.saveFilePath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("tamedAnimals") && root.get("tamedAnimals").isJsonArray()) {
               for(JsonElement elem : root.getAsJsonArray("tamedAnimals")) {
                  try {
                     TamedAnimalData data = (TamedAnimalData)GSON.fromJson(elem, TamedAnimalData.class);
                     if (data != null && data.getAnimalUuid() != null && data.getOwnerUuid() != null) {
                        if (data.isDead()) {
                           ++deadCount;
                        } else {
                           result.add(data);
                        }
                     }
                  } catch (Exception e) {
                     this.logWarning("Failed to parse tamed animal entry: " + e.getMessage());
                  }
               }
            }

            int var10001 = result.size();
            this.log("Loaded " + var10001 + " tamed animals from file (" + deadCount + " dead removed)");
         } catch (Exception e) {
            this.logWarning("Failed to load tamed_animals.json: " + e.getMessage());
         }

         if (deadCount > 0) {
            ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log("[Taming] Removed " + deadCount + " dead animals from tamed_animals.json");
            this.writeFile(result);
         }

         return result;
      } else {
         return result;
      }
   }

   public void saveData(Collection<TamedAnimalData> tamedAnimals) {
      if (this.saveFilePath != null) {
         if (this.saveInProgress.compareAndSet(false, true)) {
            List<TamedAnimalData> alive = tamedAnimals.stream().filter((d) -> d != null && !d.isDead() && d.getAnimalUuid() != null).toList();
            CompletableFuture.runAsync(() -> {
               try {
                  this.writeFile(alive);
               } finally {
                  this.saveInProgress.set(false);
               }

            }).exceptionally((e) -> {
               this.logWarning("Async save failed: " + e.getMessage());
               this.saveInProgress.set(false);
               return null;
            });
         }
      }
   }

   public void saveDataSync(Collection<TamedAnimalData> tamedAnimals) {
      if (this.saveFilePath != null) {
         List<TamedAnimalData> alive = tamedAnimals.stream().filter((d) -> d != null && !d.isDead() && d.getAnimalUuid() != null).toList();
         this.writeFile(alive);
      }
   }

   private void writeFile(List<TamedAnimalData> animals) {
      synchronized(this.saveLock) {
         try {
            if (animals.isEmpty()) {
               Files.deleteIfExists(this.saveFilePath);
               this.log("Deleted tamed_animals.json (no animals remaining)");
               return;
            }

            Path tempFile = this.saveFilePath.resolveSibling("tamed_animals.json.tmp");
            Files.createDirectories(this.saveFilePath.getParent());
            BufferedWriter fileWriter = Files.newBufferedWriter(tempFile);

            try {
               JsonWriter writer = new JsonWriter(fileWriter);

               try {
                  writer.setIndent("  ");
                  writer.beginObject();
                  writer.name("version").value(1L);
                  writer.name("lastSaved").value(System.currentTimeMillis());
                  writer.name("tamedAnimals");
                  writer.beginArray();

                  for(TamedAnimalData data : animals) {
                     GSON_STREAM.toJson(data, TamedAnimalData.class, writer);
                  }

                  writer.endArray();
                  writer.endObject();
               } catch (Throwable var11) {
                  try {
                     writer.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               writer.close();
            } catch (Throwable var12) {
               if (fileWriter != null) {
                  try {
                     fileWriter.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (fileWriter != null) {
               fileWriter.close();
            }

            Files.move(tempFile, this.saveFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            this.log("Saved " + animals.size() + " tamed animals to tamed_animals.json");
         } catch (IOException e) {
            this.logWarning("Failed to save tamed_animals.json: " + e.getMessage());
         }

      }
   }

   public void markDirty() {
   }

   public boolean isDirty() {
      return false;
   }

   public boolean isSaveInProgress() {
      return this.saveInProgress.get();
   }

   public long getLastSaveTime() {
      return 0L;
   }

   public void startAutoSave(ScheduledExecutorService scheduler, Supplier<Collection<TamedAnimalData>> dataSupplier, long intervalMinutes) {
   }

   public void stopAutoSave() {
   }

   public void forceSave(Collection<TamedAnimalData> tamedAnimals) {
      this.saveData(tamedAnimals);
   }

   public void forceSaveSync(Collection<TamedAnimalData> tamedAnimals) {
      this.saveDataSync(tamedAnimals);
   }

   public Path getSaveFilePath() {
      return this.saveFilePath;
   }
}
