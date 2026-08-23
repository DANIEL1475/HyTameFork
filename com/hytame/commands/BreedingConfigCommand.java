package com.hytame.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hytame.HyTamePlugin;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.patch.PatchSyncService;
import com.hytame.util.ConfigManager;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BreedingConfigCommand extends AbstractCommand {
   private static final String PERM_ADMIN = "hytame.admin.";
   private static final Map<String, String> FOOD_SHORTCUTS = new HashMap();
   private static final Map<String, String> FOOD_DISPLAY_NAMES;

   public BreedingConfigCommand() {
      super("breedconfig", "[Deprecated] Manage breeding configuration - Use /hytame config instead");
      this.addSubCommand(new ReloadSubCommand());
      this.addSubCommand(new SaveSubCommand());
      this.addSubCommand(new ListSubCommand());
      this.addSubCommand(new InfoSubCommand());
      this.addSubCommand(new EnableSubCommand());
      this.addSubCommand(new DisableSubCommand());
      this.addSubCommand(new SetSubCommand());
      this.addSubCommand(new AddFoodSubCommand());
      this.addSubCommand(new RemoveFoodSubCommand());
      this.addSubCommand(new PresetSubCommand());
   }

   protected CompletableFuture<Void> execute(CommandContext ctx) {
      ctx.sendMessage(Message.raw("[Deprecated] Use /hytame config instead").color("#FFAA00"));
      ctx.sendMessage(Message.raw(""));
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin == null) {
         ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
         return CompletableFuture.completedFuture((Object)null);
      } else {
         ConfigManager config = plugin.getConfigManager();
         showConfigSummary(ctx, config);
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static boolean checkAdminDenied(CommandContext ctx) {
      Player player = CommandUtil.player(ctx);
      if (player instanceof Player && !HytamePermissions.hasAdminAccess(player)) {
         ctx.sendMessage(Message.raw("This command requires admin permissions.").color("#FF5555"));
         return true;
      } else {
         return false;
      }
   }

   private static ConfigManager getConfig() {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      return plugin != null ? plugin.getConfigManager() : null;
   }

   private static void syncPatchForAnimal(ConfigManager.AnimalLookupResult lookup) {
      if (lookup != null) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            PatchSyncService patchSyncService = plugin.getPatchSyncService();
            if (patchSyncService != null) {
               if (lookup.isBuiltIn()) {
                  patchSyncService.syncForAnimal(lookup.getBuiltInType());
               } else {
                  patchSyncService.syncForCustomAnimal(lookup.getCustomConfig());
               }

            }
         }
      }
   }

   private static void syncGrowthPatchForAnimal(ConfigManager.AnimalLookupResult lookup) {
      if (lookup != null) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            PatchSyncService patchSyncService = plugin.getPatchSyncService();
            if (patchSyncService != null) {
               if (lookup.isBuiltIn()) {
                  patchSyncService.syncGrowthForAnimal(lookup.getBuiltInType());
               } else {
                  patchSyncService.syncGrowthForCustomAnimal(lookup.getCustomConfig());
               }

            }
         }
      }
   }

   private static void syncAllPatches() {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null) {
         PatchSyncService patchSyncService = plugin.getPatchSyncService();
         if (patchSyncService != null) {
            patchSyncService.syncAllPatches();
         }
      }
   }

   private static void showConfigSummary(CommandContext ctx, ConfigManager config) {
      ctx.sendMessage(Message.raw("=== Breeding Config ===").color("#FF9900"));
      ctx.sendMessage(Message.raw("Active Preset: ").color("#AAAAAA").insert(Message.raw(config.getActivePreset()).color("#FFFFFF")));
      Map<AnimalType.Category, int[]> counts = new EnumMap(AnimalType.Category.class);

      for(AnimalType.Category cat : AnimalType.Category.values()) {
         counts.put(cat, new int[]{0, 0});
      }

      for(AnimalType type : AnimalType.values()) {
         int[] c = (int[])counts.get(type.getCategory());
         int var10002 = c[1]++;
         if (config.isAnimalEnabled(type)) {
            var10002 = c[0]++;
         }
      }

      ctx.sendMessage(Message.raw("Categories:").color("#AAAAAA"));

      for(AnimalType.Category cat : AnimalType.Category.values()) {
         int[] c = (int[])counts.get(cat);
         String hexColor = c[0] > 0 ? "#55FF55" : "#AAAAAA";
         ctx.sendMessage(Message.raw("  ").insert(Message.raw(cat.name()).color(hexColor)).insert(Message.raw(": " + c[0] + "/" + c[1] + " enabled").color("#AAAAAA")));
      }

      ctx.sendMessage(Message.raw(""));
      ctx.sendMessage(Message.raw("Type ").color("#AAAAAA").insert(Message.raw("/hytame config").color("#FFFFFF")).insert(Message.raw(" and press TAB for commands").color("#AAAAAA")));
   }

   private static void handleToggle(CommandContext ctx, ConfigManager config, String target, boolean enable) {
      String statusColor = enable ? "#55FF55" : "#FF5555";
      String statusWord = enable ? "Enabled" : "Disabled";
      String featureText = enable ? " breeding+taming for " : " breeding for ";
      if (target.equalsIgnoreCase("ALL")) {
         for(AnimalType type : AnimalType.values()) {
            config.setAnimalEnabled(type, enable);
            if (enable) {
               config.setTamingEnabled(type, true);
            }
         }

         for(String customId : config.getCustomAnimals().keySet()) {
            config.setCustomAnimalEnabled(customId, enable);
            if (enable) {
               config.setCustomAnimalTamingEnabled(customId, true);
            }
         }

         ctx.sendMessage(Message.raw(statusWord).color(statusColor).insert(Message.raw(featureText + "ALL animals (including custom).").color("#AAAAAA")));
         syncAllPatches();
         if (enable) {
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null) {
               plugin.autoSetupNearbyAnimals();
               ctx.sendMessage(Message.raw("Rescanned nearby animals for interaction setup.").color("#AAAAAA"));
            }
         }

      } else {
         try {
            AnimalType.Category cat = AnimalType.Category.valueOf(target.toUpperCase());
            int count = 0;

            for(AnimalType type : AnimalType.values()) {
               if (type.getCategory() == cat) {
                  config.setAnimalEnabled(type, enable);
                  if (enable) {
                     config.setTamingEnabled(type, true);
                  }

                  ++count;
               }
            }

            ctx.sendMessage(Message.raw(statusWord).color(statusColor).insert(Message.raw(featureText + count + " " + cat.name() + " animals.").color("#AAAAAA")));
            syncAllPatches();
            if (enable) {
               HyTamePlugin plugin = HyTamePlugin.getInstance();
               if (plugin != null) {
                  plugin.autoSetupNearbyAnimals();
                  ctx.sendMessage(Message.raw("Rescanned nearby animals for interaction setup.").color("#AAAAAA"));
               }
            }

         } catch (IllegalArgumentException var13) {
            ConfigManager.AnimalLookupResult lookup = config.lookupAnimal(target);
            if (lookup != null) {
               config.setAnyAnimalEnabled(target, enable);
               if (enable) {
                  config.setAnyAnimalTamingEnabled(target, true);
               }

               syncPatchForAnimal(lookup);
               ctx.sendMessage(Message.raw(statusWord).color(statusColor).insert(Message.raw(featureText).color("#AAAAAA")).insert(Message.raw(lookup.getDisplayName()).color("#FFFFFF")));
               if (enable) {
                  HyTamePlugin plugin = HyTamePlugin.getInstance();
                  if (plugin != null) {
                     plugin.autoSetupNearbyAnimals();
                     ctx.sendMessage(Message.raw("Rescanned nearby animals for interaction setup.").color("#AAAAAA"));
                  }
               }

            } else {
               ctx.sendMessage(Message.raw("Unknown animal or category: ").color("#FF5555").insert(Message.raw(target).color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Animals: COW, PIG, CHICKEN, or custom animal names").color("#AAAAAA"));
               ctx.sendMessage(Message.raw("Categories: ").color("#AAAAAA").insert(Message.raw(Arrays.toString(AnimalType.Category.values())).color("#FFFFFF")));
            }
         }
      }
   }

   private static void handleTamingToggle(CommandContext ctx, ConfigManager config, String target, boolean enable) {
      String statusColor = enable ? "#55FF55" : "#FF5555";
      String statusWord = enable ? "Enabled" : "Disabled";
      if (!target.equalsIgnoreCase("ALL")) {
         try {
            AnimalType.Category cat = AnimalType.Category.valueOf(target.toUpperCase());
            int count = 0;

            for(AnimalType type : AnimalType.values()) {
               if (type.getCategory() == cat) {
                  config.setTamingEnabled(type, enable);
                  ++count;
               }
            }

            ctx.sendMessage(Message.raw(statusWord).color(statusColor).insert(Message.raw(" taming for " + count + " " + cat.name() + " animals.").color("#AAAAAA")));
            syncAllPatches();
         } catch (IllegalArgumentException var12) {
            ConfigManager.AnimalLookupResult lookup = config.lookupAnimal(target);
            if (lookup != null) {
               config.setAnyAnimalTamingEnabled(target, enable);
               syncPatchForAnimal(lookup);
               ctx.sendMessage(Message.raw(statusWord).color(statusColor).insert(Message.raw(" taming for ").color("#AAAAAA")).insert(Message.raw(lookup.getDisplayName()).color("#FFFFFF")));
            } else {
               ctx.sendMessage(Message.raw("Unknown animal or category: ").color("#FF5555").insert(Message.raw(target).color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Animals: COW, PIG, CHICKEN, or custom animal names").color("#AAAAAA"));
               ctx.sendMessage(Message.raw("Categories: ").color("#AAAAAA").insert(Message.raw(Arrays.toString(AnimalType.Category.values())).color("#FFFFFF")));
            }

         }
      } else {
         for(AnimalType type : AnimalType.values()) {
            config.setTamingEnabled(type, enable);
         }

         for(String customId : config.getCustomAnimals().keySet()) {
            config.setCustomAnimalTamingEnabled(customId, enable);
         }

         ctx.sendMessage(Message.raw(statusWord).color(statusColor).insert(Message.raw(" taming for ALL animals (including custom).").color("#AAAAAA")));
         syncAllPatches();
      }
   }

   public static String resolveFoodShortcut(String input) {
      if (input == null) {
         return null;
      } else {
         String resolved = (String)FOOD_SHORTCUTS.get(input.toLowerCase());
         return resolved != null ? resolved : input;
      }
   }

   public static String getFoodDisplayName(String itemId) {
      if (itemId == null) {
         return "Unknown";
      } else {
         String displayName = (String)FOOD_DISPLAY_NAMES.get(itemId);
         if (displayName != null) {
            return displayName;
         } else {
            String name = itemId;
            if (itemId.startsWith("Plant_Crop_")) {
               name = itemId.substring(11);
            } else if (itemId.startsWith("Plant_Fruit_")) {
               name = itemId.substring(12);
            } else if (itemId.startsWith("Plant_Seed_")) {
               name = itemId.substring(11);
            } else if (itemId.startsWith("Plant_")) {
               name = itemId.substring(6);
            } else if (itemId.startsWith("Food_")) {
               name = itemId.substring(5);
            }

            if (name.endsWith("_Item")) {
               name = name.substring(0, name.length() - 5);
            }

            name = name.replace("_", " ");
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = true;

            for(char c : name.toCharArray()) {
               if (c == ' ') {
                  result.append(c);
                  capitalizeNext = true;
               } else if (capitalizeNext) {
                  result.append(Character.toUpperCase(c));
                  capitalizeNext = false;
               } else {
                  result.append(Character.toLowerCase(c));
               }
            }

            return result.toString();
         }
      }
   }

   public static String getFoodDisplayList(List<String> foods) {
      return foods != null && !foods.isEmpty() ? (String)foods.stream().map(BreedingConfigCommand::getFoodDisplayName).collect(Collectors.joining(", ")) : "(none)";
   }

   static {
      FOOD_SHORTCUTS.put("carrot", "Plant_Crop_Carrot_Item");
      FOOD_SHORTCUTS.put("wheat", "Plant_Crop_Wheat_Item");
      FOOD_SHORTCUTS.put("corn", "Plant_Crop_Corn_Item");
      FOOD_SHORTCUTS.put("potato", "Plant_Crop_Potato_Item");
      FOOD_SHORTCUTS.put("lettuce", "Plant_Crop_Lettuce_Item");
      FOOD_SHORTCUTS.put("cauliflower", "Plant_Crop_Cauliflower_Item");
      FOOD_SHORTCUTS.put("rice", "Plant_Crop_Rice_Item");
      FOOD_SHORTCUTS.put("mushroom_brown", "Plant_Crop_Mushroom_Cap_Brown");
      FOOD_SHORTCUTS.put("mushroom_red", "Plant_Crop_Mushroom_Cap_Red");
      FOOD_SHORTCUTS.put("brown_mushroom", "Plant_Crop_Mushroom_Cap_Brown");
      FOOD_SHORTCUTS.put("red_mushroom", "Plant_Crop_Mushroom_Cap_Red");
      FOOD_SHORTCUTS.put("apple", "Plant_Fruit_Apple");
      FOOD_SHORTCUTS.put("berries", "Plant_Fruit_Berries_Red");
      FOOD_SHORTCUTS.put("red_berries", "Plant_Fruit_Berries_Red");
      FOOD_SHORTCUTS.put("cactus_flower", "Plant_Cactus_Flower");
      FOOD_SHORTCUTS.put("wildmeat", "Food_Wildmeat_Raw");
      FOOD_SHORTCUTS.put("wildmeat_raw", "Food_Wildmeat_Raw");
      FOOD_SHORTCUTS.put("wildmeat_cooked", "Food_Wildmeat_Cooked");
      FOOD_SHORTCUTS.put("cooked_wildmeat", "Food_Wildmeat_Cooked");
      FOOD_SHORTCUTS.put("meat", "Food_Wildmeat_Raw");
      FOOD_SHORTCUTS.put("meat_raw", "Food_Wildmeat_Raw");
      FOOD_SHORTCUTS.put("raw_meat", "Food_Wildmeat_Raw");
      FOOD_SHORTCUTS.put("meat_cooked", "Food_Wildmeat_Cooked");
      FOOD_SHORTCUTS.put("cooked_meat", "Food_Wildmeat_Cooked");
      FOOD_SHORTCUTS.put("beef", "Food_Beef_Raw");
      FOOD_SHORTCUTS.put("beef_raw", "Food_Beef_Raw");
      FOOD_SHORTCUTS.put("pork", "Food_Pork_Raw");
      FOOD_SHORTCUTS.put("pork_raw", "Food_Pork_Raw");
      FOOD_SHORTCUTS.put("chicken_meat", "Food_Chicken_Raw");
      FOOD_SHORTCUTS.put("chicken_raw", "Food_Chicken_Raw");
      FOOD_SHORTCUTS.put("fish", "Food_Fish_Raw");
      FOOD_SHORTCUTS.put("fish_raw", "Food_Fish_Raw");
      FOOD_SHORTCUTS.put("raw_fish", "Food_Fish_Raw");
      FOOD_SHORTCUTS.put("fish_grilled", "Food_Fish_Grilled");
      FOOD_SHORTCUTS.put("grilled_fish", "Food_Fish_Grilled");
      FOOD_SHORTCUTS.put("fish_cooked", "Food_Fish_Grilled");
      FOOD_SHORTCUTS.put("cooked_fish", "Food_Fish_Grilled");
      FOOD_DISPLAY_NAMES = new HashMap();
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Carrot_Item", "Carrot");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Wheat_Item", "Wheat");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Corn_Item", "Corn");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Potato_Item", "Potato");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Lettuce_Item", "Lettuce");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Cauliflower_Item", "Cauliflower");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Rice_Item", "Rice");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Beetroot_Item", "Beetroot");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Mushroom_Cap_Brown", "Brown Mushroom");
      FOOD_DISPLAY_NAMES.put("Plant_Crop_Mushroom_Cap_Red", "Red Mushroom");
      FOOD_DISPLAY_NAMES.put("Plant_Fruit_Apple", "Apple");
      FOOD_DISPLAY_NAMES.put("Plant_Fruit_Berries_Red", "Red Berries");
      FOOD_DISPLAY_NAMES.put("Plant_Cactus_Flower", "Cactus Flower");
      FOOD_DISPLAY_NAMES.put("Food_Wildmeat_Raw", "Raw Meat");
      FOOD_DISPLAY_NAMES.put("Food_Wildmeat_Cooked", "Cooked Meat");
      FOOD_DISPLAY_NAMES.put("Food_Beef_Raw", "Raw Beef");
      FOOD_DISPLAY_NAMES.put("Food_Beef_Cooked", "Cooked Beef");
      FOOD_DISPLAY_NAMES.put("Food_Pork_Raw", "Raw Pork");
      FOOD_DISPLAY_NAMES.put("Food_Pork_Cooked", "Cooked Pork");
      FOOD_DISPLAY_NAMES.put("Food_Chicken_Raw", "Raw Chicken");
      FOOD_DISPLAY_NAMES.put("Food_Chicken_Cooked", "Cooked Chicken");
      FOOD_DISPLAY_NAMES.put("Food_Fish_Raw", "Raw Fish");
      FOOD_DISPLAY_NAMES.put("Food_Fish_Grilled", "Grilled Fish");
      FOOD_DISPLAY_NAMES.put("Plant_Seed_Wheat", "Wheat Seeds");
      FOOD_DISPLAY_NAMES.put("Plant_Seed_Corn", "Corn Seeds");
      FOOD_DISPLAY_NAMES.put("Plant_Seed_Carrot", "Carrot Seeds");
      FOOD_DISPLAY_NAMES.put("Seeds", "Seeds");
   }

   public static class ReloadSubCommand extends AbstractCommand {
      public ReloadSubCommand() {
         super("reload", "Reload configuration from file");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               config.loadFromFile(HyTamePlugin.getInstance().getConfigDirectory().resolve("config.json"));
               ctx.sendMessage(Message.raw("Config reloaded from file.").color("#55FF55"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class SaveSubCommand extends AbstractCommand {
      public SaveSubCommand() {
         super("save", "Save current configuration to file");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               config.saveToFile();
               ctx.sendMessage(Message.raw("Config saved to file.").color("#55FF55"));
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class ListSubCommand extends AbstractCommand {
      private final OptionalArg<AnimalType.Category> categoryArg = this.withOptionalArg("category", "Filter by category (LIVESTOCK, MAMMAL, etc.)", ArgTypes.forEnum("category", AnimalType.Category.class));

      public ListSubCommand() {
         super("list", "List all animals or filter by category");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ConfigManager config = BreedingConfigCommand.getConfig();
         if (config == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            AnimalType.Category filterCat = (AnimalType.Category)ctx.get(this.categoryArg);
            String var10000 = filterCat != null ? " (" + String.valueOf(filterCat) + ")" : "";
            String title = "=== Animal Config" + var10000 + " ===";
            ctx.sendMessage(Message.raw(title).color("#FF9900"));
            AnimalType.Category currentCat = null;

            for(AnimalType type : AnimalType.values()) {
               if (filterCat == null || type.getCategory() == filterCat) {
                  if (currentCat != type.getCategory()) {
                     currentCat = type.getCategory();
                     ctx.sendMessage(Message.raw("--- " + currentCat.name() + " ---").color("#FFFF55"));
                  }

                  ConfigManager.AnimalConfig ac = config.getAnimalConfig(type);
                  boolean enabled = config.isAnimalEnabled(type);
                  int foodCount = ac != null ? ac.breedingFoods.size() : 1;
                  double growth = ac != null ? ac.growthTimeMinutes : (double)30.0F;
                  boolean hasBaby = type.hasBabyVariant();
                  Message line = Message.raw(enabled ? "+ " : "- ").color(enabled ? "#55FF55" : "#FF5555").insert(Message.raw(String.format("%-15s ", type.name())).color("#FFFFFF")).insert(Message.raw("foods=").color("#AAAAAA")).insert(Message.raw(String.valueOf(foodCount)).color("#FFFF55")).insert(Message.raw(" growth=").color("#AAAAAA")).insert(Message.raw(String.format("%.0fm", growth)).color("#FFFF55"));
                  if (!hasBaby) {
                     line = line.insert(Message.raw(" (no baby)").color("#555555"));
                  }

                  ctx.sendMessage(line);
               }
            }

            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class InfoSubCommand extends AbstractCommand {
      private final RequiredArg<String> animalArg;

      public InfoSubCommand() {
         super("info", "Show detailed information for an animal");
         this.animalArg = this.withRequiredArg("animal", "Animal name (built-in or custom)", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ConfigManager config = BreedingConfigCommand.getConfig();
         if (config == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            String animalId = (String)ctx.get(this.animalArg);
            ConfigManager.AnimalLookupResult lookup = config.lookupAnimal(animalId);
            if (lookup == null) {
               ctx.sendMessage(Message.raw("Unknown animal: ").color("#FF5555").insert(Message.raw(animalId).color("#FFFFFF")));
               ctx.sendMessage(Message.raw("Use /hytame custom add to register custom animals").color("#AAAAAA"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               if (lookup.isBuiltIn()) {
                  AnimalType type = lookup.getBuiltInType();
                  ConfigManager.AnimalConfig ac = config.getAnimalConfig(type);
                  String var10001 = type.name();
                  ctx.sendMessage(Message.raw("=== " + var10001 + " ===").color("#FF9900"));
                  ctx.sendMessage(Message.raw("Category: ").color("#AAAAAA").insert(Message.raw(type.getCategory().name()).color("#FFFFFF")));
                  boolean breedingEnabled = config.isBreedingEnabled(type);
                  boolean tamingEnabled = config.isTamingEnabled(type);
                  ctx.sendMessage(Message.raw("Breeding: ").color("#AAAAAA").insert(Message.raw(breedingEnabled ? "Yes" : "No").color(breedingEnabled ? "#55FF55" : "#FF5555")).insert(Message.raw("  Taming: ").color("#AAAAAA")).insert(Message.raw(tamingEnabled ? "Yes" : "No").color(tamingEnabled ? "#55FF55" : "#FF5555")));
                  boolean hasBaby = type.hasBabyVariant();
                  Message babyMsg = Message.raw("Has Baby: ").color("#AAAAAA").insert(Message.raw(hasBaby ? "Yes" : "No").color(hasBaby ? "#55FF55" : "#FF5555"));
                  if (hasBaby) {
                     babyMsg = babyMsg.insert(Message.raw(" (" + type.getBabyNpcRoleId() + ")").color("#AAAAAA"));
                  }

                  ctx.sendMessage(babyMsg);
                  Message var22 = Message.raw("Growth Time: ").color("#AAAAAA");
                  double var10002 = ac != null ? ac.growthTimeMinutes : (double)30.0F;
                  ctx.sendMessage(var22.insert(Message.raw(var10002 + " min").color("#FFFF55")));
                  var22 = Message.raw("Cooldown: ").color("#AAAAAA");
                  var10002 = ac != null ? ac.breedCooldownMinutes : (double)5.0F;
                  ctx.sendMessage(var22.insert(Message.raw(var10002 + " min").color("#FFFF55")));
                  ctx.sendMessage(Message.raw("Breeding Foods:").color("#AAAAAA"));
                  List<String> foods = config.getBreedingFoods(type);

                  for(int i = 0; i < foods.size(); ++i) {
                     String food = (String)foods.get(i);
                     boolean isPrimary = i == 0;
                     ctx.sendMessage(Message.raw(isPrimary ? "* " : "  ").color(isPrimary ? "#55FF55" : "#AAAAAA").insert(Message.raw(food).color("#FFFFFF")));
                  }
               } else {
                  CustomAnimalConfig custom = lookup.getCustomConfig();
                  String var24 = custom.getDisplayName();
                  ctx.sendMessage(Message.raw("=== " + var24 + " (Custom) ===").color("#FF9900"));
                  ctx.sendMessage(Message.raw("Model ID: ").color("#AAAAAA").insert(Message.raw(custom.getModelAssetId()).color("#FFFFFF")));
                  boolean breedingEnabled = custom.isBreedingEnabled();
                  boolean tamingEnabled = custom.isTamingEnabled();
                  ctx.sendMessage(Message.raw("Breeding: ").color("#AAAAAA").insert(Message.raw(breedingEnabled ? "Yes" : "No").color(breedingEnabled ? "#55FF55" : "#FF5555")).insert(Message.raw("  Taming: ").color("#AAAAAA")).insert(Message.raw(tamingEnabled ? "Yes" : "No").color(tamingEnabled ? "#55FF55" : "#FF5555")));
                  ctx.sendMessage(Message.raw("NPC Role: ").color("#AAAAAA").insert(Message.raw(custom.getAdultNpcRoleId()).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Growth Time: ").color("#AAAAAA").insert(Message.raw(custom.getGrowthTimeMinutes() + " min").color("#FFFF55")));
                  ctx.sendMessage(Message.raw("Cooldown: ").color("#AAAAAA").insert(Message.raw(custom.getBreedCooldownMinutes() + " min").color("#FFFF55")));
                  ctx.sendMessage(Message.raw("Breeding Foods:").color("#AAAAAA"));
                  List<String> foods = custom.getBreedingFoods();

                  for(int i = 0; i < foods.size(); ++i) {
                     String food = (String)foods.get(i);
                     boolean isPrimary = i == 0;
                     ctx.sendMessage(Message.raw(isPrimary ? "* " : "  ").color(isPrimary ? "#55FF55" : "#AAAAAA").insert(Message.raw(food).color("#FFFFFF")));
                  }
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class EnableSubCommand extends AbstractCommand {
      private final RequiredArg<String> targetArg;

      public EnableSubCommand() {
         super("enable", "Enable breeding for animal, category, or ALL");
         this.targetArg = this.withRequiredArg("target", "Animal name, category (LIVESTOCK, MAMMAL, etc.), or ALL", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String target = ((String)ctx.get(this.targetArg)).toUpperCase();
               BreedingConfigCommand.handleToggle(ctx, config, target, true);
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class DisableSubCommand extends AbstractCommand {
      private final RequiredArg<String> targetArg;

      public DisableSubCommand() {
         super("disable", "Disable breeding for animal, category, or ALL");
         this.targetArg = this.withRequiredArg("target", "Animal name, category (LIVESTOCK, MAMMAL, etc.), or ALL", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String target = ((String)ctx.get(this.targetArg)).toUpperCase();
               BreedingConfigCommand.handleToggle(ctx, config, target, false);
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class EnableTamingSubCommand extends AbstractCommand {
      private final RequiredArg<String> targetArg;

      public EnableTamingSubCommand() {
         super("enabletaming", "Enable taming for animal, category, or ALL");
         this.targetArg = this.withRequiredArg("target", "Animal name, category (LIVESTOCK, MAMMAL, etc.), or ALL", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String target = ((String)ctx.get(this.targetArg)).toUpperCase();
               BreedingConfigCommand.handleTamingToggle(ctx, config, target, true);
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class DisableTamingSubCommand extends AbstractCommand {
      private final RequiredArg<String> targetArg;

      public DisableTamingSubCommand() {
         super("disabletaming", "Disable taming for animal, category, or ALL");
         this.targetArg = this.withRequiredArg("target", "Animal name, category (LIVESTOCK, MAMMAL, etc.), or ALL", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String target = ((String)ctx.get(this.targetArg)).toUpperCase();
               BreedingConfigCommand.handleTamingToggle(ctx, config, target, false);
               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class SetSubCommand extends AbstractCommand {
      private final RequiredArg<String> animalArg;
      private final RequiredArg<String> propertyArg;
      private final RequiredArg<String> valueArg;

      public SetSubCommand() {
         super("set", "Set animal property (food, growth, cooldown)");
         this.animalArg = this.withRequiredArg("animal", "Animal name (built-in or custom)", ArgTypes.STRING);
         this.propertyArg = this.withRequiredArg("property", "Property: food, growth, cooldown", ArgTypes.STRING);
         this.valueArg = this.withRequiredArg("value", "New value", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String animalId = (String)ctx.get(this.animalArg);
               String property = ((String)ctx.get(this.propertyArg)).toLowerCase();
               String value = (String)ctx.get(this.valueArg);
               ConfigManager.AnimalLookupResult lookup = config.lookupAnimal(animalId);
               if (lookup == null) {
                  ctx.sendMessage(Message.raw("Unknown animal: ").color("#FF5555").insert(Message.raw(animalId).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Use /hytame custom add to register custom animals").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  String displayName = lookup.getDisplayName();
                  switch (property) {
                     case "food":
                        String resolvedFood = BreedingConfigCommand.resolveFoodShortcut(value);
                        config.setAnyAnimalFood(animalId, resolvedFood);
                        ctx.sendMessage(Message.raw("Set ").color("#55FF55").insert(Message.raw(displayName).color("#FFFFFF")).insert(Message.raw(" primary food to: ").color("#55FF55")).insert(Message.raw(resolvedFood).color("#FFFFFF")));
                        ctx.sendMessage(Message.raw("(This replaces all foods. Use ").color("#AAAAAA").insert(Message.raw("/hytame config addfood").color("#FFFFFF")).insert(Message.raw(" to add more.)").color("#AAAAAA")));
                        config.saveToFile();
                        BreedingConfigCommand.syncPatchForAnimal(lookup);
                        break;
                     case "growth":
                        try {
                           double minutes = Double.parseDouble(value);
                           config.setAnyAnimalGrowthTime(animalId, minutes);
                           ctx.sendMessage(Message.raw("Set ").color("#55FF55").insert(Message.raw(displayName).color("#FFFFFF")).insert(Message.raw(" growth time to: ").color("#55FF55")).insert(Message.raw(minutes + " min").color("#FFFF55")));
                           config.saveToFile();
                           BreedingConfigCommand.syncGrowthPatchForAnimal(lookup);
                        } catch (NumberFormatException var14) {
                           ctx.sendMessage(Message.raw("Invalid number: ").color("#FF5555").insert(Message.raw(value).color("#FFFFFF")));
                        }
                        break;
                     case "cooldown":
                        try {
                           double minutes = Double.parseDouble(value);
                           config.setAnyAnimalCooldown(animalId, minutes);
                           ctx.sendMessage(Message.raw("Set ").color("#55FF55").insert(Message.raw(displayName).color("#FFFFFF")).insert(Message.raw(" cooldown to: ").color("#55FF55")).insert(Message.raw(minutes + " min").color("#FFFF55")));
                           config.saveToFile();
                        } catch (NumberFormatException var13) {
                           ctx.sendMessage(Message.raw("Invalid number: ").color("#FF5555").insert(Message.raw(value).color("#FFFFFF")));
                        }
                        break;
                     default:
                        ctx.sendMessage(Message.raw("Unknown property: ").color("#FF5555").insert(Message.raw(property).color("#FFFFFF")));
                        ctx.sendMessage(Message.raw("Valid: food, growth, cooldown").color("#AAAAAA"));
                  }

                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }
   }

   public static class AddFoodSubCommand extends AbstractCommand {
      private final RequiredArg<String> animalArg;
      private final RequiredArg<String> foodArg;

      public AddFoodSubCommand() {
         super("addfood", "Add a breeding food to an animal");
         this.animalArg = this.withRequiredArg("animal", "Animal name (built-in or custom)", ArgTypes.STRING);
         this.foodArg = this.withRequiredArg("food", "Item ID or shortcut (e.g., Carrot, Wheat, Apple)", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String animalId = (String)ctx.get(this.animalArg);
               String foodInput = (String)ctx.get(this.foodArg);
               ConfigManager.AnimalLookupResult lookup = config.lookupAnimal(animalId);
               if (lookup == null) {
                  ctx.sendMessage(Message.raw("Unknown animal: ").color("#FF5555").insert(Message.raw(animalId).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Use /hytame custom add to register custom animals").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               } else if (foodInput != null && !foodInput.isEmpty()) {
                  String food = BreedingConfigCommand.resolveFoodShortcut(foodInput);
                  config.addAnyAnimalFood(animalId, food);
                  ctx.sendMessage(Message.raw("Added ").color("#55FF55").insert(Message.raw(food).color("#FFFFFF")).insert(Message.raw(" to " + lookup.getDisplayName() + " breeding foods.").color("#55FF55")));
                  ctx.sendMessage(Message.raw("Foods: ").color("#AAAAAA").insert(Message.raw(String.join(", ", config.getAnyAnimalFoods(animalId))).color("#FFFFFF")));
                  config.saveToFile();
                  BreedingConfigCommand.syncPatchForAnimal(lookup);
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  ctx.sendMessage(Message.raw("Food item is required!").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }
   }

   public static class RemoveFoodSubCommand extends AbstractCommand {
      private final RequiredArg<String> animalArg;
      private final RequiredArg<String> foodArg;

      public RemoveFoodSubCommand() {
         super("removefood", "Remove a breeding food from an animal");
         this.animalArg = this.withRequiredArg("animal", "Animal name (built-in or custom)", ArgTypes.STRING);
         this.foodArg = this.withRequiredArg("food", "Item ID or shortcut to remove", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String animalId = (String)ctx.get(this.animalArg);
               String foodInput = (String)ctx.get(this.foodArg);
               ConfigManager.AnimalLookupResult lookup = config.lookupAnimal(animalId);
               if (lookup == null) {
                  ctx.sendMessage(Message.raw("Unknown animal: ").color("#FF5555").insert(Message.raw(animalId).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Use /hytame custom add to register custom animals").color("#AAAAAA"));
                  return CompletableFuture.completedFuture((Object)null);
               } else if (foodInput != null && !foodInput.isEmpty()) {
                  String food = BreedingConfigCommand.resolveFoodShortcut(foodInput);
                  List<String> foods = config.getAnyAnimalFoods(animalId);
                  if (foods.size() <= 1) {
                     ctx.sendMessage(Message.raw("Cannot remove last food. Use ").color("#FF5555").insert(Message.raw("/hytame config set food").color("#FFFFFF")).insert(Message.raw(" to replace instead.").color("#FF5555")));
                     return CompletableFuture.completedFuture((Object)null);
                  } else {
                     config.removeAnyAnimalFood(animalId, food);
                     ctx.sendMessage(Message.raw("Removed ").color("#55FF55").insert(Message.raw(food).color("#FFFFFF")).insert(Message.raw(" from " + lookup.getDisplayName() + " breeding foods.").color("#55FF55")));
                     ctx.sendMessage(Message.raw("Foods: ").color("#AAAAAA").insert(Message.raw(String.join(", ", config.getAnyAnimalFoods(animalId))).color("#FFFFFF")));
                     config.saveToFile();
                     BreedingConfigCommand.syncPatchForAnimal(lookup);
                     return CompletableFuture.completedFuture((Object)null);
                  }
               } else {
                  ctx.sendMessage(Message.raw("Food item is required!").color("#FF5555"));
                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }
   }

   public static class PresetSubCommand extends AbstractCommand {
      public PresetSubCommand() {
         super("preset", "Manage configuration presets");
         this.addSubCommand(new PresetListSubCommand());
         this.addSubCommand(new PresetApplySubCommand());
         this.addSubCommand(new PresetSaveSubCommand());
         this.addSubCommand(new PresetRestoreSubCommand());
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ctx.sendMessage(Message.raw("=== Preset Commands ===").color("#FF9900"));
         ctx.sendMessage(Message.raw("/hytame config preset list").color("#FFFFFF").insert(Message.raw("- Show available presets").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame config preset apply <name>").color("#FFFFFF").insert(Message.raw("- Apply a preset").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame config preset save <name>").color("#FFFFFF").insert(Message.raw("- Save current config as preset").color("#AAAAAA")));
         ctx.sendMessage(Message.raw("/hytame config preset restore <name>").color("#FFFFFF").insert(Message.raw("- Reset built-in preset to defaults").color("#AAAAAA")));
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   public static class PresetListSubCommand extends AbstractCommand {
      public PresetListSubCommand() {
         super("list", "Show available presets");
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         ConfigManager config = BreedingConfigCommand.getConfig();
         if (config == null) {
            ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ctx.sendMessage(Message.raw("=== Available Presets ===").color("#FF9900"));
            ctx.sendMessage(Message.raw("Current: ").color("#AAAAAA").insert(Message.raw(config.getActivePreset()).color("#FFFFFF")));

            for(String preset : config.getAvailablePresets()) {
               boolean isCurrent = preset.equals(config.getActivePreset());
               boolean isBuiltin = config.isBuiltinPreset(preset);
               String desc;
               switch (preset) {
                  case "default" -> desc = "Original values, livestock only";
                  case "default_extended" -> desc = "Default timings + multiple foods";
                  case "lait_curated" -> desc = "Balanced timings, multiple foods";
                  case "zoo" -> desc = "Real animals (no mythic/vermin/boss)";
                  case "all" -> desc = "All 119 animals enabled";
                  default -> desc = "(custom)";
               }

               String marker = isCurrent ? "* " : (isBuiltin ? "  " : "  ");
               Message line = Message.raw(marker).color(isCurrent ? "#55FF55" : "#AAAAAA").insert(Message.raw(preset).color("#FFFFFF")).insert(Message.raw(" - " + desc).color("#555555"));
               ctx.sendMessage(line);
            }

            return CompletableFuture.completedFuture((Object)null);
         }
      }
   }

   public static class PresetApplySubCommand extends AbstractCommand {
      private final RequiredArg<String> presetArg;

      public PresetApplySubCommand() {
         super("apply", "Apply a configuration preset");
         this.presetArg = this.withRequiredArg("preset", "Preset name (default, lait_curated)", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String presetName = ((String)ctx.get(this.presetArg)).toLowerCase();
               if (config.applyPreset(presetName)) {
                  BreedingConfigCommand.syncAllPatches();
                  ctx.sendMessage(Message.raw("Applied preset: ").color("#55FF55").insert(Message.raw(presetName).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Use ").color("#AAAAAA").insert(Message.raw("/hytame config save").color("#FFFFFF")).insert(Message.raw(" to persist changes.").color("#AAAAAA")));
               } else {
                  ctx.sendMessage(Message.raw("Unknown preset: ").color("#FF5555").insert(Message.raw(presetName).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("Available: ").color("#AAAAAA").insert(Message.raw(String.join(", ", config.getAvailablePresets())).color("#FFFFFF")));
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class PresetSaveSubCommand extends AbstractCommand {
      private final RequiredArg<String> presetArg;

      public PresetSaveSubCommand() {
         super("save", "Save current configuration as a preset");
         this.presetArg = this.withRequiredArg("name", "Name for the new preset", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String presetName = ((String)ctx.get(this.presetArg)).toLowerCase().replaceAll("[^a-z0-9_-]", "_");
               if (config.saveAsPreset(presetName)) {
                  ctx.sendMessage(Message.raw("Saved preset: ").color("#55FF55").insert(Message.raw(presetName).color("#FFFFFF")));
                  ctx.sendMessage(Message.raw("File: ").color("#AAAAAA").insert(Message.raw("mods/presets/" + presetName + ".json").color("#FFFFFF")));
               } else {
                  ctx.sendMessage(Message.raw("Failed to save preset!").color("#FF5555"));
               }

               return CompletableFuture.completedFuture((Object)null);
            }
         }
      }
   }

   public static class PresetRestoreSubCommand extends AbstractCommand {
      private final RequiredArg<String> presetArg;

      public PresetRestoreSubCommand() {
         super("restore", "Reset a built-in preset to its default values");
         this.presetArg = this.withRequiredArg("preset", "Preset name (default, lait_curated, zoo, all)", ArgTypes.STRING);
      }

      protected boolean canGeneratePermission() {
         return false;
      }

      protected CompletableFuture<Void> execute(CommandContext ctx) {
         if (BreedingConfigCommand.checkAdminDenied(ctx)) {
            return CompletableFuture.completedFuture((Object)null);
         } else {
            ConfigManager config = BreedingConfigCommand.getConfig();
            if (config == null) {
               ctx.sendMessage(Message.raw("Plugin not initialized!").color("#FF5555"));
               return CompletableFuture.completedFuture((Object)null);
            } else {
               String presetName = ((String)ctx.get(this.presetArg)).toLowerCase();
               if (!config.isBuiltinPreset(presetName)) {
                  ctx.sendMessage(Message.raw("Can only restore built-in presets: ").color("#FF5555").insert(Message.raw("default, default_extended, lait_curated, zoo, all").color("#FFFFFF")));
                  return CompletableFuture.completedFuture((Object)null);
               } else {
                  if (config.restorePreset(presetName)) {
                     ctx.sendMessage(Message.raw("Restored preset to defaults: ").color("#55FF55").insert(Message.raw(presetName).color("#FFFFFF")));
                     ctx.sendMessage(Message.raw("All customizations have been reset.").color("#AAAAAA"));
                  } else {
                     ctx.sendMessage(Message.raw("Failed to restore preset!").color("#FF5555"));
                  }

                  return CompletableFuture.completedFuture((Object)null);
               }
            }
         }
      }
   }
}
