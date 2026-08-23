package com.hytame.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.GrowthStage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class ConfigManager {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int CURRENT_CONFIG_VERSION = 2;
   private int configVersion = 1;
   private static final Map<String, String> ITEM_MIGRATIONS = Map.of("Bone", "Ingredient_Bone_Fragment", "Ectoplasm", "Ingredient_Void_Essence", "Gem_Crystal", "Ingredient_Crystal_Purple", "Ingot_Iron", "Ingredient_Bar_Iron", "Snowball", "Ingredient_Ice_Essence", "Void_Shard", "Ingredient_Void_Essence");
   private final Map<AnimalType, AnimalConfig> animalConfigs = new EnumMap(AnimalType.class);
   private final Map<String, CustomAnimalConfig> customAnimals = new HashMap();
   private double defaultGrowthTimeMinutes = (double)30.0F;
   private double defaultBreedCooldownMinutes = (double)5.0F;
   private boolean debugMode = false;
   private boolean growthEnabled = true;
   private boolean persistenceEnabled = true;
   private boolean usePerPlayerLimit = false;
   private int perPlayerTameLimit = 50;
   private boolean usePerClaimLimit = false;
   private int perClaimTameLimit = 30;
   private String activePreset = "default_extended";
   private int initializationGracePeriodSeconds = 15;
   private Set<String> tameableAnimalGroups = new HashSet(Arrays.asList("PreyBig", "PreySmall", "Livestock", "Critters"));
   private Path configFilePath;
   private Path presetsDirectory;
   private static final Pattern VALID_PRESET_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");
   private Set<UUID> tooltipSeenPlayers;

   private static double safeGetDouble(JsonObject json, String key, double defaultValue) {
      try {
         if (json != null && json.has(key)) {
            JsonElement elem = json.get(key);
            if (elem != null && !elem.isJsonNull()) {
               if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isNumber()) {
                  return elem.getAsDouble();
               } else {
                  return elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString() ? Double.parseDouble(elem.getAsString()) : defaultValue;
               }
            } else {
               return defaultValue;
            }
         } else {
            return defaultValue;
         }
      } catch (Exception var5) {
         return defaultValue;
      }
   }

   private static boolean safeGetBoolean(JsonObject json, String key, boolean defaultValue) {
      try {
         if (json != null && json.has(key)) {
            JsonElement elem = json.get(key);
            if (elem != null && !elem.isJsonNull()) {
               if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isBoolean()) {
                  return elem.getAsBoolean();
               } else if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                  String str = elem.getAsString().toLowerCase();
                  return "true".equals(str) || "1".equals(str) || "yes".equals(str);
               } else if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isNumber()) {
                  return elem.getAsInt() != 0;
               } else {
                  return defaultValue;
               }
            } else {
               return defaultValue;
            }
         } else {
            return defaultValue;
         }
      } catch (Exception var5) {
         return defaultValue;
      }
   }

   private static String safeGetString(JsonObject json, String key, String defaultValue) {
      try {
         if (json != null && json.has(key)) {
            JsonElement elem = json.get(key);
            return safeGetString(elem, defaultValue);
         } else {
            return defaultValue;
         }
      } catch (Exception var4) {
         return defaultValue;
      }
   }

   private static String safeGetString(JsonElement elem, String defaultValue) {
      try {
         if (elem != null && !elem.isJsonNull()) {
            return elem.isJsonPrimitive() ? elem.getAsString() : defaultValue;
         } else {
            return defaultValue;
         }
      } catch (Exception var3) {
         return defaultValue;
      }
   }

   public ConfigManager() {
      this.loadDefaults();
   }

   private void logVerbose(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   private void loadDefaults() {
      for(AnimalType type : AnimalType.values()) {
         List<String> defaultFoods = new ArrayList();
         defaultFoods.add(type.getDefaultBreedingFood());
         AnimalConfig config = new AnimalConfig(type.isLivestock(), defaultFoods, this.defaultGrowthTimeMinutes, this.defaultBreedCooldownMinutes);
         config.tamingEnabled = true;
         this.animalConfigs.put(type, config);
      }

   }

   public void loadFromFile(Path configPath) {
      this.configFilePath = configPath;
      this.presetsDirectory = configPath.getParent().resolve("presets");
      this.initializePresets();
      if (!Files.exists(configPath, new LinkOption[0])) {
         String var10001 = this.activePreset;
         this.logVerbose("Config file not found, creating with " + var10001 + " preset: " + String.valueOf(configPath));
         this.applyPreset(this.activePreset);
         this.saveToFile();
      } else {
         try {
            String json = Files.readString(configPath);
            this.loadFromJson(json);
            this.logVerbose("Loaded config from: " + String.valueOf(configPath));
            this.saveToFile();
         } catch (JsonSyntaxException e) {
            this.handleCorruptedConfig(configPath, "JSON syntax error: " + e.getMessage());
         } catch (JsonParseException e) {
            this.handleCorruptedConfig(configPath, "JSON parse error: " + e.getMessage());
         } catch (IOException e) {
            this.logVerbose("Error reading config file: " + e.getMessage() + ", using defaults");
            this.loadDefaults();
         } catch (Exception e) {
            this.handleCorruptedConfig(configPath, "Unexpected error: " + e.getMessage());
         }

      }
   }

   private void handleCorruptedConfig(Path configPath, String errorMessage) {
      this.logVerbose("Config file appears corrupted: " + errorMessage);

      try {
         Path backupPath = configPath.resolveSibling("config.json.corrupted");
         if (Files.exists(backupPath, new LinkOption[0])) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            backupPath = configPath.resolveSibling("config.json.corrupted." + timestamp);
         }

         Files.copy(configPath, backupPath);
         this.logVerbose("Backed up corrupted config to: " + String.valueOf(backupPath));
      } catch (Exception backupError) {
         this.logVerbose("Could not backup corrupted config: " + backupError.getMessage());
      }

      this.logVerbose("Resetting config to defaults");
      this.loadDefaults();
      this.applyPreset(this.activePreset);
      this.saveToFile();
   }

   private void initializePresets() {
      try {
         if (!Files.exists(this.presetsDirectory, new LinkOption[0])) {
            Files.createDirectories(this.presetsDirectory);
            this.logVerbose("Created presets directory: " + String.valueOf(this.presetsDirectory));
         }

         String[] builtinPresets = new String[]{"default", "default_extended", "lait_curated", "zoo", "all"};

         for(String presetName : builtinPresets) {
            Path presetFile = this.presetsDirectory.resolve(presetName + ".json");
            if (!Files.exists(presetFile, new LinkOption[0])) {
               this.saveBuiltinPresetToFile(presetName, presetFile);
               this.logVerbose("Created " + presetName + " preset file: " + String.valueOf(presetFile));
            } else {
               int added = this.updatePresetWithMissingAnimals(presetName, presetFile);
               if (added > 0) {
                  this.logVerbose("Updated " + presetName + " preset: added " + added + " new animals");
               }
            }
         }
      } catch (Exception e) {
         this.logVerbose("Error initializing presets: " + e.getMessage());
      }

   }

   private int updatePresetWithMissingAnimals(String presetName, Path presetFile) {
      try {
         String json = Files.readString(presetFile);
         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         JsonObject animals = root.has("animals") ? root.getAsJsonObject("animals") : new JsonObject();
         Map<AnimalType, AnimalConfig> presetDefaults = this.getBuiltinPresetConfigs(presetName);
         int addedCount = 0;
         boolean formatMigrated = false;

         for(AnimalType type : AnimalType.values()) {
            String key = type.name();
            if (animals.has(key)) {
               JsonObject animalJson = animals.getAsJsonObject(key);
               if (animalJson.has("enabled") && !animalJson.has("breedingEnabled")) {
                  boolean enabledValue = animalJson.get("enabled").getAsBoolean();
                  animalJson.remove("enabled");
                  animalJson.addProperty("breedingEnabled", enabledValue);
                  animalJson.addProperty("tamingEnabled", enabledValue);
                  formatMigrated = true;
               }
            } else {
               AnimalConfig defaultConfig = (AnimalConfig)presetDefaults.get(type);
               if (defaultConfig != null) {
                  JsonObject animalJson = new JsonObject();
                  animalJson.addProperty("breedingEnabled", defaultConfig.breedingEnabled);
                  animalJson.addProperty("tamingEnabled", defaultConfig.tamingEnabled);
                  JsonArray foodsArray = new JsonArray();

                  for(String food : defaultConfig.breedingFoods) {
                     foodsArray.add(food);
                  }

                  animalJson.add("breedingFoods", foodsArray);
                  animalJson.addProperty("growthTimeMinutes", defaultConfig.growthTimeMinutes);
                  animalJson.addProperty("breedCooldownMinutes", defaultConfig.breedCooldownMinutes);
                  animals.add(key, animalJson);
                  ++addedCount;
               }
            }
         }

         if (addedCount > 0 || formatMigrated) {
            root.add("animals", animals);
            Files.writeString(presetFile, GSON.toJson(root));
            if (formatMigrated) {
               this.logVerbose("Migrated " + presetName + " preset to new format (breedingEnabled/tamingEnabled)");
            }
         }

         return addedCount;
      } catch (Exception e) {
         this.logVerbose("Error updating preset " + presetName + ": " + e.getMessage());
         return 0;
      }
   }

   private Map<AnimalType, AnimalConfig> getBuiltinPresetConfigs(String presetName) {
      Map<AnimalType, AnimalConfig> originalConfigs = new EnumMap(AnimalType.class);

      for(Map.Entry<AnimalType, AnimalConfig> entry : this.animalConfigs.entrySet()) {
         AnimalConfig copy = new AnimalConfig();
         copy.breedingEnabled = ((AnimalConfig)entry.getValue()).breedingEnabled;
         copy.tamingEnabled = ((AnimalConfig)entry.getValue()).tamingEnabled;
         copy.breedingFoods = new ArrayList(((AnimalConfig)entry.getValue()).breedingFoods);
         copy.growthTimeMinutes = ((AnimalConfig)entry.getValue()).growthTimeMinutes;
         copy.breedCooldownMinutes = ((AnimalConfig)entry.getValue()).breedCooldownMinutes;
         originalConfigs.put((AnimalType)entry.getKey(), copy);
      }

      double originalGrowth = this.defaultGrowthTimeMinutes;
      double originalCooldown = this.defaultBreedCooldownMinutes;
      switch (presetName) {
         case "default" -> this.applyBuiltinDefaultPreset();
         case "default_extended" -> this.applyBuiltinDefaultExtendedPreset();
         case "lait_curated" -> this.applyBuiltinLaitCuratedPreset();
         case "zoo" -> this.applyBuiltinZooPreset();
         case "all" -> this.applyBuiltinAllPreset();
         case "_debug" -> this.applyBuiltinDebugPreset();
      }

      Map<AnimalType, AnimalConfig> presetConfigs = new EnumMap(AnimalType.class);

      for(Map.Entry<AnimalType, AnimalConfig> entry : this.animalConfigs.entrySet()) {
         AnimalConfig copy = new AnimalConfig();
         copy.breedingEnabled = ((AnimalConfig)entry.getValue()).breedingEnabled;
         copy.tamingEnabled = ((AnimalConfig)entry.getValue()).tamingEnabled;
         copy.breedingFoods = new ArrayList(((AnimalConfig)entry.getValue()).breedingFoods);
         copy.growthTimeMinutes = ((AnimalConfig)entry.getValue()).growthTimeMinutes;
         copy.breedCooldownMinutes = ((AnimalConfig)entry.getValue()).breedCooldownMinutes;
         presetConfigs.put((AnimalType)entry.getKey(), copy);
      }

      this.animalConfigs.clear();
      this.animalConfigs.putAll(originalConfigs);
      this.defaultGrowthTimeMinutes = originalGrowth;
      this.defaultBreedCooldownMinutes = originalCooldown;
      return presetConfigs;
   }

   public boolean restorePreset(String presetName) {
      if (!this.isBuiltinPreset(presetName)) {
         this.logVerbose("Cannot restore non-builtin preset: " + presetName);
         return false;
      } else {
         try {
            Path presetFile = this.presetsDirectory.resolve(presetName + ".json");
            this.saveBuiltinPresetToFile(presetName, presetFile);
            this.logVerbose("Restored " + presetName + " preset to default values");
            return true;
         } catch (Exception e) {
            this.logVerbose("Error restoring preset: " + e.getMessage());
            return false;
         }
      }
   }

   public boolean isBuiltinPreset(String presetName) {
      return presetName.equals("default") || presetName.equals("default_extended") || presetName.equals("lait_curated") || presetName.equals("zoo") || presetName.equals("all") || presetName.equals("_debug");
   }

   private void saveBuiltinPresetToFile(String presetName, Path filePath) throws IOException {
      Map<AnimalType, AnimalConfig> originalConfigs = new EnumMap(AnimalType.class);

      for(Map.Entry<AnimalType, AnimalConfig> entry : this.animalConfigs.entrySet()) {
         AnimalConfig copy = new AnimalConfig();
         copy.breedingEnabled = ((AnimalConfig)entry.getValue()).breedingEnabled;
         copy.tamingEnabled = ((AnimalConfig)entry.getValue()).tamingEnabled;
         copy.breedingFoods = new ArrayList(((AnimalConfig)entry.getValue()).breedingFoods);
         copy.growthTimeMinutes = ((AnimalConfig)entry.getValue()).growthTimeMinutes;
         copy.breedCooldownMinutes = ((AnimalConfig)entry.getValue()).breedCooldownMinutes;
         originalConfigs.put((AnimalType)entry.getKey(), copy);
      }

      double originalGrowth = this.defaultGrowthTimeMinutes;
      double originalCooldown = this.defaultBreedCooldownMinutes;
      if (presetName.equals("default")) {
         this.applyBuiltinDefaultPreset();
      } else if (presetName.equals("default_extended")) {
         this.applyBuiltinDefaultExtendedPreset();
      } else if (presetName.equals("lait_curated")) {
         this.applyBuiltinLaitCuratedPreset();
      } else if (presetName.equals("zoo")) {
         this.applyBuiltinZooPreset();
      } else if (presetName.equals("all")) {
         this.applyBuiltinAllPreset();
      } else if (presetName.equals("_debug")) {
         this.applyBuiltinDebugPreset();
      }

      String json = this.toJson();
      Files.writeString(filePath, json);
      this.animalConfigs.clear();
      this.animalConfigs.putAll(originalConfigs);
      this.defaultGrowthTimeMinutes = originalGrowth;
      this.defaultBreedCooldownMinutes = originalCooldown;
   }

   public void loadFromResource(String resourcePath) {
      try {
         InputStream is = this.getClass().getResourceAsStream(resourcePath);

         label54: {
            try {
               if (is == null) {
                  this.logVerbose("Config resource not found: " + resourcePath + ", using defaults");
                  break label54;
               }

               String json = new String(is.readAllBytes());
               this.loadFromJson(json);
               this.logVerbose("Loaded config from resource: " + resourcePath);
            } catch (Throwable var6) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (is != null) {
               is.close();
            }

            return;
         }

         if (is != null) {
            is.close();
         }

      } catch (Exception e) {
         this.logVerbose("Error loading config resource: " + e.getMessage() + ", using defaults");
      }
   }

   public void loadFromJson(String json) {
      if (json != null && !json.trim().isEmpty()) {
         try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed == null || !parsed.isJsonObject()) {
               this.logVerbose("Config is not a valid JSON object, using defaults");
               return;
            }

            JsonObject root = parsed.getAsJsonObject();
            this.activePreset = safeGetString(root, "activePreset", this.activePreset);
            if (root.has("defaults") && root.get("defaults").isJsonObject()) {
               JsonObject defaults = root.getAsJsonObject("defaults");
               this.defaultGrowthTimeMinutes = safeGetDouble(defaults, "growthTimeMinutes", this.defaultGrowthTimeMinutes);
               this.defaultBreedCooldownMinutes = safeGetDouble(defaults, "breedCooldownMinutes", this.defaultBreedCooldownMinutes);
               this.growthEnabled = safeGetBoolean(defaults, "growthEnabled", this.growthEnabled);
               this.persistenceEnabled = safeGetBoolean(defaults, "persistenceEnabled", this.persistenceEnabled);
               this.usePerPlayerLimit = safeGetBoolean(defaults, "usePerPlayerLimit", this.usePerPlayerLimit);
               this.perPlayerTameLimit = (int)safeGetDouble(defaults, "perPlayerTameLimit", (double)this.perPlayerTameLimit);
               this.usePerClaimLimit = safeGetBoolean(defaults, "usePerClaimLimit", this.usePerClaimLimit);
               this.perClaimTameLimit = (int)safeGetDouble(defaults, "perClaimTameLimit", (double)this.perClaimTameLimit);
               this.initializationGracePeriodSeconds = (int)safeGetDouble(defaults, "initializationGracePeriodSeconds", (double)this.initializationGracePeriodSeconds);
            }

            if (root.has("animals") && root.get("animals").isJsonObject()) {
               JsonObject animals = root.getAsJsonObject("animals");

               for(AnimalType type : AnimalType.values()) {
                  String key = type.name();
                  if (animals.has(key) && animals.get(key).isJsonObject()) {
                     JsonObject animalJson = animals.getAsJsonObject(key);
                     AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
                     if (config == null) {
                        config = new AnimalConfig();
                        this.animalConfigs.put(type, config);
                     }

                     if (animalJson.has("enabled")) {
                        boolean enabled = safeGetBoolean(animalJson, "enabled", true);
                        config.breedingEnabled = enabled;
                        config.tamingEnabled = enabled;
                     }

                     if (animalJson.has("breedingEnabled")) {
                        config.breedingEnabled = safeGetBoolean(animalJson, "breedingEnabled", config.breedingEnabled);
                     }

                     if (animalJson.has("tamingEnabled")) {
                        config.tamingEnabled = safeGetBoolean(animalJson, "tamingEnabled", config.tamingEnabled);
                     }

                     if (animalJson.has("breedingFoods") && animalJson.get("breedingFoods").isJsonArray()) {
                        config.breedingFoods.clear();

                        for(JsonElement elem : animalJson.getAsJsonArray("breedingFoods")) {
                           String food = safeGetString(elem, (String)null);
                           if (food != null) {
                              config.breedingFoods.add(food);
                           }
                        }
                     } else if (animalJson.has("breedingFood")) {
                        String food = safeGetString(animalJson, "breedingFood", (String)null);
                        if (food != null) {
                           config.breedingFoods.clear();
                           config.breedingFoods.add(food);
                        }
                     }

                     config.growthTimeMinutes = safeGetDouble(animalJson, "growthTimeMinutes", config.growthTimeMinutes > (double)0.0F ? config.growthTimeMinutes : this.defaultGrowthTimeMinutes);
                     config.breedCooldownMinutes = safeGetDouble(animalJson, "breedCooldownMinutes", config.breedCooldownMinutes > (double)0.0F ? config.breedCooldownMinutes : this.defaultBreedCooldownMinutes);
                  }
               }
            }

            if (root.has("customAnimals") && root.get("customAnimals").isJsonObject()) {
               this.customAnimals.clear();
               JsonObject customAnimalsJson = root.getAsJsonObject("customAnimals");

               for(String modelAssetId : customAnimalsJson.keySet()) {
                  try {
                     JsonObject customJson = customAnimalsJson.getAsJsonObject(modelAssetId);
                     List<String> foods = new ArrayList();
                     if (customJson.has("breedingFoods")) {
                        for(JsonElement elem : customJson.getAsJsonArray("breedingFoods")) {
                           foods.add(elem.getAsString());
                        }
                     }

                     String displayName = safeGetString(customJson, "displayName", modelAssetId);
                     double growthTime = safeGetDouble(customJson, "growthTimeMinutes", this.defaultGrowthTimeMinutes);
                     double breedCooldown = safeGetDouble(customJson, "breedCooldownMinutes", this.defaultBreedCooldownMinutes);
                     String babyNpcRole = safeGetString(customJson, "babyNpcRoleId", (String)null);
                     String adultNpcRole = safeGetString(customJson, "adultNpcRoleId", modelAssetId);
                     boolean mountable = safeGetBoolean(customJson, "mountable", false);
                     boolean legacyEnabled = safeGetBoolean(customJson, "enabled", true);
                     boolean breedingEnabled = customJson.has("breedingEnabled") ? safeGetBoolean(customJson, "breedingEnabled", true) : legacyEnabled;
                     boolean tamingEnabled = customJson.has("tamingEnabled") ? safeGetBoolean(customJson, "tamingEnabled", true) : legacyEnabled;
                     String npcRolePath = safeGetString(customJson, "npcRolePath", (String)null);
                     CustomAnimalConfig customConfig = new CustomAnimalConfig(modelAssetId, displayName, foods, growthTime, breedCooldown, babyNpcRole, adultNpcRole, mountable, breedingEnabled, tamingEnabled, npcRolePath);
                     this.customAnimals.put(modelAssetId, customConfig);
                     this.logVerbose("Loaded custom animal: " + modelAssetId);
                  } catch (Exception e) {
                     this.logVerbose("Error parsing custom animal " + modelAssetId + ": " + e.getMessage());
                  }
               }

               this.logVerbose("Loaded " + this.customAnimals.size() + " custom animals");
            }
         } catch (Exception e) {
            this.logVerbose("Error parsing config JSON: " + e.getMessage());
         }

         this.migrateInvalidItemIds();
      } else {
         this.logVerbose("Empty config JSON, using defaults");
      }
   }

   private void migrateInvalidItemIds() {
      for(AnimalConfig config : this.animalConfigs.values()) {
         this.migrateItemList(config.baseFoods);
         this.migrateItemList(config.breedingFoods);
         if (config.tamingFoods != null) {
            this.migrateItemList(config.tamingFoods);
         }
      }

   }

   private void migrateItemList(List<String> foods) {
      for(int i = 0; i < foods.size(); ++i) {
         String replacement = (String)ITEM_MIGRATIONS.get(foods.get(i));
         if (replacement != null) {
            foods.set(i, replacement);
         }
      }

   }

   public void saveToFile() {
      if (this.configFilePath == null) {
         this.logVerbose("No config file path set, cannot save");
      } else {
         try {
            String json = this.toJson();
            Files.createDirectories(this.configFilePath.getParent());
            Files.writeString(this.configFilePath, json);
            this.logVerbose("Saved config to: " + String.valueOf(this.configFilePath));
         } catch (Exception e) {
            this.logVerbose("Error saving config: " + e.getMessage());
         }

      }
   }

   public void reloadFromFile() {
      if (this.configFilePath == null) {
         this.logVerbose("No config file path set, cannot reload");
      } else {
         try {
            String json = Files.readString(this.configFilePath);
            this.loadFromJson(json);
            this.logVerbose("Reloaded config from: " + String.valueOf(this.configFilePath));
         } catch (Exception e) {
            this.logVerbose("Error reloading config: " + e.getMessage());
         }

      }
   }

   public String toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("activePreset", this.activePreset);
      JsonObject defaults = new JsonObject();
      defaults.addProperty("growthTimeMinutes", this.defaultGrowthTimeMinutes);
      defaults.addProperty("breedCooldownMinutes", this.defaultBreedCooldownMinutes);
      defaults.addProperty("growthEnabled", this.growthEnabled);
      defaults.addProperty("persistenceEnabled", this.persistenceEnabled);
      defaults.addProperty("usePerPlayerLimit", this.usePerPlayerLimit);
      defaults.addProperty("perPlayerTameLimit", this.perPlayerTameLimit);
      defaults.addProperty("usePerClaimLimit", this.usePerClaimLimit);
      defaults.addProperty("perClaimTameLimit", this.perClaimTameLimit);
      defaults.addProperty("initializationGracePeriodSeconds", this.initializationGracePeriodSeconds);
      root.add("defaults", defaults);
      JsonObject animals = new JsonObject();

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            JsonObject animalJson = new JsonObject();
            animalJson.addProperty("breedingEnabled", config.breedingEnabled);
            animalJson.addProperty("tamingEnabled", config.tamingEnabled);
            JsonArray foodsArray = new JsonArray();

            for(String food : config.breedingFoods) {
               foodsArray.add(food);
            }

            animalJson.add("breedingFoods", foodsArray);
            animalJson.addProperty("growthTimeMinutes", config.growthTimeMinutes);
            animalJson.addProperty("breedCooldownMinutes", config.breedCooldownMinutes);
            animals.add(type.name(), animalJson);
         }
      }

      root.add("animals", animals);
      if (!this.customAnimals.isEmpty()) {
         JsonObject customAnimalsJson = new JsonObject();

         for(CustomAnimalConfig custom : this.customAnimals.values()) {
            JsonObject customJson = new JsonObject();
            customJson.addProperty("breedingEnabled", custom.isBreedingEnabled());
            customJson.addProperty("tamingEnabled", custom.isTamingEnabled());
            customJson.addProperty("displayName", custom.getDisplayName());
            JsonArray foodsArray = new JsonArray();

            for(String food : custom.getBreedingFoods()) {
               foodsArray.add(food);
            }

            customJson.add("breedingFoods", foodsArray);
            customJson.addProperty("growthTimeMinutes", custom.getGrowthTimeMinutes());
            customJson.addProperty("breedCooldownMinutes", custom.getBreedCooldownMinutes());
            if (custom.getBabyNpcRoleId() != null) {
               customJson.addProperty("babyNpcRoleId", custom.getBabyNpcRoleId());
            }

            if (!custom.getAdultNpcRoleId().equals(custom.getModelAssetId())) {
               customJson.addProperty("adultNpcRoleId", custom.getAdultNpcRoleId());
            }

            if (custom.getNpcRolePath() != null) {
               customJson.addProperty("npcRolePath", custom.getNpcRolePath());
            }

            if (custom.isMountable()) {
               customJson.addProperty("mountable", true);
            }

            customAnimalsJson.add(custom.getModelAssetId(), customJson);
         }

         root.add("customAnimals", customAnimalsJson);
      }

      return GSON.toJson(root);
   }

   public List<String> getAvailablePresets() {
      List<String> presets = new ArrayList();
      if (this.presetsDirectory != null && Files.exists(this.presetsDirectory, new LinkOption[0])) {
         try {
            Files.list(this.presetsDirectory).filter((p) -> p.toString().endsWith(".json")).forEach((p) -> {
               String name = p.getFileName().toString();
               name = name.substring(0, name.length() - 5);
               presets.add(name);
            });
         } catch (Exception e) {
            this.logVerbose("Error listing presets: " + e.getMessage());
         }
      }

      if (!presets.contains("all")) {
         presets.add("all");
      }

      if (!presets.contains("default")) {
         presets.add("default");
      }

      if (!presets.contains("default_extended")) {
         presets.add("default_extended");
      }

      if (!presets.contains("lait_curated")) {
         presets.add("lait_curated");
      }

      if (!presets.contains("zoo")) {
         presets.add("zoo");
      }

      Collections.sort(presets);
      return presets;
   }

   public String getActivePreset() {
      return this.activePreset;
   }

   public boolean applyPreset(String presetName) {
      Path presetFile = this.presetsDirectory != null ? this.presetsDirectory.resolve(presetName + ".json") : null;
      if (presetFile != null && Files.exists(presetFile, new LinkOption[0])) {
         try {
            String json = Files.readString(presetFile);
            this.loadFromJson(json);
            this.activePreset = presetName;
            this.logVerbose("Applied preset from file: " + String.valueOf(presetFile));
            return true;
         } catch (Exception e) {
            this.logVerbose("Error loading preset file: " + e.getMessage());
         }
      }

      switch (presetName.toLowerCase()) {
         case "default":
            this.applyBuiltinDefaultPreset();
            this.activePreset = "default";
            return true;
         case "default_extended":
            this.applyBuiltinDefaultExtendedPreset();
            this.activePreset = "default_extended";
            return true;
         case "lait_curated":
            this.applyBuiltinLaitCuratedPreset();
            this.activePreset = "lait_curated";
            return true;
         case "zoo":
            this.applyBuiltinZooPreset();
            this.activePreset = "zoo";
            return true;
         case "all":
            this.applyBuiltinAllPreset();
            this.activePreset = "all";
            return true;
         case "_debug":
            this.applyBuiltinDebugPreset();
            this.activePreset = "_debug";
            return true;
         default:
            this.logVerbose("Preset not found: " + presetName);
            return false;
      }
   }

   public boolean isValidPresetName(String presetName) {
      if (presetName != null && !presetName.isEmpty()) {
         if (presetName.length() > 64) {
            return false;
         } else {
            return !presetName.contains("..") && !presetName.contains("/") && !presetName.contains("\\") ? VALID_PRESET_NAME.matcher(presetName).matches() : false;
         }
      } else {
         return false;
      }
   }

   public boolean saveAsPreset(String presetName) {
      if (this.presetsDirectory == null) {
         this.logVerbose("Presets directory not initialized");
         return false;
      } else if (!this.isValidPresetName(presetName)) {
         this.logVerbose("Invalid preset name: " + presetName + " (must be alphanumeric with underscores/hyphens only)");
         return false;
      } else {
         try {
            Path presetFile = this.presetsDirectory.resolve(presetName + ".json");
            if (!presetFile.normalize().startsWith(this.presetsDirectory.normalize())) {
               this.logVerbose("Security error: preset path escapes presets directory");
               return false;
            } else {
               String json = this.toJson();
               Files.writeString(presetFile, json);
               this.logVerbose("Saved preset: " + String.valueOf(presetFile));
               return true;
            }
         } catch (Exception e) {
            this.logVerbose("Error saving preset: " + e.getMessage());
            return false;
         }
      }
   }

   public boolean renamePreset(String oldName, String newName) {
      if (this.presetsDirectory == null) {
         this.logVerbose("Presets directory not initialized");
         return false;
      } else if (this.isBuiltinPreset(oldName)) {
         this.logVerbose("Cannot rename built-in preset: " + oldName);
         return false;
      } else if (!this.isValidPresetName(newName)) {
         this.logVerbose("Invalid new preset name: " + newName);
         return false;
      } else {
         try {
            Path oldFile = this.presetsDirectory.resolve(oldName + ".json");
            Path newFile = this.presetsDirectory.resolve(newName + ".json");
            if (oldFile.normalize().startsWith(this.presetsDirectory.normalize()) && newFile.normalize().startsWith(this.presetsDirectory.normalize())) {
               if (!Files.exists(oldFile, new LinkOption[0])) {
                  this.logVerbose("Preset file not found: " + String.valueOf(oldFile));
                  return false;
               } else if (Files.exists(newFile, new LinkOption[0])) {
                  this.logVerbose("Preset already exists: " + newName);
                  return false;
               } else {
                  Files.move(oldFile, newFile);
                  if (oldName.equals(this.activePreset)) {
                     this.activePreset = newName;
                  }

                  this.logVerbose("Renamed preset: " + oldName + " -> " + newName);
                  return true;
               }
            } else {
               this.logVerbose("Security error: preset path escapes presets directory");
               return false;
            }
         } catch (Exception e) {
            this.logVerbose("Error renaming preset: " + e.getMessage());
            return false;
         }
      }
   }

   public boolean copyPreset(String sourceName, String destName) {
      if (this.presetsDirectory == null) {
         this.logVerbose("Presets directory not initialized");
         return false;
      } else if (!this.isValidPresetName(destName)) {
         this.logVerbose("Invalid destination preset name: " + destName);
         return false;
      } else {
         try {
            Path srcFile = this.presetsDirectory.resolve(sourceName + ".json");
            Path dstFile = this.presetsDirectory.resolve(destName + ".json");
            if (srcFile.normalize().startsWith(this.presetsDirectory.normalize()) && dstFile.normalize().startsWith(this.presetsDirectory.normalize())) {
               if (!Files.exists(srcFile, new LinkOption[0])) {
                  this.logVerbose("Source preset not found: " + String.valueOf(srcFile));
                  return false;
               } else if (Files.exists(dstFile, new LinkOption[0])) {
                  this.logVerbose("Destination preset already exists: " + destName);
                  return false;
               } else {
                  Files.copy(srcFile, dstFile);
                  this.logVerbose("Copied preset: " + sourceName + " -> " + destName);
                  return true;
               }
            } else {
               this.logVerbose("Security error: preset path escapes presets directory");
               return false;
            }
         } catch (Exception e) {
            this.logVerbose("Error copying preset: " + e.getMessage());
            return false;
         }
      }
   }

   public Path getPresetsDirectory() {
      return this.presetsDirectory;
   }

   private void applyBuiltinDefaultPreset() {
      this.persistenceEnabled = true;
      this.defaultGrowthTimeMinutes = (double)30.0F;
      this.defaultBreedCooldownMinutes = (double)5.0F;

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config == null) {
            config = new AnimalConfig();
            this.animalConfigs.put(type, config);
         }

         boolean hasBaby = type.hasBabyVariant();
         config.tamingEnabled = hasBaby;
         config.breedingEnabled = hasBaby;
         config.breedingFoods.clear();
         config.breedingFoods.add(type.getDefaultBreedingFood());
         config.growthTimeMinutes = this.defaultGrowthTimeMinutes;
         config.breedCooldownMinutes = this.defaultBreedCooldownMinutes;
      }

   }

   private void applyBuiltinDefaultExtendedPreset() {
      this.persistenceEnabled = true;
      this.defaultGrowthTimeMinutes = (double)30.0F;
      this.defaultBreedCooldownMinutes = (double)5.0F;
      this.applyBuiltinLaitCuratedPreset();
      this.defaultGrowthTimeMinutes = (double)30.0F;
      this.defaultBreedCooldownMinutes = (double)5.0F;

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            AnimalType.Category cat = type.getCategory();
            boolean excluded = cat == AnimalType.Category.MISC || cat == AnimalType.Category.SPIRIT || cat == AnimalType.Category.BOSS || cat == AnimalType.Category.MYTHIC;
            config.tamingEnabled = !excluded;
            config.breedingEnabled = type.isLivestock() || cat == AnimalType.Category.MAMMAL || cat == AnimalType.Category.DINOSAUR || cat == AnimalType.Category.AVIAN;
            config.growthTimeMinutes = this.defaultGrowthTimeMinutes;
            config.breedCooldownMinutes = this.defaultBreedCooldownMinutes;
         }
      }

   }

   private void applyBuiltinLaitCuratedPreset() {
      this.persistenceEnabled = true;
      this.defaultGrowthTimeMinutes = (double)20.0F;
      this.defaultBreedCooldownMinutes = (double)3.0F;

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config == null) {
            config = new AnimalConfig();
            this.animalConfigs.put(type, config);
         }

         AnimalType.Category cat = type.getCategory();
         boolean excluded = cat == AnimalType.Category.MISC || cat == AnimalType.Category.SPIRIT || cat == AnimalType.Category.BOSS || cat == AnimalType.Category.MYTHIC;
         config.tamingEnabled = !excluded;
         config.breedingEnabled = !excluded;
         config.breedingFoods.clear();
         switch (type) {
            case COW:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Cauliflower_Item", "Plant_Crop_Lettuce_Item"));
               config.growthTimeMinutes = (double)25.0F;
               break;
            case PIG:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Carrot_Item", "Plant_Crop_Potato_Item", "Plant_Crop_Mushroom_Cap_Brown", "Plant_Fruit_Apple"));
               config.growthTimeMinutes = (double)15.0F;
               break;
            case CHICKEN:
            case CHICKEN_DESERT:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Corn_Item", "Plant_Crop_Wheat_Item", "Plant_Crop_Rice_Item"));
               config.growthTimeMinutes = (double)10.0F;
               break;
            case TURKEY:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Corn_Item", "Plant_Crop_Wheat_Item"));
               config.growthTimeMinutes = (double)15.0F;
               break;
            case SHEEP:
            case MOUFLON:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Lettuce_Item", "Plant_Crop_Cauliflower_Item"));
               config.growthTimeMinutes = (double)20.0F;
               break;
            case RAM:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Fruit_Apple"));
               config.growthTimeMinutes = (double)20.0F;
               break;
            case GOAT:
               config.breedingFoods.addAll(Arrays.asList("Plant_Fruit_Apple", "Plant_Crop_Wheat_Item", "Plant_Crop_Carrot_Item"));
               config.growthTimeMinutes = (double)18.0F;
               break;
            case HORSE:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Carrot_Item", "Plant_Fruit_Apple", "Plant_Crop_Wheat_Item"));
               config.growthTimeMinutes = (double)30.0F;
               break;
            case CAMEL:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Cactus_Flower"));
               config.growthTimeMinutes = (double)35.0F;
               break;
            case BISON:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Corn_Item"));
               config.growthTimeMinutes = (double)35.0F;
               break;
            case RABBIT:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Carrot_Item", "Plant_Crop_Lettuce_Item", "Plant_Fruit_Apple"));
               config.growthTimeMinutes = (double)8.0F;
               config.breedCooldownMinutes = (double)1.0F;
               break;
            case BOAR:
            case WARTHOG:
            case PIG_WILD:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Mushroom_Cap_Red", "Plant_Crop_Mushroom_Cap_Brown", "Plant_Fruit_Apple"));
               config.growthTimeMinutes = (double)18.0F;
               break;
            case SKRILL:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Corn_Item", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)20.0F;
               break;
            case WOLF:
            case WOLF_WHITE:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Cooked", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)25.0F;
               break;
            case FOX:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Raw", "Food_Fish_Raw"));
               config.growthTimeMinutes = (double)15.0F;
               break;
            case BEAR_GRIZZLY:
               config.breedingFoods.addAll(Arrays.asList("Plant_Fruit_Apple", "Food_Fish_Raw", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)45.0F;
               break;
            case BEAR_POLAR:
               config.breedingFoods.addAll(Arrays.asList("Food_Fish_Grilled", "Food_Fish_Raw", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)45.0F;
               break;
            case DEER_DOE:
            case DEER_STAG:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Carrot_Item", "Plant_Fruit_Apple", "Plant_Crop_Wheat_Item"));
               config.growthTimeMinutes = (double)25.0F;
               break;
            case MOOSE_BULL:
            case MOOSE_COW:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Lettuce_Item"));
               config.growthTimeMinutes = (double)40.0F;
               break;
            case FROG:
            case GECKO:
            case LIZARD_SAND:
               config.breedingFoods.add("Plant_Fruit_Berries_Red");
               config.growthTimeMinutes = (double)5.0F;
               break;
            case MEERKAT:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)5.0F;
               break;
            case MOUSE:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Corn_Item"));
               config.growthTimeMinutes = (double)3.0F;
               config.breedCooldownMinutes = (double)0.5F;
               break;
            case SQUIRREL:
               config.breedingFoods.addAll(Arrays.asList("Plant_Fruit_Apple", "Plant_Crop_Corn_Item"));
               config.growthTimeMinutes = (double)5.0F;
               break;
            case DUCK:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Corn_Item", "Plant_Crop_Wheat_Item"));
               config.growthTimeMinutes = (double)12.0F;
               break;
            case PIGEON:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Corn_Item"));
               config.growthTimeMinutes = (double)10.0F;
               break;
            case PENGUIN:
            case FLAMINGO:
               config.breedingFoods.addAll(Arrays.asList("Food_Fish_Raw"));
               config.growthTimeMinutes = (double)20.0F;
               break;
            case PARROT:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Corn_Item", "Plant_Fruit_Apple"));
               config.growthTimeMinutes = (double)15.0F;
               break;
            case CROW:
            case RAVEN:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Corn_Item", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)12.0F;
               break;
            case OWL_BROWN:
            case OWL_SNOW:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)18.0F;
               break;
            case TORTOISE:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Lettuce_Item", "Plant_Crop_Cauliflower_Item"));
               config.growthTimeMinutes = (double)60.0F;
               break;
            case CROCODILE:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)50.0F;
               break;
            case EMBERWULF:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Cooked", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)40.0F;
               break;
            case YETI:
            case FEN_STALKER:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)50.0F;
               break;
            case RAPTOR_CAVE:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Cooked", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)35.0F;
               break;
            case REX_CAVE:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Cooked", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)60.0F;
               config.breedCooldownMinutes = (double)10.0F;
               break;
            case ARCHAEOPTERYX:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Raw", "Plant_Fruit_Berries_Red"));
               config.growthTimeMinutes = (double)20.0F;
               break;
            case PTERODACTYL:
               config.breedingFoods.addAll(Arrays.asList("Food_Fish_Raw", "Food_Fish_Grilled", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)40.0F;
               break;
            case HYENA:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)20.0F;
               break;
            case ANTELOPE:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Lettuce_Item"));
               config.growthTimeMinutes = (double)20.0F;
               break;
            case ARMADILLO:
               config.breedingFoods.add("Plant_Fruit_Berries_Red");
               config.growthTimeMinutes = (double)15.0F;
               break;
            case LEOPARD_SNOW:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Raw", "Food_Wildmeat_Cooked"));
               config.growthTimeMinutes = (double)35.0F;
               break;
            case MOSSHORN:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Lettuce_Item"));
               config.growthTimeMinutes = (double)40.0F;
               break;
            case TIGER_SABERTOOTH:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Raw", "Food_Wildmeat_Cooked"));
               config.growthTimeMinutes = (double)45.0F;
               break;
            case BAT:
            case BAT_ICE:
               config.breedingFoods.add("Plant_Fruit_Berries_Red");
               config.growthTimeMinutes = (double)8.0F;
               break;
            case BLUEBIRD:
            case FINCH_GREEN:
            case SPARROW:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Fruit_Berries_Red"));
               config.growthTimeMinutes = (double)8.0F;
               break;
            case WOODPECKER:
               config.breedingFoods.add("Plant_Fruit_Berries_Red");
               config.growthTimeMinutes = (double)10.0F;
               break;
            case HAWK:
            case VULTURE:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)20.0F;
               break;
            case TETRABIRD:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Raw", "Food_Fish_Raw"));
               config.growthTimeMinutes = (double)25.0F;
               break;
            case TOAD_RHINO:
               config.breedingFoods.add("Plant_Fruit_Berries_Red");
               config.growthTimeMinutes = (double)30.0F;
               break;
            case TOAD_RHINO_MAGMA:
               config.breedingFoods.add("Food_Wildmeat_Cooked");
               config.growthTimeMinutes = (double)35.0F;
               break;
            case CACTEE:
               config.breedingFoods.add("Plant_Cactus_Flower");
               config.growthTimeMinutes = (double)20.0F;
               break;
            case HATWORM:
               config.breedingFoods.add("Plant_Crop_Mushroom_Cap_Brown");
               config.growthTimeMinutes = (double)15.0F;
               break;
            case SNAPDRAGON:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)25.0F;
               break;
            case SPARK_LIVING:
               config.breedingFoods.add("Plant_Crop_Corn_Item");
               config.growthTimeMinutes = (double)10.0F;
               break;
            case TRILLODON:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)40.0F;
               break;
            case RAT:
            case MOLERAT:
               config.breedingFoods.addAll(Arrays.asList("Plant_Crop_Wheat_Item", "Plant_Crop_Corn_Item"));
               config.growthTimeMinutes = (double)3.0F;
               config.breedCooldownMinutes = (double)0.5F;
               break;
            case LARVA_SILK:
               config.breedingFoods.add("Plant_Crop_Lettuce_Item");
               config.growthTimeMinutes = (double)5.0F;
               break;
            case SCORPION:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)10.0F;
               break;
            case SLUG_MAGMA:
            case SNAIL_MAGMA:
               config.breedingFoods.add("Food_Wildmeat_Cooked");
               config.growthTimeMinutes = (double)8.0F;
               break;
            case SNAIL_FROST:
               config.breedingFoods.add("Plant_Crop_Lettuce_Item");
               config.growthTimeMinutes = (double)8.0F;
               break;
            case SNAKE_COBRA:
            case SNAKE_MARSH:
            case SNAKE_RATTLE:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)12.0F;
               break;
            case SPIDER:
            case SPIDER_CAVE:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)10.0F;
               break;
            case BLUEGILL:
            case CATFISH:
            case FROSTGILL:
            case MINNOW:
            case PIKE:
            case SALMON:
            case SNAPJAW:
            case TROUT_RAINBOW:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)10.0F;
               break;
            case PIRANHA:
            case PIRANHA_BLACK:
               config.breedingFoods.add("Food_Wildmeat_Raw");
               config.growthTimeMinutes = (double)8.0F;
               break;
            case CLOWNFISH:
            case TANG_BLUE:
            case TANG_CHEVRON:
            case TANG_LEMON_PEEL:
            case TANG_SAILFIN:
            case PUFFERFISH:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)12.0F;
               break;
            case CRAB:
            case LOBSTER:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)15.0F;
               break;
            case JELLYFISH_BLUE:
            case JELLYFISH_CYAN:
            case JELLYFISH_GREEN:
            case JELLYFISH_RED:
            case JELLYFISH_YELLOW:
            case JELLYFISH_MAN_OF_WAR:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)10.0F;
               break;
            case EEL_MORAY:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)20.0F;
               break;
            case SHARK_HAMMERHEAD:
               config.breedingFoods.addAll(Arrays.asList("Food_Fish_Raw", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)60.0F;
               config.breedCooldownMinutes = (double)10.0F;
               break;
            case SHELLFISH_LAVA:
               config.breedingFoods.add("Food_Wildmeat_Cooked");
               config.growthTimeMinutes = (double)15.0F;
               break;
            case TRILOBITE:
            case TRILOBITE_BLACK:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)20.0F;
               break;
            case WHALE_HUMPBACK:
               config.breedingFoods.add("Food_Fish_Raw");
               config.growthTimeMinutes = (double)120.0F;
               config.breedCooldownMinutes = (double)30.0F;
               break;
            case DRAGON_FIRE:
               config.breedingFoods.addAll(Arrays.asList("Food_Wildmeat_Cooked", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)180.0F;
               config.breedCooldownMinutes = (double)60.0F;
               break;
            case DRAGON_FROST:
               config.breedingFoods.addAll(Arrays.asList("Food_Fish_Raw", "Food_Wildmeat_Raw"));
               config.growthTimeMinutes = (double)180.0F;
               config.breedCooldownMinutes = (double)60.0F;
               break;
            default:
               config.breedingFoods.add(type.getDefaultBreedingFood());
               config.growthTimeMinutes = this.defaultGrowthTimeMinutes;
         }

         if (config.breedCooldownMinutes == (double)0.0F) {
            config.breedCooldownMinutes = this.defaultBreedCooldownMinutes;
         }
      }

   }

   private void applyBuiltinZooPreset() {
      this.persistenceEnabled = true;
      this.applyBuiltinLaitCuratedPreset();

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            AnimalType.Category cat = type.getCategory();
            boolean excluded = cat == AnimalType.Category.MISC || cat == AnimalType.Category.SPIRIT || cat == AnimalType.Category.BOSS || cat == AnimalType.Category.MYTHIC;
            config.tamingEnabled = !excluded;
            config.breedingEnabled = type.isLivestock();
         }
      }

   }

   private void applyBuiltinAllPreset() {
      this.persistenceEnabled = true;
      this.applyBuiltinLaitCuratedPreset();

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            config.tamingEnabled = true;
            config.breedingEnabled = true;
         }
      }

   }

   private void applyBuiltinDebugPreset() {
      this.persistenceEnabled = true;
      String debugFood = "Plant_Crop_Wheat_Item";

      for(AnimalType type : AnimalType.values()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.computeIfAbsent(type, (k) -> new AnimalConfig());
         config.breedingEnabled = true;
         config.tamingEnabled = true;
         config.breedingFoods = new ArrayList(List.of(debugFood));
         config.growthTimeMinutes = 0.1;
         config.breedCooldownMinutes = 0.1;
      }

   }

   /** @deprecated */
   @Deprecated
   public boolean isAnimalEnabled(AnimalType type) {
      return this.isBreedingEnabled(type);
   }

   public boolean isBreedingEnabled(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      return config != null && config.breedingEnabled;
   }

   public boolean isTamingEnabled(AnimalType type) {
      if (type.usesVanillaTaming()) {
         return true;
      } else {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         return config != null && config.tamingEnabled;
      }
   }

   public CustomAnimalConfig getCustomAnimal(String modelAssetId) {
      return (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
   }

   public boolean isCustomAnimal(String modelAssetId) {
      return this.customAnimals.containsKey(modelAssetId);
   }

   /** @deprecated */
   @Deprecated
   public boolean isCustomAnimalEnabled(String modelAssetId) {
      CustomAnimalConfig custom = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      return custom != null && (custom.isBreedingEnabled() || custom.isTamingEnabled());
   }

   public boolean isCustomAnimalBreedingEnabled(String modelAssetId) {
      CustomAnimalConfig custom = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      return custom != null && custom.isBreedingEnabled();
   }

   public boolean isCustomAnimalTamingEnabled(String modelAssetId) {
      CustomAnimalConfig custom = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      return custom != null && custom.isTamingEnabled();
   }

   public Map<String, CustomAnimalConfig> getCustomAnimals() {
      return Collections.unmodifiableMap(this.customAnimals);
   }

   public AnimalLookupResult lookupAnimal(String id) {
      if (id != null && !id.isEmpty()) {
         AnimalType type = AnimalType.fromModelAssetId(id);
         if (type != null) {
            return new AnimalLookupResult(type, (CustomAnimalConfig)null);
         } else {
            CustomAnimalConfig custom = (CustomAnimalConfig)this.customAnimals.get(id);
            if (custom != null) {
               return new AnimalLookupResult((AnimalType)null, custom);
            } else {
               for(Map.Entry<String, CustomAnimalConfig> entry : this.customAnimals.entrySet()) {
                  if (((String)entry.getKey()).equalsIgnoreCase(id)) {
                     return new AnimalLookupResult((AnimalType)null, (CustomAnimalConfig)entry.getValue());
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   public boolean setAnyAnimalGrowthTime(String id, double minutes) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.setGrowthTime(result.getBuiltInType(), minutes);
         } else {
            this.setCustomAnimalGrowthTime(result.getCustomConfig().getModelAssetId(), minutes);
         }

         return true;
      }
   }

   public boolean setAnyAnimalCooldown(String id, double minutes) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.setBreedingCooldown(result.getBuiltInType(), minutes);
         } else {
            this.setCustomAnimalCooldown(result.getCustomConfig().getModelAssetId(), minutes);
         }

         return true;
      }
   }

   public boolean setAnyAnimalFood(String id, String food) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.setBreedingFood(result.getBuiltInType(), food);
         } else {
            String modelId = result.getCustomConfig().getModelAssetId();
            CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelId);
            if (existing != null) {
               List<String> newFoods = new ArrayList();
               newFoods.add(food);
               this.customAnimals.put(modelId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), newFoods, existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
            }
         }

         return true;
      }
   }

   public boolean addAnyAnimalFood(String id, String food) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.addBreedingFood(result.getBuiltInType(), food);
         } else {
            this.addCustomAnimalFood(result.getCustomConfig().getModelAssetId(), food);
         }

         return true;
      }
   }

   public boolean removeAnyAnimalFood(String id, String food) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.removeBreedingFood(result.getBuiltInType(), food);
         } else {
            this.removeCustomAnimalFood(result.getCustomConfig().getModelAssetId(), food);
         }

         return true;
      }
   }

   /** @deprecated */
   @Deprecated
   public boolean setAnyAnimalEnabled(String id, boolean enabled) {
      return this.setAnyAnimalBreedingEnabled(id, enabled);
   }

   public boolean setAnyAnimalBreedingEnabled(String id, boolean enabled) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.setBreedingEnabled(result.getBuiltInType(), enabled);
         } else {
            this.setCustomAnimalBreedingEnabled(result.getCustomConfig().getModelAssetId(), enabled);
         }

         return true;
      }
   }

   public boolean setAnyAnimalTamingEnabled(String id, boolean enabled) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         if (result.isBuiltIn()) {
            this.setTamingEnabled(result.getBuiltInType(), enabled);
         } else {
            this.setCustomAnimalTamingEnabled(result.getCustomConfig().getModelAssetId(), enabled);
         }

         return true;
      }
   }

   public List<String> getAnyAnimalFoods(String id) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return Collections.emptyList();
      } else {
         return result.isBuiltIn() ? this.getBreedingFoods(result.getBuiltInType()) : result.getCustomConfig().getBreedingFoods();
      }
   }

   /** @deprecated */
   @Deprecated
   public boolean isAnyAnimalEnabled(String id) {
      return this.isAnyAnimalBreedingEnabled(id) || this.isAnyAnimalTamingEnabled(id);
   }

   public boolean isAnyAnimalBreedingEnabled(String id) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         return result.isBuiltIn() ? this.isBreedingEnabled(result.getBuiltInType()) : result.getCustomConfig().isBreedingEnabled();
      }
   }

   public boolean isAnyAnimalTamingEnabled(String id) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return false;
      } else {
         return result.isBuiltIn() ? this.isTamingEnabled(result.getBuiltInType()) : result.getCustomConfig().isTamingEnabled();
      }
   }

   public double getAnyAnimalGrowthTimeMinutes(String id) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return this.defaultGrowthTimeMinutes;
      } else if (result.isBuiltIn()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(result.getBuiltInType());
         return config != null ? config.growthTimeMinutes : this.defaultGrowthTimeMinutes;
      } else {
         return result.getCustomConfig().getGrowthTimeMinutes();
      }
   }

   public double getAnyAnimalCooldownMinutes(String id) {
      AnimalLookupResult result = this.lookupAnimal(id);
      if (result == null) {
         return this.defaultBreedCooldownMinutes;
      } else if (result.isBuiltIn()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(result.getBuiltInType());
         return config != null ? config.breedCooldownMinutes : this.defaultBreedCooldownMinutes;
      } else {
         return result.getCustomConfig().getBreedCooldownMinutes();
      }
   }

   public long getCustomAnimalBreedingCooldown(String modelAssetId) {
      CustomAnimalConfig custom = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      return custom != null ? (long)(custom.getBreedCooldownMinutes() * (double)60.0F * (double)1000.0F) : (long)(this.defaultBreedCooldownMinutes * (double)60.0F * (double)1000.0F);
   }

   public long getCustomAnimalGrowthTime(String modelAssetId) {
      CustomAnimalConfig custom = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      return custom != null ? (long)(custom.getGrowthTimeMinutes() * (double)60.0F * (double)1000.0F) : (long)(this.defaultGrowthTimeMinutes * (double)60.0F * (double)1000.0F);
   }

   public String getBreedingFood(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      return config != null && !config.breedingFoods.isEmpty() ? (String)config.breedingFoods.get(0) : type.getDefaultBreedingFood();
   }

   public List<String> getBreedingFoods(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      return config != null && !config.breedingFoods.isEmpty() ? Collections.unmodifiableList(config.breedingFoods) : Collections.singletonList(type.getDefaultBreedingFood());
   }

   public boolean isBreedingFood(AnimalType type, String itemId) {
      if (itemId != null && type != null) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            for(String food : config.getEffectiveBreedingFoods()) {
               if (itemId.equalsIgnoreCase(food)) {
                  return true;
               }
            }

            return false;
         } else {
            return itemId.equalsIgnoreCase(type.getDefaultBreedingFood());
         }
      } else {
         return false;
      }
   }

   public List<String> getTamingFoods(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      return config != null ? config.getEffectiveTamingFoods() : Collections.singletonList(type.getDefaultBreedingFood());
   }

   public boolean isTamingFood(AnimalType type, String itemId) {
      if (itemId != null && type != null) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            for(String food : config.getEffectiveTamingFoods()) {
               if (itemId.equalsIgnoreCase(food)) {
                  return true;
               }
            }

            return false;
         } else {
            return itemId.equalsIgnoreCase(type.getDefaultBreedingFood());
         }
      } else {
         return false;
      }
   }

   public Set<String> getHealingFoods(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         return config.getAllHealingFoods();
      } else {
         Set<String> defaultFoods = new HashSet();
         defaultFoods.add(type.getDefaultBreedingFood());
         return defaultFoods;
      }
   }

   public boolean isHealingFood(AnimalType type, String itemId) {
      if (itemId != null && type != null) {
         for(String food : this.getHealingFoods(type)) {
            if (itemId.equalsIgnoreCase(food)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public Set<String> getTameableAnimalGroups() {
      return Collections.unmodifiableSet(this.tameableAnimalGroups);
   }

   public boolean isGroupTameable(String groupName) {
      return groupName != null && this.tameableAnimalGroups.contains(groupName);
   }

   public int getConfigVersion() {
      return this.configVersion;
   }

   public long getBreedingCooldown(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      double minutes = config != null ? config.breedCooldownMinutes : this.defaultBreedCooldownMinutes;
      return (long)(minutes * (double)60.0F * (double)1000.0F);
   }

   public long getGrowthTime(AnimalType type) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      double minutes = config != null && config.growthTimeMinutes > (double)0.0F ? config.growthTimeMinutes : this.defaultGrowthTimeMinutes;
      return (long)(minutes * (double)60.0F * (double)1000.0F);
   }

   public long getGrowthStageDuration(GrowthStage stage) {
      return stage == GrowthStage.ADULT ? 0L : (long)(this.defaultGrowthTimeMinutes * (double)60.0F * (double)1000.0F / (double)2.0F);
   }

   public long getGrowthStageDuration(AnimalType type, GrowthStage stage) {
      if (stage == GrowthStage.ADULT) {
         return 0L;
      } else {
         long totalGrowthTime = this.getGrowthTime(type);
         return totalGrowthTime / 2L;
      }
   }

   public long getGestationPeriod(AnimalType type) {
      return 0L;
   }

   public boolean isDebugMode() {
      return this.debugMode;
   }

   public void setDebugMode(boolean debugMode) {
      this.debugMode = debugMode;
   }

   public boolean isGrowthEnabled() {
      return this.growthEnabled;
   }

   public void setGrowthEnabled(boolean enabled) {
      this.growthEnabled = enabled;
   }

   public int getInitializationGracePeriodSeconds() {
      return this.initializationGracePeriodSeconds;
   }

   public boolean isPersistenceEnabled() {
      return this.persistenceEnabled;
   }

   public boolean isUsePerPlayerLimit() {
      return this.usePerPlayerLimit;
   }

   public void setUsePerPlayerLimit(boolean enabled) {
      this.usePerPlayerLimit = enabled;
   }

   public int getPerPlayerTameLimit() {
      return this.perPlayerTameLimit;
   }

   public void setPerPlayerTameLimit(int limit) {
      this.perPlayerTameLimit = limit;
   }

   public boolean isUsePerClaimLimit() {
      return this.usePerClaimLimit;
   }

   public void setUsePerClaimLimit(boolean enabled) {
      this.usePerClaimLimit = enabled;
   }

   public int getPerClaimTameLimit() {
      return this.perClaimTameLimit;
   }

   public void setPerClaimTameLimit(int limit) {
      this.perClaimTameLimit = limit;
   }

   /** @deprecated */
   @Deprecated
   public void setAnimalEnabled(AnimalType type, boolean enabled) {
      this.setBreedingEnabled(type, enabled);
   }

   public void setBreedingEnabled(AnimalType type, boolean enabled) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         config.breedingEnabled = enabled;
      }

   }

   public void setTamingEnabled(AnimalType type, boolean enabled) {
      if (!type.usesVanillaTaming()) {
         AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
         if (config != null) {
            config.tamingEnabled = enabled;
         }

      }
   }

   public void setBreedingFood(AnimalType type, String food) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         config.breedingFoods.clear();
         config.breedingFoods.add(food);
      }

   }

   public void setBreedingFoods(AnimalType type, List<String> foods) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         config.breedingFoods.clear();
         config.breedingFoods.addAll(foods);
      }

   }

   public void addBreedingFood(AnimalType type, String food) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null && !config.breedingFoods.contains(food)) {
         config.breedingFoods.add(food);
      }

   }

   public void removeBreedingFood(AnimalType type, String food) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         config.breedingFoods.remove(food);
      }

   }

   public void setBreedingCooldown(AnimalType type, double minutes) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         config.breedCooldownMinutes = minutes;
      }

   }

   public void setGrowthTime(AnimalType type, double minutes) {
      AnimalConfig config = (AnimalConfig)this.animalConfigs.get(type);
      if (config != null) {
         config.growthTimeMinutes = minutes;
      }

   }

   public double getDefaultGrowthTimeMinutes() {
      return this.defaultGrowthTimeMinutes;
   }

   public double getDefaultBreedCooldownMinutes() {
      return this.defaultBreedCooldownMinutes;
   }

   public void setDefaultGrowthTime(double minutes) {
      this.defaultGrowthTimeMinutes = minutes;

      for(AnimalConfig config : this.animalConfigs.values()) {
         config.growthTimeMinutes = minutes;
      }

   }

   public void setDefaultBreedCooldown(double minutes) {
      this.defaultBreedCooldownMinutes = minutes;

      for(AnimalConfig config : this.animalConfigs.values()) {
         config.breedCooldownMinutes = minutes;
      }

   }

   public AnimalConfig getAnimalConfig(AnimalType type) {
      return (AnimalConfig)this.animalConfigs.get(type);
   }

   public Map<AnimalType, AnimalConfig> getAllAnimalConfigs() {
      return new EnumMap(this.animalConfigs);
   }

   public CustomAnimalConfig addCustomAnimal(String modelAssetId, List<String> breedingFoods) {
      CustomAnimalConfig config = new CustomAnimalConfig(modelAssetId, modelAssetId, breedingFoods, this.defaultGrowthTimeMinutes, this.defaultBreedCooldownMinutes, (String)null, modelAssetId, false, true);
      this.customAnimals.put(modelAssetId, config);
      this.logVerbose("Added custom animal: " + modelAssetId + " with foods: " + String.valueOf(breedingFoods));
      return config;
   }

   public CustomAnimalConfig addCustomAnimal(String modelAssetId, String displayName, List<String> breedingFoods, double growthTimeMinutes, double breedCooldownMinutes, boolean mountable) {
      CustomAnimalConfig config = new CustomAnimalConfig(modelAssetId, displayName, breedingFoods, growthTimeMinutes, breedCooldownMinutes, (String)null, modelAssetId, mountable, true);
      this.customAnimals.put(modelAssetId, config);
      this.logVerbose("Added custom animal: " + modelAssetId);
      return config;
   }

   public boolean removeCustomAnimal(String modelAssetId) {
      CustomAnimalConfig removed = (CustomAnimalConfig)this.customAnimals.remove(modelAssetId);
      if (removed != null) {
         this.logVerbose("Removed custom animal: " + modelAssetId);
         return true;
      } else {
         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   public void setCustomAnimalEnabled(String modelAssetId, boolean enabled) {
      this.setCustomAnimalBreedingEnabled(modelAssetId, enabled);
      this.setCustomAnimalTamingEnabled(modelAssetId, enabled);
   }

   public void setCustomAnimalBreedingEnabled(String modelAssetId, boolean enabled) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), existing.getBreedingFoods(), existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), enabled, existing.isTamingEnabled(), existing.getNpcRolePath()));
      }

   }

   public void setCustomAnimalTamingEnabled(String modelAssetId, boolean enabled) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), existing.getBreedingFoods(), existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), enabled, existing.getNpcRolePath()));
      }

   }

   public void addCustomAnimalFood(String modelAssetId, String food) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         List<String> foods = new ArrayList(existing.getBreedingFoods());
         if (!foods.contains(food)) {
            foods.add(food);
            this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), foods, existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
         }
      }

   }

   public void removeCustomAnimalFood(String modelAssetId, String food) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         List<String> foods = new ArrayList(existing.getBreedingFoods());
         if (foods.remove(food)) {
            this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), foods, existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
         }
      }

   }

   public void setCustomAnimalNpcRole(String modelAssetId, String roleId) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), existing.getBreedingFoods(), existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), roleId, existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
         this.logVerbose("Set NPC role for " + modelAssetId + " to: " + roleId);
      }

   }

   public void setCustomAnimalBabyRole(String modelAssetId, String babyRoleId) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), existing.getBreedingFoods(), existing.getGrowthTimeMinutes(), existing.getBreedCooldownMinutes(), babyRoleId, existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
         this.logVerbose("Set baby NPC role for " + modelAssetId + " to: " + babyRoleId);
      }

   }

   public void setCustomAnimalGrowthTime(String modelAssetId, double growthTimeMinutes) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), existing.getBreedingFoods(), growthTimeMinutes, existing.getBreedCooldownMinutes(), existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
         this.logVerbose("Set growth time for " + modelAssetId + " to: " + growthTimeMinutes + " min");
      }

   }

   public void setCustomAnimalCooldown(String modelAssetId, double cooldownMinutes) {
      CustomAnimalConfig existing = (CustomAnimalConfig)this.customAnimals.get(modelAssetId);
      if (existing != null) {
         this.customAnimals.put(modelAssetId, new CustomAnimalConfig(existing.getModelAssetId(), existing.getDisplayName(), existing.getBreedingFoods(), existing.getGrowthTimeMinutes(), cooldownMinutes, existing.getBabyNpcRoleId(), existing.getAdultNpcRoleId(), existing.isMountable(), existing.isBreedingEnabled(), existing.isTamingEnabled(), existing.getNpcRolePath()));
         this.logVerbose("Set cooldown for " + modelAssetId + " to: " + cooldownMinutes + " min");
      }

   }

   public String getBreedingStartedMessage() {
      return this.formatMessage("&aBreeding started!");
   }

   public String getOnCooldownMessage() {
      return this.formatMessage("&cThis animal needs to rest before breeding again.");
   }

   public String getNotAdultMessage() {
      return this.formatMessage("&cThis animal is not old enough to breed.");
   }

   public String getWrongFoodMessage() {
      return this.formatMessage("&cThis animal doesn't want that food.");
   }

   public String getBirthMessage(AnimalType animalType) {
      return this.formatMessage("&bA baby " + animalType.getId() + " was born!");
   }

   public String getGrowthStageMessage(AnimalType animalType, GrowthStage stage) {
      String var10001 = animalType.getId();
      return this.formatMessage("&e" + var10001 + " has grown to " + stage.getDisplayName() + "!");
   }

   private String formatMessage(String message) {
      return message.replace("&", "§");
   }

   private Path getTooltipFile() {
      return this.configFilePath != null ? this.configFilePath.getParent().resolve("tooltip_seen.txt") : null;
   }

   private void loadTooltipSeen() {
      if (this.tooltipSeenPlayers == null) {
         this.tooltipSeenPlayers = new HashSet();
         Path file = this.getTooltipFile();
         if (file != null && Files.exists(file, new LinkOption[0])) {
            try {
               for(String line : Files.readAllLines(file)) {
                  String trimmed = line.trim();
                  if (!trimmed.isEmpty()) {
                     this.tooltipSeenPlayers.add(UUID.fromString(trimmed));
                  }
               }
            } catch (Exception e) {
               this.logVerbose("Error loading tooltip seen file: " + e.getMessage());
            }

         }
      }
   }

   public boolean hasSeenTooltip(UUID playerUuid) {
      this.loadTooltipSeen();
      return this.tooltipSeenPlayers.contains(playerUuid);
   }

   public void markTooltipSeen(UUID playerUuid) {
      this.loadTooltipSeen();
      if (this.tooltipSeenPlayers.add(playerUuid)) {
         Path file = this.getTooltipFile();
         if (file == null) {
            return;
         }

         try {
            List<String> lines = new ArrayList();

            for(UUID uuid : this.tooltipSeenPlayers) {
               lines.add(uuid.toString());
            }

            Files.write(file, lines);
         } catch (Exception e) {
            this.logVerbose("Error saving tooltip seen file: " + e.getMessage());
         }
      }

   }

   public static class AnimalConfig {
      public boolean breedingEnabled = true;
      public boolean tamingEnabled = true;
      public List<String> baseFoods = new ArrayList();
      public List<String> tamingFoods = null;
      public List<String> breedingFoods = new ArrayList();
      public double growthTimeMinutes;
      public double breedCooldownMinutes;

      public AnimalConfig() {
      }

      public AnimalConfig(boolean enabled, List<String> breedingFoods, double growthTimeMinutes, double breedCooldownMinutes) {
         this.breedingEnabled = enabled;
         this.tamingEnabled = enabled;
         this.breedingFoods = breedingFoods != null ? new ArrayList(breedingFoods) : new ArrayList();
         this.baseFoods = new ArrayList(this.breedingFoods);
         this.growthTimeMinutes = growthTimeMinutes;
         this.breedCooldownMinutes = breedCooldownMinutes;
      }

      public AnimalConfig(boolean enabled, String breedingFood, double growthTimeMinutes, double breedCooldownMinutes) {
         this.breedingEnabled = enabled;
         this.tamingEnabled = enabled;
         this.breedingFoods = new ArrayList();
         this.baseFoods = new ArrayList();
         if (breedingFood != null) {
            this.breedingFoods.add(breedingFood);
            this.baseFoods.add(breedingFood);
         }

         this.growthTimeMinutes = growthTimeMinutes;
         this.breedCooldownMinutes = breedCooldownMinutes;
      }

      public List<String> getEffectiveTamingFoods() {
         if (this.tamingFoods != null && !this.tamingFoods.isEmpty()) {
            return this.tamingFoods;
         } else {
            return this.baseFoods != null && !this.baseFoods.isEmpty() ? this.baseFoods : this.breedingFoods;
         }
      }

      public List<String> getEffectiveBreedingFoods() {
         if (this.breedingFoods != null && !this.breedingFoods.isEmpty()) {
            return this.breedingFoods;
         } else {
            return (List<String>)(this.baseFoods != null ? this.baseFoods : new ArrayList());
         }
      }

      public Set<String> getAllHealingFoods() {
         Set<String> all = new HashSet();
         if (this.baseFoods != null) {
            all.addAll(this.baseFoods);
         }

         if (this.tamingFoods != null) {
            all.addAll(this.tamingFoods);
         }

         if (this.breedingFoods != null) {
            all.addAll(this.breedingFoods);
         }

         return all;
      }
   }

   public static class AnimalLookupResult {
      private final AnimalType builtInType;
      private final CustomAnimalConfig customConfig;

      public AnimalLookupResult(AnimalType builtInType, CustomAnimalConfig customConfig) {
         this.builtInType = builtInType;
         this.customConfig = customConfig;
      }

      public boolean isBuiltIn() {
         return this.builtInType != null;
      }

      public boolean isCustom() {
         return this.customConfig != null;
      }

      public AnimalType getBuiltInType() {
         return this.builtInType;
      }

      public CustomAnimalConfig getCustomConfig() {
         return this.customConfig;
      }

      public String getId() {
         if (this.builtInType != null) {
            return this.builtInType.name();
         } else {
            return this.customConfig != null ? this.customConfig.getModelAssetId() : null;
         }
      }

      public String getDisplayName() {
         if (this.builtInType != null) {
            return this.builtInType.getModelAssetId();
         } else {
            return this.customConfig != null ? this.customConfig.getDisplayName() : null;
         }
      }
   }
}
