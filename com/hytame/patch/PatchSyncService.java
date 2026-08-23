package com.hytame.patch;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.assetstore.AssetPack.PackSource;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class PatchSyncService {
   private static final double REAL_TO_GAME_TIME_RATIO = (double)30.0F;
   private static final int CURRENT_PATCH_FORMAT = 2;
   private static final String METADATA_FILE = "metadata.json";
   private static final String ASSET_PACK_NAME = "Config_HyTame";
   private static final String MANIFEST_TEMPLATE = "{\n    \"Group\": \"HyTame\",\n    \"Name\": \"Config_HyTame\",\n    \"Version\": \"1.0.0\",\n    \"Description\": \"[Auto-Generated] Contains config and patches for HyTame\",\n    \"Authors\": [\n        {\n            \"Name\": \"Lait\",\n            \"Email\": \"lait.kelomins@gmail.com\",\n            \"Url\": \"\"\n        },\n        {\n            \"Name\": \"TheBrandolorian\",\n            \"Email\": \"\",\n            \"Url\": \"\"\n        }\n    ],\n    \"Website\": \"\",\n    \"Main\": \"com.hytame.Config_HyTame\",\n    \"ServerVersion\": \"*\",\n    \"Dependencies\": {\n        \"com.hypersonicsharkz:Hytalor\": \"*\"\n    },\n    \"IncludesAssetPack\": true\n}\n";
   private static final Set<String> ANIMAL_NEUTRAL_VARIANTS = Set.of("Boar", "Bison", "Camel", "Chicken", "Chicken_Desert", "Cow", "Goat", "Horse", "Mouflon", "Pig", "Pig_Wild", "Rabbit", "Ram", "Sheep", "Skrill", "Turkey", "Warthog", "Boar_Piglet", "Bison_Calf", "Bunny", "Camel_Calf", "Chicken_Chick", "Chicken_Desert_Chick", "Cow_Calf", "Goat_Kid", "Horse_Foal", "Mouflon_Lamb", "Pig_Piglet", "Pig_Wild_Piglet", "Ram_Lamb", "Sheep_Lamb", "Skrill_Chick", "Turkey_Chick", "Warthog_Piglet", "Antelope", "Armadillo", "Deer_Doe", "Deer_Stag", "Moose_Bull", "Moose_Cow", "Mosshorn", "Mosshorn_Plain", "Crab", "Flamingo", "Penguin", "Tetrabird", "Tortoise");
   private static final Set<String> HAS_NATIVE_ATTRACTIVE_ITEM_SET = Set.of("Boar", "Boar_Piglet", "Bunny", "Camel", "Camel_Calf", "Chicken", "Chicken_Chick", "Chicken_Desert", "Chicken_Desert_Chick", "Cow", "Cow_Calf", "Goat", "Goat_Kid", "Horse", "Horse_Foal", "Mosshorn", "Mosshorn_Plain", "Mouflon", "Mouflon_Lamb", "Penguin", "Pig", "Pig_Piglet", "Pig_Wild", "Pig_Wild_Piglet", "Rabbit", "Ram", "Ram_Lamb", "Sheep", "Sheep_Lamb", "Skrill", "Skrill_Chick", "Turkey", "Turkey_Chick", "Warthog_Piglet");
   private static final Set<String> COMPUTE_DEPENDENT_PARAMS = Set.of("AttractiveItemSet", "GrowthTimeout");
   private static final Set<String> CANNOT_USE_MODIFY = Set.of("Wolf_White", "Bear_Polar", "Spider_Cave", "Snake_Cobra", "Snake_Rattle", "Zombie", "Zombie_Frost", "Zombie_Burnt", "Zombie_Sand", "Zombie_Aberrant", "Goblin_Scrapper", "Goblin_Thief", "Goblin_Miner", "Goblin_Lobber", "Goblin_Scavenger", "Goblin_Hermit", "Goblin_Ogre", "Goblin_Duke", "Trork_Brawler", "Trork_Warrior", "Trork_Hunter", "Trork_Sentry", "Trork_Guard", "Trork_Mauler", "Trork_Shaman", "Trork_Chieftain", "Trork_Doctor_Witch", "Scarak_Louse", "Scarak_Seeker", "Scarak_Fighter", "Scarak_Defender", "Scarak_Broodmother", "Kweebec_Seedling", "Kweebec_Sproutling", "Kweebec_Sapling", "Kweebec_Rootling", "Kweebec_Razorleaf", "Kweebec_Elder", "Outlander_Peon", "Outlander_Hunter", "Outlander_Marauder", "Outlander_Stalker", "Outlander_Berserker", "Outlander_Brute", "Outlander_Cultist", "Outlander_Priest", "Outlander_Sorcerer", "Eye_Void", "Spectre_Void");
   private Path assetPackRoot;
   private Path patchFolder;
   private ConfigManager configManager;
   private boolean forceSyncAll = false;

   public void initialize(ConfigManager configManager) {
      this.assetPackRoot = PluginManager.MODS_PATH.resolve("Config_HyTame");
      this.patchFolder = this.assetPackRoot.resolve("Server").resolve("Patch");
      this.configManager = configManager;
      this.ensureAssetPackExists();
      int existingFormat = this.readPatchFormat();
      if (existingFormat != 2) {
         this.logVerbose("Patch format changed (" + existingFormat + " -> 2), wiping old patches for clean regeneration");
         this.wipeOldPatches();
         this.forceSyncAll = true;
      }

      this.syncAllPatchesInternal();
      this.forceSyncAll = false;
      this.writeMetadata();
   }

   public void syncAllPatches() {
      this.syncAllPatchesInternal();
   }

   public void forceSyncAllPatches() {
      this.forceSyncAll = true;

      try {
         this.syncAllPatchesInternal();
      } finally {
         this.forceSyncAll = false;
      }

   }

   public void syncAllPatchesDeferred(int delaySeconds) {
      (new Thread(() -> {
         try {
            Thread.sleep((long)delaySeconds * 1000L);
         } catch (InterruptedException var3) {
            return;
         }

         this.logVerbose("Starting deferred patch sync...");
         this.syncAllPatchesInternal();
      }, "HyTame-PatchSync")).start();
   }

   private void ensureAssetPackExists() {
      try {
         Files.createDirectories(this.patchFolder);
         Path manifestPath = this.assetPackRoot.resolve("manifest.json");
         if (!Files.exists(manifestPath, new LinkOption[0])) {
            Files.writeString(manifestPath, "{\n    \"Group\": \"HyTame\",\n    \"Name\": \"Config_HyTame\",\n    \"Version\": \"1.0.0\",\n    \"Description\": \"[Auto-Generated] Contains config and patches for HyTame\",\n    \"Authors\": [\n        {\n            \"Name\": \"Lait\",\n            \"Email\": \"lait.kelomins@gmail.com\",\n            \"Url\": \"\"\n        },\n        {\n            \"Name\": \"TheBrandolorian\",\n            \"Email\": \"\",\n            \"Url\": \"\"\n        }\n    ],\n    \"Website\": \"\",\n    \"Main\": \"com.hytame.Config_HyTame\",\n    \"ServerVersion\": \"*\",\n    \"Dependencies\": {\n        \"com.hypersonicsharkz:Hytalor\": \"*\"\n    },\n    \"IncludesAssetPack\": true\n}\n");
            this.logVerbose("Created asset pack manifest: " + String.valueOf(manifestPath));
         }
      } catch (IOException e) {
         this.logWarning("Failed to create asset pack structure: " + e.getMessage());
      }

   }

   private int readPatchFormat() {
      Path metadataPath = this.assetPackRoot.resolve("metadata.json");
      if (!Files.exists(metadataPath, new LinkOption[0])) {
         return 0;
      } else {
         try {
            String content = Files.readString(metadataPath);
            String key = "\"patchFormat\"";
            int keyIdx = content.indexOf(key);
            if (keyIdx == -1) {
               return 0;
            } else {
               int colonIdx = content.indexOf(":", keyIdx + key.length());
               if (colonIdx == -1) {
                  return 0;
               } else {
                  String rest = content.substring(colonIdx + 1).trim();
                  StringBuilder numStr = new StringBuilder();

                  for(char c : rest.toCharArray()) {
                     if (!Character.isDigit(c)) {
                        break;
                     }

                     numStr.append(c);
                  }

                  return numStr.isEmpty() ? 0 : Integer.parseInt(numStr.toString());
               }
            }
         } catch (Exception e) {
            this.logWarning("Failed to read metadata: " + e.getMessage());
            return 0;
         }
      }
   }

   private void writeMetadata() {
      Path metadataPath = this.assetPackRoot.resolve("metadata.json");
      String json = "{\n    \"patchFormat\": 2\n}\n";

      try {
         Files.writeString(metadataPath, json);
         this.logVerbose("Wrote metadata: patchFormat=2");
      } catch (IOException e) {
         this.logWarning("Failed to write metadata: " + e.getMessage());
      }

   }

   private void wipeOldPatches() {
      if (this.patchFolder != null && Files.exists(this.patchFolder, new LinkOption[0])) {
         try {
            Stream<Path> files = Files.list(this.patchFolder);

            try {
               int deleted = 0;

               for(Path file : files.toList()) {
                  String name = file.getFileName().toString();
                  if (name.startsWith("NPC_") || name.startsWith("Tamed_") || name.startsWith("Growth_")) {
                     Files.deleteIfExists(file);
                     ++deleted;
                  }
               }

               if (deleted > 0) {
                  this.logVerbose("Wiped " + deleted + " old patch files for format migration");
               }
            } catch (Throwable var7) {
               if (files != null) {
                  try {
                     files.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (files != null) {
               files.close();
            }
         } catch (IOException e) {
            this.logWarning("Failed to wipe old patches: " + e.getMessage());
         }

      }
   }

   public void ensurePackRegistered() {
      try {
         AssetModule assetModule = AssetModule.get();
         if (assetModule == null) {
            this.logWarning("AssetModule not available, cannot register pack");
            return;
         }

         for(AssetPack pack : assetModule.getAssetPacks()) {
            if (pack.getName().contains("Config_HyTame")) {
               this.logVerbose("Pack already registered by AssetModule: " + pack.getName());
               return;
            }
         }

         this.registerAssetPack();
      } catch (Throwable e) {
         this.logWarning("Failed to ensure pack registered: " + e.getMessage());
      }

   }

   public void registerAssetPack() {
      try {
         PluginManifest manifest = new PluginManifest();
         manifest.setGroup("HyTame");
         manifest.setName("Config_HyTame");
         manifest.setVersion(Semver.fromString("1.0.0"));
         manifest.setDescription("Auto-generated asset patches from HyTame config");
         AssetModule.get().registerPack("com.hytame:Config_HyTame", this.assetPackRoot, manifest, PackSource.MODS);
         this.logVerbose("Registered asset pack: com.hytame:Config_HyTame at " + String.valueOf(this.assetPackRoot.toAbsolutePath()));
      } catch (Throwable e) {
         this.logWarning("Failed to register asset pack: " + e.getMessage());
      }

   }

   private void syncAllPatchesInternal() {
      int synced = 0;
      int skipped = 0;
      List<AnimalType> ordered = new ArrayList();

      for(AnimalType type : AnimalType.values()) {
         if (type.getCategory() == AnimalType.Category.LIVESTOCK) {
            ordered.add(0, type);
         } else {
            ordered.add(type);
         }
      }

      for(AnimalType type : ordered) {
         String npcPath = type.getNpcRolePath();
         if (npcPath == null) {
            ++skipped;
         } else {
            ConfigManager.AnimalConfig config = this.configManager.getAnimalConfig(type);
            if (type.usesVanillaTaming()) {
               boolean didSync = this.syncLivestockPatches(type, config);
               if (didSync) {
                  ++synced;
               } else {
                  ++skipped;
               }
            } else {
               List<String> configFoods = this.getAllAttractiveItemSet(config);
               if (configFoods.isEmpty()) {
                  this.deletePatch(type.getModelAssetId());
                  ++skipped;
               } else {
                  boolean npcNeedsSync = this.needsSync(type, configFoods);
                  if (npcNeedsSync) {
                     double breedCooldown = config.breedCooldownMinutes;
                     double growthForAdult = type.hasBabyVariant() ? (double)-1.0F : config.growthTimeMinutes;
                     this.syncCreaturePatch(type.getModelAssetId(), npcPath, configFoods, breedCooldown, growthForAdult);
                     ++synced;
                  } else {
                     ++skipped;
                  }
               }

               if (type.hasBabyVariant() && this.needsGrowthSync(type)) {
                  this.syncGrowthForAnimal(type);
               }
            }
         }
      }

      int customSynced = 0;
      Map<String, CustomAnimalConfig> customAnimals = this.configManager.getCustomAnimals();

      for(CustomAnimalConfig custom : customAnimals.values()) {
         if (!custom.getBreedingFoods().isEmpty()) {
            this.syncForCustomAnimal(custom);
            ++customSynced;
         }
      }

      this.logVerbose("Synced " + synced + " patches + " + customSynced + " custom, skipped " + skipped + " (already in sync)");
   }

   private boolean needsSync(AnimalType type, List<String> configFoods) {
      if (this.forceSyncAll) {
         return true;
      } else {
         Path patchFile = this.patchFolder.resolve("NPC_" + type.getModelAssetId() + ".json");
         if (Files.exists(patchFile, new LinkOption[0])) {
            List<String> patchFoods = this.readAttractiveItemSetFromPatch(patchFile);
            if (!this.foodsMatch(configFoods, patchFoods)) {
               return true;
            } else {
               String expectedPath = "Server/" + type.getNpcRolePath() + ".json";
               String existingPath = this.readBaseAssetPathFromPatch(patchFile);
               if (!expectedPath.equals(existingPath)) {
                  return true;
               } else {
                  try {
                     String content = Files.readString(patchFile);
                     boolean shouldUseModify = !CANNOT_USE_MODIFY.contains(type.getModelAssetId());
                     boolean hasModify = content.contains("\"Modify\"");
                     if (shouldUseModify != hasModify) {
                        return true;
                     }

                     if (shouldUseModify) {
                        boolean hasNative = HAS_NATIVE_ATTRACTIVE_ITEM_SET.contains(type.getModelAssetId());
                        boolean usesDollarPrefix = content.contains("\"$.AttractiveItemSet\"");
                        if (hasNative && !usesDollarPrefix) {
                           return true;
                        }

                        if (!hasNative && usesDollarPrefix) {
                           return true;
                        }
                     }

                     ConfigManager.AnimalConfig ac = this.configManager.getAnimalConfig(type);
                     String expectedCooldownIso = this.minutesToIso(ac.breedCooldownMinutes * (double)30.0F);
                     if (ac.breedCooldownMinutes > (double)0.0F && !content.contains(expectedCooldownIso)) {
                        return true;
                     }

                     double growthForAdult = type.hasBabyVariant() ? (double)-1.0F : ac.growthTimeMinutes;
                     if (growthForAdult > (double)0.0F) {
                        String expectedGrowthIso = this.minutesToIso(growthForAdult * (double)30.0F);
                        if (!content.contains(expectedGrowthIso)) {
                           return true;
                        }
                     }
                  } catch (IOException var15) {
                  }

                  return false;
               }
            }
         } else {
            return true;
         }
      }
   }

   private boolean needsGrowthSync(AnimalType type) {
      if (this.forceSyncAll) {
         return true;
      } else {
         ConfigManager.AnimalConfig ac = this.configManager.getAnimalConfig(type);
         String expectedIso = this.minutesToIso(ac.growthTimeMinutes * (double)30.0F);
         Path wildPatch = this.patchFolder.resolve("Growth_" + type.getModelAssetId() + ".json");
         if (!Files.exists(wildPatch, new LinkOption[0])) {
            return true;
         } else {
            try {
               String content = Files.readString(wildPatch);
               if (!content.contains(expectedIso)) {
                  return true;
               }
            } catch (IOException var8) {
               return true;
            }

            if (type.usesVanillaTaming() && type.getTamedBabyNpcRolePath() != null) {
               Path tamedPatch = this.patchFolder.resolve("Growth_Tamed_" + type.getModelAssetId() + ".json");
               if (!Files.exists(tamedPatch, new LinkOption[0])) {
                  return true;
               }

               try {
                  String content = Files.readString(tamedPatch);
                  if (!content.contains(expectedIso)) {
                     return true;
                  }
               } catch (IOException var7) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private List<String> readAttractiveItemSetFromPatch(Path patchFile) {
      try {
         String content = Files.readString(patchFile);
         int valueStart = content.indexOf("\"$.AttractiveItemSet\"");
         if (valueStart == -1) {
            valueStart = content.indexOf("\"AttractiveItemSet\"");
         }

         if (valueStart == -1) {
            valueStart = content.indexOf("\"Value\"");
         }

         if (valueStart == -1) {
            return Collections.emptyList();
         } else {
            int arrayStart = content.indexOf("[", valueStart);
            int arrayEnd = content.indexOf("]", arrayStart);
            if (arrayStart != -1 && arrayEnd != -1) {
               String arrayContent = content.substring(arrayStart + 1, arrayEnd);
               List<String> foods = new ArrayList();

               for(String item : arrayContent.split(",")) {
                  String trimmed = item.trim().replace("\"", "");
                  if (!trimmed.isEmpty()) {
                     foods.add(trimmed);
                  }
               }

               return foods;
            } else {
               return Collections.emptyList();
            }
         }
      } catch (IOException var13) {
         return Collections.emptyList();
      }
   }

   private String readBaseAssetPathFromPatch(Path patchFile) {
      try {
         String content = Files.readString(patchFile);
         String key = "\"_BaseAssetPath\"";
         int keyIdx = content.indexOf(key);
         if (keyIdx == -1) {
            return "";
         } else {
            int colonIdx = content.indexOf(":", keyIdx + key.length());
            if (colonIdx == -1) {
               return "";
            } else {
               int quoteStart = content.indexOf("\"", colonIdx + 1);
               int quoteEnd = content.indexOf("\"", quoteStart + 1);
               return quoteStart != -1 && quoteEnd != -1 ? content.substring(quoteStart + 1, quoteEnd) : "";
            }
         }
      } catch (IOException var8) {
         return "";
      }
   }

   private boolean foodsMatch(List<String> a, List<String> b) {
      if (a.size() != b.size()) {
         return false;
      } else {
         Set<String> setA = new HashSet(a);
         Set<String> setB = new HashSet(b);
         return setA.equals(setB);
      }
   }

   public void syncCreaturePatch(String name, String npcPath, List<String> attractiveItems, double breedCooldownMinutes, double growthMinutes) {
      if (this.patchFolder == null) {
         this.logWarning("PatchSyncService not initialized, cannot sync patch for " + name);
      } else {
         Path patchFile = this.patchFolder.resolve("NPC_" + name + ".json");
         String json = this.generateCombinedPatchJson(name, npcPath, attractiveItems, breedCooldownMinutes, growthMinutes);

         try {
            Files.writeString(patchFile, json);
            this.logVerbose("Synced patch: " + String.valueOf(patchFile.getFileName()));
         } catch (IOException e) {
            String var10001 = String.valueOf(patchFile);
            this.logWarning("Failed to write patch file " + var10001 + ": " + e.getMessage());
         }

      }
   }

   public void deletePatch(String name) {
      if (this.patchFolder != null) {
         Path patchFile = this.patchFolder.resolve("NPC_" + name + ".json");

         try {
            Files.deleteIfExists(patchFile);
            this.logVerbose("Deleted patch: " + String.valueOf(patchFile.getFileName()));
         } catch (IOException e) {
            String var10001 = String.valueOf(patchFile);
            this.logWarning("Failed to delete patch file " + var10001 + ": " + e.getMessage());
         }

      }
   }

   private String generateCombinedPatchJson(String modelAssetId, String npcPath, List<String> attractiveItems, double breedCooldownMinutes, double growthMinutes) {
      StringBuilder sb = new StringBuilder();
      sb.append("{\n");
      sb.append("    \"$Comment\": \"Auto-generated by HyTame - DO NOT EDIT\",\n");
      sb.append("    \"_BaseAssetPath\": \"Server/").append(npcPath).append(".json\",\n");
      sb.append("    \"_priority\": 200,\n");
      boolean useModify = !CANNOT_USE_MODIFY.contains(modelAssetId);
      if (useModify) {
         boolean hasNative = HAS_NATIVE_ATTRACTIVE_ITEM_SET.contains(modelAssetId);
         String modifyKey = hasNative ? "$.AttractiveItemSet" : "AttractiveItemSet";
         sb.append("    \"Modify\": {\n");
         sb.append("        \"").append(modifyKey).append("\": [");

         for(int i = 0; i < attractiveItems.size(); ++i) {
            if (i > 0) {
               sb.append(", ");
            }

            sb.append("\"").append((String)attractiveItems.get(i)).append("\"");
         }

         sb.append("]");
         if (growthMinutes > (double)0.0F) {
            String iso = this.minutesToIso(growthMinutes * (double)30.0F);
            sb.append(",\n        \"GrowthTimeout\": [\"").append(iso).append("\", \"").append(iso).append("\"]");
         }

         sb.append("\n    }");
      }

      boolean needsAttractiveItemSetParam = !useModify;
      boolean needsGrowthParam = !useModify && growthMinutes > (double)0.0F;
      boolean hasParams = breedCooldownMinutes > (double)0.0F || needsAttractiveItemSetParam || needsGrowthParam;
      if (hasParams) {
         if (useModify) {
            sb.append(",");
         }

         sb.append("\n    \"Parameters\": {");
         boolean first = true;
         if (needsAttractiveItemSetParam) {
            sb.append("\n        \"AttractiveItemSet\": {\n");
            sb.append("            \"Value\": [");

            for(int i = 0; i < attractiveItems.size(); ++i) {
               if (i > 0) {
                  sb.append(", ");
               }

               sb.append("\"").append((String)attractiveItems.get(i)).append("\"");
            }

            sb.append("],\n");
            sb.append("            \"TypeHint\": \"String\"\n");
            sb.append("        }");
            first = false;
         }

         if (breedCooldownMinutes > (double)0.0F) {
            String iso = this.minutesToIso(breedCooldownMinutes * (double)30.0F);
            if (!first) {
               sb.append(",");
            }

            sb.append("\n        \"BreedCooldownTimeout\": {\n");
            sb.append("            \"TypeHint\": \"TemporalAmountRange\",\n");
            sb.append("            \"Value\": [\"").append(iso).append("\", \"").append(iso).append("\"]\n");
            sb.append("        }");
            first = false;
         }

         if (needsGrowthParam) {
            String iso = this.minutesToIso(growthMinutes * (double)30.0F);
            if (!first) {
               sb.append(",");
            }

            sb.append("\n        \"GrowthTimeout\": {\n");
            sb.append("            \"TypeHint\": \"TemporalAmountRange\",\n");
            sb.append("            \"Value\": [\"").append(iso).append("\", \"").append(iso).append("\"]\n");
            sb.append("        }");
         }

         sb.append("\n    }");
      }

      sb.append("\n}\n");
      return sb.toString();
   }

   private List<String> getAllAttractiveItemSet(ConfigManager.AnimalConfig config) {
      if (!config.breedingEnabled && !config.tamingEnabled) {
         return Collections.emptyList();
      } else {
         Set<String> all = new LinkedHashSet();
         if (config.tamingEnabled) {
            all.addAll(config.getEffectiveTamingFoods());
         }

         if (config.breedingEnabled) {
            all.addAll(config.getEffectiveBreedingFoods());
         }

         return new ArrayList(all);
      }
   }

   public void syncForAnimal(AnimalType type) {
      String npcPath = type.getNpcRolePath();
      if (npcPath == null) {
         this.logVerbose("No NPC path for " + type.name() + ", skipping patch sync");
      } else {
         ConfigManager.AnimalConfig config = this.configManager.getAnimalConfig(type);
         if (type.usesVanillaTaming()) {
            this.syncLivestockPatches(type, config);
         } else {
            List<String> attractiveItems = this.getAllAttractiveItemSet(config);
            if (attractiveItems.isEmpty()) {
               this.deletePatch(type.getModelAssetId());
            } else {
               double breedCooldown = config.breedCooldownMinutes;
               double growthForAdult = type.hasBabyVariant() ? (double)-1.0F : config.growthTimeMinutes;
               this.syncCreaturePatch(type.getModelAssetId(), npcPath, attractiveItems, breedCooldown, growthForAdult);
            }

            if (type.hasBabyVariant()) {
               this.syncGrowthForAnimal(type);
            }

         }
      }
   }

   private boolean syncLivestockPatches(AnimalType type, ConfigManager.AnimalConfig config) {
      boolean anyWritten = false;
      List<String> tamingFoods = (List<String>)(config.tamingEnabled ? new ArrayList(config.getEffectiveTamingFoods()) : Collections.emptyList());
      Path wildPatch = this.patchFolder.resolve("NPC_" + type.getModelAssetId() + ".json");
      if (!tamingFoods.isEmpty()) {
         String wildJson = this.generateWildLivestockPatchJson(type, tamingFoods);

         try {
            if (this.forceSyncAll || !Files.exists(wildPatch, new LinkOption[0]) || !Files.readString(wildPatch).equals(wildJson)) {
               Files.writeString(wildPatch, wildJson);
               this.logVerbose("Synced wild livestock patch: " + String.valueOf(wildPatch.getFileName()));
               anyWritten = true;
            }
         } catch (IOException e) {
            this.logWarning("Failed to write wild livestock patch: " + e.getMessage());
         }
      } else {
         try {
            if (Files.deleteIfExists(wildPatch)) {
               this.logVerbose("Deleted stale wild livestock patch: " + String.valueOf(wildPatch.getFileName()));
               anyWritten = true;
            }
         } catch (IOException e) {
            this.logWarning("Failed to delete stale wild patch: " + e.getMessage());
         }
      }

      List<String> breedingFoods = (List<String>)(config.breedingEnabled ? new ArrayList(config.getEffectiveBreedingFoods()) : Collections.emptyList());
      Path tamedPatch = this.patchFolder.resolve("Tamed_" + type.getModelAssetId() + ".json");
      if (!breedingFoods.isEmpty()) {
         String tamedJson = this.generateTamedLivestockPatchJson(type, breedingFoods, config.breedCooldownMinutes);

         try {
            if (this.forceSyncAll || !Files.exists(tamedPatch, new LinkOption[0]) || !Files.readString(tamedPatch).equals(tamedJson)) {
               Files.writeString(tamedPatch, tamedJson);
               this.logVerbose("Synced tamed livestock patch: " + String.valueOf(tamedPatch.getFileName()));
               anyWritten = true;
            }
         } catch (IOException e) {
            this.logWarning("Failed to write tamed livestock patch: " + e.getMessage());
         }
      } else {
         try {
            if (Files.deleteIfExists(tamedPatch)) {
               this.logVerbose("Deleted stale tamed livestock patch: " + String.valueOf(tamedPatch.getFileName()));
               anyWritten = true;
            }
         } catch (IOException e) {
            this.logWarning("Failed to delete stale tamed patch: " + e.getMessage());
         }
      }

      if (type.hasBabyVariant() && config.growthTimeMinutes > (double)0.0F) {
         String adultPath = type.getNpcRolePath();
         String wildBabyPath = adultPath != null ? adultPath.substring(0, adultPath.lastIndexOf(47) + 1) + type.getBabyNpcRoleId() : null;
         if (wildBabyPath != null) {
            Path wildGrowthPatch = this.patchFolder.resolve("Growth_" + type.getModelAssetId() + ".json");
            String wildGrowthJson = this.generateGrowthPatchJson(wildBabyPath, config.growthTimeMinutes);

            try {
               if (this.forceSyncAll || !Files.exists(wildGrowthPatch, new LinkOption[0]) || !Files.readString(wildGrowthPatch).equals(wildGrowthJson)) {
                  Files.writeString(wildGrowthPatch, wildGrowthJson);
                  this.logVerbose("Synced wild growth patch: " + String.valueOf(wildGrowthPatch.getFileName()));
                  anyWritten = true;
               }
            } catch (IOException e) {
               this.logWarning("Failed to write wild growth patch: " + e.getMessage());
            }
         }

         String tamedBabyPath = type.getTamedBabyNpcRolePath();
         if (tamedBabyPath != null) {
            Path tamedGrowthPatch = this.patchFolder.resolve("Growth_Tamed_" + type.getModelAssetId() + ".json");
            String tamedGrowthJson = this.generateGrowthPatchJson(tamedBabyPath, config.growthTimeMinutes);

            try {
               if (this.forceSyncAll || !Files.exists(tamedGrowthPatch, new LinkOption[0]) || !Files.readString(tamedGrowthPatch).equals(tamedGrowthJson)) {
                  Files.writeString(tamedGrowthPatch, tamedGrowthJson);
                  this.logVerbose("Synced tamed growth patch: " + String.valueOf(tamedGrowthPatch.getFileName()));
                  anyWritten = true;
               }
            } catch (IOException e) {
               this.logWarning("Failed to write tamed growth patch: " + e.getMessage());
            }
         }
      }

      return anyWritten;
   }

   private String generateWildLivestockPatchJson(AnimalType type, List<String> tamingFoods) {
      StringBuilder sb = new StringBuilder();
      sb.append("{\n");
      sb.append("    \"$Comment\": \"Auto-generated by HyTame - DO NOT EDIT\",\n");
      sb.append("    \"_BaseAssetPath\": \"Server/").append(type.getNpcRolePath()).append(".json\",\n");
      sb.append("    \"_priority\": 200,\n");
      sb.append("    \"Modify\": {\n");
      sb.append("        \"$.AttractiveItemSet\": [");

      for(int i = 0; i < tamingFoods.size(); ++i) {
         if (i > 0) {
            sb.append(", ");
         }

         sb.append("\"").append((String)tamingFoods.get(i)).append("\"");
      }

      sb.append("]\n");
      sb.append("    }\n");
      sb.append("}\n");
      return sb.toString();
   }

   private String generateTamedLivestockPatchJson(AnimalType type, List<String> breedingFoods, double breedCooldownMinutes) {
      StringBuilder sb = new StringBuilder();
      sb.append("{\n");
      sb.append("    \"$Comment\": \"Auto-generated by HyTame - DO NOT EDIT\",\n");
      sb.append("    \"_BaseAssetPath\": \"Server/").append(type.getTamedNpcRolePath()).append(".json\",\n");
      sb.append("    \"_priority\": 200,\n");
      sb.append("    \"Modify\": {\n");
      sb.append("        \"AttractiveItemSet\": [");

      for(int i = 0; i < breedingFoods.size(); ++i) {
         if (i > 0) {
            sb.append(", ");
         }

         sb.append("\"").append((String)breedingFoods.get(i)).append("\"");
      }

      sb.append("],\n");
      sb.append("        \"$.AttractiveItemSet\": [\"Tool_Feedbag\"");

      for(String food : breedingFoods) {
         sb.append(", \"").append(food).append("\"");
      }

      sb.append("]\n    }");
      if (breedCooldownMinutes > (double)0.0F) {
         String iso = this.minutesToIso(breedCooldownMinutes * (double)30.0F);
         sb.append(",\n    \"Parameters\": {\n");
         sb.append("        \"BreedCooldownTimeout\": {\n");
         sb.append("            \"TypeHint\": \"TemporalAmountRange\",\n");
         sb.append("            \"Value\": [\"").append(iso).append("\", \"").append(iso).append("\"]\n");
         sb.append("        }\n    }");
      }

      sb.append("\n}\n");
      return sb.toString();
   }

   public void syncGrowthForAnimal(AnimalType type) {
      if (type.hasBabyVariant()) {
         ConfigManager.AnimalConfig config = this.configManager.getAnimalConfig(type);
         String adultPath = type.getNpcRolePath();
         String wildPath = adultPath != null ? adultPath.substring(0, adultPath.lastIndexOf(47) + 1) + type.getBabyNpcRoleId() : null;
         if (wildPath != null) {
            Path wildPatch = this.patchFolder.resolve("Growth_" + type.getModelAssetId() + ".json");
            String wildJson = this.generateGrowthPatchJson(wildPath, config.growthTimeMinutes);

            try {
               Files.writeString(wildPatch, wildJson);
               String var13 = String.valueOf(wildPatch.getFileName());
               this.logVerbose("Synced wild growth patch: " + var13 + " (" + config.growthTimeMinutes + " min)");
            } catch (IOException e) {
               String var10001 = String.valueOf(wildPatch);
               this.logWarning("Failed to write wild growth patch " + var10001 + ": " + e.getMessage());
            }
         } else {
            this.logVerbose("No NPC path for " + type.name() + " wild baby role, skipping growth patch sync");
         }

         String tamedPath = type.getTamedBabyNpcRolePath();
         if (tamedPath != null) {
            Path tamedPatch = this.patchFolder.resolve("Growth_Tamed_" + type.getModelAssetId() + ".json");
            String tamedJson = this.generateGrowthPatchJson(tamedPath, config.growthTimeMinutes);

            try {
               Files.writeString(tamedPatch, tamedJson);
               String var15 = String.valueOf(tamedPatch.getFileName());
               this.logVerbose("Synced tamed growth patch: " + var15 + " (" + config.growthTimeMinutes + " min)");
            } catch (IOException e) {
               String var14 = String.valueOf(tamedPatch);
               this.logWarning("Failed to write tamed growth patch " + var14 + ": " + e.getMessage());
            }
         }
      } else {
         this.syncForAnimal(type);
      }

   }

   private String generateGrowthPatchJson(String npcPath, double realMinutes) {
      double gameMinutes = realMinutes * (double)30.0F;
      String iso = this.minutesToIso(gameMinutes);
      StringBuilder sb = new StringBuilder();
      sb.append("{\n");
      sb.append("    \"$Comment\": \"Auto-generated by HyTame - DO NOT EDIT\",\n");
      sb.append("    \"_BaseAssetPath\": \"Server/").append(npcPath).append(".json\",\n");
      sb.append("    \"Modify\": {\n");
      sb.append("        \"GrowthTimeout\": [\"").append(iso).append("\", \"").append(iso).append("\"]\n");
      sb.append("    }\n");
      sb.append("}\n");
      return sb.toString();
   }

   private String minutesToIso(double minutes) {
      int totalSeconds = (int)Math.round(minutes * (double)60.0F);
      int hours = totalSeconds / 3600;
      int mins = totalSeconds % 3600 / 60;
      int secs = totalSeconds % 60;
      StringBuilder sb = new StringBuilder("PT");
      if (hours > 0) {
         sb.append(hours).append("H");
      }

      if (mins > 0) {
         sb.append(mins).append("M");
      }

      if (secs > 0) {
         sb.append(secs).append("S");
      }

      if (hours == 0 && mins == 0 && secs == 0) {
         sb.append("0S");
      }

      return sb.toString();
   }

   private String resolveTargetPath(String roleId, String configRolePath) {
      String resolved = EcsReflectionUtil.resolveNpcRolePath(roleId);
      return resolved != null ? resolved : configRolePath;
   }

   public void syncForCustomAnimal(CustomAnimalConfig custom) {
      String targetPath = this.resolveTargetPath(custom.getAdultNpcRoleId(), custom.getNpcRolePath());
      if (targetPath == null) {
         this.logVerbose("Cannot resolve NPC path for custom animal " + custom.getDisplayName() + ", skipping patch");
      } else {
         List<String> foods = custom.getBreedingFoods();
         if (foods.isEmpty()) {
            this.deletePatch(custom.getModelAssetId());
         } else {
            double breedCooldown = custom.getBreedCooldownMinutes();
            double growthForAdult = custom.hasBabyVariant() ? (double)-1.0F : custom.getGrowthTimeMinutes();
            this.syncCreaturePatch(custom.getModelAssetId(), targetPath, foods, breedCooldown, growthForAdult);
         }

         if (custom.hasBabyVariant()) {
            this.syncGrowthForCustomAnimal(custom);
         }

      }
   }

   public void syncGrowthForCustomAnimal(CustomAnimalConfig custom) {
      String roleId = custom.hasBabyVariant() ? custom.getBabyNpcRoleId() : custom.getAdultNpcRoleId();
      String targetPath = this.resolveTargetPath(roleId, custom.getNpcRolePath());
      if (targetPath == null) {
         this.logVerbose("Cannot resolve NPC path for custom animal " + custom.getDisplayName() + ", skipping growth patch");
      } else {
         double growthMinutes = custom.getGrowthTimeMinutes();
         Path patchFile = this.patchFolder.resolve("Growth_" + custom.getModelAssetId() + ".json");
         String json = this.generateGrowthPatchJson(targetPath, growthMinutes);

         try {
            Files.writeString(patchFile, json);
            String var10 = String.valueOf(patchFile.getFileName());
            this.logVerbose("Synced custom growth patch: " + var10 + " (" + growthMinutes + " min)");
         } catch (IOException e) {
            String var10001 = String.valueOf(patchFile);
            this.logWarning("Failed to write custom growth patch " + var10001 + ": " + e.getMessage());
         }

      }
   }

   private void logVerbose(String message) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null && HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[PatchSync] " + message);
      }

   }

   private void logWarning(String message) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null) {
         ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[PatchSync] " + message);
      }

   }
}
