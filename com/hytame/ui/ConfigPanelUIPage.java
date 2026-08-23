package com.hytame.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.TamedAnimalData;
import com.hytame.patch.PatchSyncService;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConfigPanelUIPage extends InteractiveCustomUIPage<ConfigEventData> {
   private static final String[][] KNOWN_FOODS = new String[][]{{"Plant_Crop_Wheat_Item", "Wheat"}, {"Plant_Crop_Carrot_Item", "Carrot"}, {"Plant_Crop_Potato_Item", "Potato"}, {"Plant_Crop_Cauliflower_Item", "Cauliflower"}, {"Plant_Crop_Lettuce_Item", "Lettuce"}, {"Plant_Crop_Corn_Item", "Corn"}, {"Plant_Crop_Rice_Item", "Rice"}, {"Plant_Crop_Chilli_Item", "Chilli"}, {"Plant_Crop_Mushroom_Cap_Brown", "Brown Mushroom"}, {"Plant_Crop_Mushroom_Cap_Red", "Red Mushroom"}, {"Plant_Fruit_Apple", "Apple"}, {"Plant_Fruit_Berries_Red", "Red Berries"}, {"Plant_Cactus_Flower", "Cactus Flower"}, {"Food_Beef_Raw", "Raw Beef"}, {"Food_Chicken_Raw", "Raw Chicken"}, {"Food_Pork_Raw", "Raw Pork"}, {"Food_Fish_Raw", "Raw Fish"}, {"Food_Wildmeat_Raw", "Raw Wildmeat"}, {"Food_Fish_Grilled", "Grilled Fish"}, {"Food_Wildmeat_Cooked", "Cooked Wildmeat"}, {"Food_Bread", "Bread"}};
   private static final String[][] KNOWN_MATERIALS = new String[][]{{"Ingredient_Bone_Fragment", "Bone Fragment"}, {"Ingredient_Crystal_Purple", "Purple Crystal"}, {"Ingredient_Void_Essence", "Void Essence"}, {"Ingredient_Bar_Iron", "Iron Bar"}, {"Ingredient_Ice_Essence", "Ice Essence"}};
   private static final Set<String> KNOWN_ICON_IDS;
   private static final Set<String> ALL_KNOWN_ITEM_IDS;
   private static final Set<String> BABY_SUFFIXES;
   private final boolean readOnly;
   private String presetSearchFilter;
   private String animalSearchFilter;
   private String foodSearchFilter;
   private String selectedAnimalName;
   private boolean dirty;
   private boolean globalDirty;
   private final Set<AnimalType> dirtyAnimals;
   private String activeTab;
   private String selectedTamedId;
   private String tamedSearchFilter;
   private Map<String, Integer> tamedUiIndexMap;
   private boolean showingRolePicker;
   private String rolePickerMode;
   private String roleSearchFilter;
   private List<AnimalType> currentFilteredAnimals;
   private Map<String, Integer> animalUiIndexMap;
   private List<String> currentFilteredPresets;
   private static final Set<AnimalType.Category> HIDDEN_CATEGORIES;
   private static final String[] SEPARATOR_SENTINEL;
   private static final String[] FOOD_ITEM_PREFIXES;
   private static final Map<String, String> ICON_FALLBACK;
   private static final Set<String> NO_MEMORY_ICON;
   private static final String NPC_DEFAULT_ICON = "Pages/Icons/Npc_Default.png";

   public ConfigPanelUIPage(PlayerRef playerRef) {
      this(playerRef, false);
   }

   public ConfigPanelUIPage(PlayerRef playerRef, boolean readOnly) {
      super(playerRef, CustomPageLifetime.CanDismiss, ConfigPanelUIPage.ConfigEventData.CODEC);
      this.presetSearchFilter = "";
      this.animalSearchFilter = "";
      this.foodSearchFilter = "";
      this.selectedAnimalName = null;
      this.dirty = false;
      this.globalDirty = false;
      this.dirtyAnimals = EnumSet.noneOf(AnimalType.class);
      this.activeTab = "list";
      this.selectedTamedId = null;
      this.tamedSearchFilter = "";
      this.tamedUiIndexMap = new HashMap();
      this.showingRolePicker = false;
      this.rolePickerMode = null;
      this.roleSearchFilter = "";
      this.currentFilteredAnimals = new ArrayList();
      this.animalUiIndexMap = new HashMap();
      this.currentFilteredPresets = new ArrayList();
      this.readOnly = readOnly;
   }

   public ConfigPanelUIPage(PlayerRef playerRef, String presetSearch, String animalSearch, String selectedAnimalName, boolean dirty, boolean readOnly, String activeTab, String selectedTamedId, String tamedSearchFilter) {
      super(playerRef, CustomPageLifetime.CanDismiss, ConfigPanelUIPage.ConfigEventData.CODEC);
      this.presetSearchFilter = "";
      this.animalSearchFilter = "";
      this.foodSearchFilter = "";
      this.selectedAnimalName = null;
      this.dirty = false;
      this.globalDirty = false;
      this.dirtyAnimals = EnumSet.noneOf(AnimalType.class);
      this.activeTab = "list";
      this.selectedTamedId = null;
      this.tamedSearchFilter = "";
      this.tamedUiIndexMap = new HashMap();
      this.showingRolePicker = false;
      this.rolePickerMode = null;
      this.roleSearchFilter = "";
      this.currentFilteredAnimals = new ArrayList();
      this.animalUiIndexMap = new HashMap();
      this.currentFilteredPresets = new ArrayList();
      this.readOnly = readOnly;
      this.presetSearchFilter = presetSearch != null ? presetSearch : "";
      this.animalSearchFilter = animalSearch != null ? animalSearch : "";
      this.selectedAnimalName = selectedAnimalName;
      this.dirty = dirty;
      this.activeTab = activeTab != null ? activeTab : "list";
      this.selectedTamedId = selectedTamedId;
      this.tamedSearchFilter = tamedSearchFilter != null ? tamedSearchFilter : "";
   }

   private boolean isListView() {
      return (this.readOnly || "list".equals(this.activeTab)) && !"tamed".equals(this.activeTab);
   }

   private boolean isTamedView() {
      return "tamed".equals(this.activeTab);
   }

   public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
      if (this.isTamedView()) {
         cmd.append(this.readOnly ? "Pages/ConfigPanelTamedReadOnly.ui" : "Pages/ConfigPanelTamed.ui");
      } else if (this.readOnly) {
         cmd.append("Pages/ConfigPanelReadOnly.ui");
      } else if (this.isListView()) {
         cmd.append("Pages/ConfigPanelList.ui");
      } else {
         cmd.append("Pages/ConfigPanel.ui");
      }

      if (this.isTamedView()) {
         cmd.set("#tabTamedBtn.Background", "#3a5a8a");
      } else if (!this.readOnly) {
         if ("list".equals(this.activeTab)) {
            cmd.set("#tabListBtn.Background", "#3a5a8a");
         } else {
            cmd.set("#tabEditBtn.Background", "#3a5a8a");
         }
      } else {
         cmd.set("#tabListBtn.Background", "#3a5a8a");
      }

      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null) {
         ConfigManager config = plugin.getConfigManager();
         if (config != null) {
            if (this.isTamedView()) {
               this.populateTamedAnimalList(cmd, events, ref, store);
               this.populateTamedDetailPanel(cmd);
               this.setupTamedEventBindings(cmd, events);
               cmd.set("#tamedSearch.Value", this.tamedSearchFilter);
            } else {
               if (!this.isListView()) {
                  this.populatePresetSettings(cmd, config);
                  List<String> presets = this.getFilteredPresets(config);
                  this.populatePresetList(cmd, events, presets, config.getActivePreset(), config);
               }

               List<AnimalType> animals = this.getFilteredAnimals();
               this.populateAnimalTable(cmd, events, animals, config);
               if (!this.isListView()) {
                  this.populateDetailPanel(cmd, config);
                  this.populateFoodPicker(cmd, events, config);
               }

               this.setupEventBindings(cmd, events);
               if (!this.isListView()) {
                  cmd.set("#presetSearch.Value", this.presetSearchFilter);
                  cmd.set("#foodSearch.Value", this.foodSearchFilter);
               }

               cmd.set("#animalSearch.Value", this.animalSearchFilter);
               if (!this.isListView()) {
                  String activePreset = config.getActivePreset();
                  cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
                  if (activePreset != null && !config.isBuiltinPreset(activePreset)) {
                     cmd.set("#saveAsPresetBtn.Text", "SAVE AS \"" + activePreset + "\" PRESET");
                  } else {
                     cmd.set("#saveAsPresetBtn.Text", "SAVE AS NEW PRESET");
                  }
               }

            }
         }
      }
   }

   private void populatePresetSettings(UICommandBuilder cmd, ConfigManager config) {
      String activePreset = config.getActivePreset();
      boolean isBuiltin = activePreset != null && config.isBuiltinPreset(activePreset);
      cmd.set("#presetRenameInput.Value", activePreset != null ? activePreset : "none");
      if (isBuiltin) {
         cmd.set("#renamePresetBtn.Text", "RESTORE");
         cmd.set("#actionRenamePreset.Value", "RESTORE_PRESET");
      } else {
         cmd.set("#renamePresetBtn.Text", "RENAME");
         cmd.set("#actionRenamePreset.Value", "RENAME_PRESET");
      }

      cmd.set("#growthToggleBtn.Text", config.isGrowthEnabled() ? "ON" : "OFF");
      cmd.set("#growthToggleBtn.Background", config.isGrowthEnabled() ? "#2a6a2a" : "#6a2a2a");
      cmd.set("#growthTimeInput.Value", String.valueOf(config.getDefaultGrowthTimeMinutes()));
      cmd.set("#cooldownInput.Value", String.valueOf(config.getDefaultBreedCooldownMinutes()));
      cmd.set("#perPlayerLimitToggleBtn.Text", config.isUsePerPlayerLimit() ? "ON" : "OFF");
      cmd.set("#perPlayerLimitToggleBtn.Background", config.isUsePerPlayerLimit() ? "#2a6a2a" : "#6a2a2a");
      cmd.set("#perPlayerLimitInput.Value", String.valueOf(config.getPerPlayerTameLimit()));
      boolean simpleClaimsAvailable = HyTamePlugin.getInstance() != null && HyTamePlugin.getInstance().isSimpleClaimsInstalled();
      if (simpleClaimsAvailable) {
         cmd.append("#perClaimLimitRow", "Pages/PerClaimLimitRow.ui");
         cmd.set("#perClaimLimitToggleBtn.Text", config.isUsePerClaimLimit() ? "ON" : "OFF");
         cmd.set("#perClaimLimitToggleBtn.Background", config.isUsePerClaimLimit() ? "#2a6a2a" : "#6a2a2a");
         cmd.set("#perClaimLimitInput.Value", String.valueOf(config.getPerClaimTameLimit()));
      }

   }

   private List<String> getFilteredPresets(ConfigManager config) {
      List<String> allPresets = config.getAvailablePresets();
      if (this.presetSearchFilter != null && !this.presetSearchFilter.isEmpty()) {
         List<String> filtered = new ArrayList();
         String filterLower = this.presetSearchFilter.toLowerCase();

         for(String preset : allPresets) {
            if (preset.toLowerCase().contains(filterLower)) {
               filtered.add(preset);
            }
         }

         return filtered;
      } else {
         return allPresets;
      }
   }

   private void populatePresetList(UICommandBuilder cmd, UIEventBuilder events, List<String> presets, String activePreset, ConfigManager config) {
      cmd.clear("#presetList");
      this.currentFilteredPresets = new ArrayList(presets);

      for(int i = 0; i < presets.size(); ++i) {
         String presetName = (String)presets.get(i);
         String sel = "#presetList[" + i + "]";
         cmd.append("#presetList", "Pages/ConfigPresetRow.ui");
         String displayName = config.isBuiltinPreset(presetName) ? "[HyTame] " + presetName : presetName;
         cmd.set(sel + " #presetBtn.Text", displayName);
         cmd.set(sel + " #presetAction.Value", "SELECT_PRESET:" + presetName);
         cmd.set(sel + " #copyAction.Value", "COPY_PRESET:" + presetName);
         if (presetName.equals(activePreset)) {
            cmd.set(sel + ".Background", "#3a5a8a");
         }

         if (!this.readOnly) {
            events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #presetBtn", (new EventData()).append("@action", sel + " #presetAction.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value"));
            events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #copyPresetBtn", (new EventData()).append("@action", sel + " #copyAction.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value"));
         }
      }

   }

   private List<AnimalType> getFilteredAnimals() {
      AnimalType[] allAnimals = AnimalType.values();
      List<AnimalType> filtered = new ArrayList();
      String filterLower = this.animalSearchFilter != null && !this.animalSearchFilter.isEmpty() ? this.animalSearchFilter.toLowerCase() : null;

      for(AnimalType animal : allAnimals) {
         if (!HIDDEN_CATEGORIES.contains(animal.getCategory()) && (filterLower == null || animal.name().toLowerCase().contains(filterLower) || animal.getModelAssetId().toLowerCase().contains(filterLower))) {
            filtered.add(animal);
         }
      }

      return filtered;
   }

   private static String getCategoryDisplayName(AnimalType.Category cat) {
      switch (cat) {
         case LIVESTOCK -> {
            return "Livestock";
         }
         case MAMMAL -> {
            return "Mammals";
         }
         case CRITTER -> {
            return "Critters";
         }
         case AVIAN -> {
            return "Birds";
         }
         case REPTILE -> {
            return "Reptiles";
         }
         case VERMIN -> {
            return "Vermin";
         }
         case AQUATIC -> {
            return "Aquatic";
         }
         case MYTHIC -> {
            return "Mythical";
         }
         case DINOSAUR -> {
            return "Dinosaurs";
         }
         case BOSS -> {
            return "Bosses";
         }
         case UNDEAD -> {
            return "Undead";
         }
         case GOLEM -> {
            return "Golems";
         }
         case SPIRIT -> {
            return "Spirits";
         }
         case GOBLIN -> {
            return "Goblins";
         }
         case TRORK -> {
            return "Trorks";
         }
         case SCARAK -> {
            return "Scaraks";
         }
         case KWEEBEC -> {
            return "Kweebecs";
         }
         case OUTLANDER -> {
            return "Outlanders";
         }
         case VOID -> {
            return "Void";
         }
         case MISC -> {
            return "Other";
         }
         default -> {
            return cat.name();
         }
      }
   }

   private void populateAnimalTable(UICommandBuilder cmd, UIEventBuilder events, List<AnimalType> animals, ConfigManager config) {
      cmd.clear("#animalRows");
      this.currentFilteredAnimals = new ArrayList(animals);
      this.animalUiIndexMap.clear();
      int uiIndex = 0;
      AnimalType.Category lastCategory = null;

      for(int i = 0; i < animals.size(); ++i) {
         AnimalType animal = (AnimalType)animals.get(i);
         if (animal.getCategory() != lastCategory) {
            lastCategory = animal.getCategory();
            cmd.appendInline("#animalRows", "Group { Anchor: (Height: 30, Bottom: 4, Top: 4); Background: #2a2a3a; Padding: (Left: 10); Label { Text: \"" + getCategoryDisplayName(lastCategory) + "\"; Style: (FontSize: 13, RenderBold: true, TextColor: #ffaa00, VerticalAlignment: Center); } }");
            ++uiIndex;
         }

         ConfigManager.AnimalConfig animalConfig = config.getAnimalConfig(animal);
         String sel = "#animalRows[" + uiIndex + "]";
         this.animalUiIndexMap.put(animal.name(), uiIndex);
         String rowTemplate;
         if (this.isListView()) {
            rowTemplate = "Pages/AnimalRowReadOnly.ui";
         } else if (animal.usesVanillaTaming()) {
            rowTemplate = "Pages/AnimalRowTameLocked.ui";
         } else {
            rowTemplate = "Pages/AnimalRow.ui";
         }

         cmd.append("#animalRows", rowTemplate);
         cmd.set(sel + " #selectBtn.Text", animal.getModelAssetId().replace("_", " "));
         cmd.appendInline(sel + " #icon", animalIconMarkup(36, this.getAnimalIconPath(animal), this.getAnimalBackgroundColor(animal)));
         boolean breedEnabled = animalConfig != null && animalConfig.breedingEnabled;
         cmd.set(sel + " #breedToggle.Text", breedEnabled ? "ON" : "OFF");
         if (this.isListView()) {
            cmd.set(sel + " #breedToggle.Background", breedEnabled ? "#1a3a1a" : "#3a1a1a");
         } else {
            cmd.set(sel + " #breedToggle.Background", breedEnabled ? "#2a6a2a" : "#6a2a2a");
         }

         boolean tameEnabled = animalConfig != null && animalConfig.tamingEnabled;
         boolean tameLocked = animal.usesVanillaTaming();
         if (tameLocked) {
            tameEnabled = true;
         }

         cmd.set(sel + " #tameToggle.Text", tameLocked ? "ON" : (tameEnabled ? "ON" : "OFF"));
         if (this.isListView()) {
            cmd.set(sel + " #tameToggle.Background", tameEnabled ? "#1a3a1a" : "#3a1a1a");
         }

         if (this.isListView()) {
            double cooldown = animalConfig != null ? animalConfig.breedCooldownMinutes : (double)0.0F;
            double growth = animalConfig != null ? animalConfig.growthTimeMinutes : (double)0.0F;
            cmd.set(sel + " #cooldownLabel.Text", cooldown > (double)0.0F ? formatMinutes(cooldown) : "-");
            cmd.set(sel + " #growthLabel.Text", growth > (double)0.0F ? formatMinutes(growth) : "-");
         }

         if (animal.name().equals(this.selectedAnimalName)) {
            cmd.set(sel + ".Background", "#3a5a8a");
         }

         cmd.set(sel + " #selectAction.Value", "SELECT_ANIMAL:" + animal.name());
         cmd.set(sel + " #breedAction.Value", "TOGGLE_BREED:" + animal.name());
         cmd.set(sel + " #tameAction.Value", "TOGGLE_TAMING:" + animal.name());
         List<String> foods = animalConfig != null ? animalConfig.getEffectiveBreedingFoods() : Collections.singletonList(animal.getDefaultBreedingFood());
         int maxIcons = this.isListView() ? 12 : 3;

         for(int f = 0; f < Math.min(foods.size(), maxIcons); ++f) {
            String foodIconPath = this.getFoodIconPath((String)foods.get(f));
            StringBuilder foodMarkup = new StringBuilder();
            foodMarkup.append("Group {\n");
            foodMarkup.append("  Anchor: (Width: 24, Height: 24, Right: 2);\n");
            if (foodIconPath != null) {
               foodMarkup.append("  Background: (TexturePath: \"").append(foodIconPath).append("\");\n");
            } else {
               foodMarkup.append("  Background: #3a3a3a;\n");
            }

            foodMarkup.append("}");
            cmd.appendInline(sel + " #foodIcons", foodMarkup.toString());
         }

         if (foods.size() > maxIcons) {
            cmd.appendInline(sel + " #foodIcons", "Label { Anchor: (Width: 20, Height: 24); Text: \"...\"; }");
         }

         if (!this.isListView()) {
            events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #selectBtn", this.buildDetailEventData(sel + " #selectAction.Value"));
         }

         if (!this.isListView()) {
            events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #breedToggle", this.buildDetailEventData(sel + " #breedAction.Value"));
            if (!animal.usesVanillaTaming()) {
               events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #tameToggle", this.buildDetailEventData(sel + " #tameAction.Value"));
            }
         }

         ++uiIndex;
      }

      Map<String, CustomAnimalConfig> customAnimals = config.getCustomAnimals();
      if (!customAnimals.isEmpty()) {
         String customFilter = this.animalSearchFilter != null && !this.animalSearchFilter.isEmpty() ? this.animalSearchFilter.toLowerCase() : null;
         List<Map.Entry<String, CustomAnimalConfig>> filteredCustom = new ArrayList();

         for(Map.Entry<String, CustomAnimalConfig> entry : customAnimals.entrySet()) {
            if (customFilter == null || ((String)entry.getKey()).toLowerCase().contains(customFilter) || ((CustomAnimalConfig)entry.getValue()).getDisplayName().toLowerCase().contains(customFilter)) {
               filteredCustom.add(entry);
            }
         }

         if (!filteredCustom.isEmpty()) {
            Map<String, List<Map.Entry<String, CustomAnimalConfig>>> byMod = new LinkedHashMap();

            for(Map.Entry<String, CustomAnimalConfig> entry : filteredCustom) {
               String mod = EcsReflectionUtil.getModNameForNpcRole(((CustomAnimalConfig)entry.getValue()).getAdultNpcRoleId());
               if (mod == null) {
                  mod = "Custom";
               }

               ((List)byMod.computeIfAbsent(mod, (k) -> new ArrayList())).add(entry);
            }

            for(Map.Entry<String, List<Map.Entry<String, CustomAnimalConfig>>> modGroup : byMod.entrySet()) {
               String modName = (String)modGroup.getKey();
               cmd.appendInline("#animalRows", "Group { Anchor: (Height: 30, Bottom: 4, Top: 8); Background: #2a2a3a; Padding: (Left: 10); Label { Text: \"" + modName + "\"; Style: (FontSize: 13, RenderBold: true, TextColor: #ff6600, VerticalAlignment: Center); } }");
               ++uiIndex;

               for(Map.Entry<String, CustomAnimalConfig> entry : (List)modGroup.getValue()) {
                  String modelId = (String)entry.getKey();
                  CustomAnimalConfig custom = (CustomAnimalConfig)entry.getValue();
                  String sel = "#animalRows[" + uiIndex + "]";
                  String mapKey = "CUSTOM:" + modelId;
                  this.animalUiIndexMap.put(mapKey, uiIndex);
                  cmd.append("#animalRows", this.isListView() ? "Pages/AnimalRowReadOnly.ui" : "Pages/AnimalRow.ui");
                  String var10001 = sel + " #selectBtn.Text";
                  String var10002 = custom.getDisplayName().replace("_", " ");
                  cmd.set(var10001, "[C] " + var10002);
                  cmd.appendInline(sel + " #icon", "Group { Anchor: (Width: 36, Height: 36); Background: (TexturePath: \"Pages/Icons/Npc_Default.png\"); }");
                  boolean breedEnabled = custom.isBreedingEnabled();
                  cmd.set(sel + " #breedToggle.Text", breedEnabled ? "ON" : "OFF");
                  if (this.isListView()) {
                     cmd.set(sel + " #breedToggle.Background", breedEnabled ? "#1a3a1a" : "#3a1a1a");
                  } else {
                     cmd.set(sel + " #breedToggle.Background", breedEnabled ? "#2a6a2a" : "#6a2a2a");
                  }

                  boolean tameEnabled = custom.isTamingEnabled();
                  cmd.set(sel + " #tameToggle.Text", tameEnabled ? "ON" : "OFF");
                  if (this.isListView()) {
                     cmd.set(sel + " #tameToggle.Background", tameEnabled ? "#1a3a1a" : "#3a1a1a");
                  } else {
                     cmd.set(sel + " #tameToggle.Background", tameEnabled ? "#2a6a2a" : "#6a2a2a");
                  }

                  if (this.isListView()) {
                     cmd.set(sel + " #cooldownLabel.Text", custom.getBreedCooldownMinutes() > (double)0.0F ? formatMinutes(custom.getBreedCooldownMinutes()) : "-");
                     cmd.set(sel + " #growthLabel.Text", custom.getGrowthTimeMinutes() > (double)0.0F ? formatMinutes(custom.getGrowthTimeMinutes()) : "-");
                  }

                  if (mapKey.equals(this.selectedAnimalName)) {
                     cmd.set(sel + ".Background", "#3a5a8a");
                  }

                  cmd.set(sel + " #selectAction.Value", "SELECT_CUSTOM:" + modelId);
                  cmd.set(sel + " #breedAction.Value", "TOGGLE_BREED_CUSTOM:" + modelId);
                  cmd.set(sel + " #tameAction.Value", "TOGGLE_TAMING_CUSTOM:" + modelId);
                  List<String> foods = custom.getBreedingFoods();
                  int maxIcons = this.isListView() ? 12 : 3;

                  for(int f = 0; f < Math.min(foods.size(), maxIcons); ++f) {
                     String foodIconPath = this.getFoodIconPath((String)foods.get(f));
                     StringBuilder foodMarkup = new StringBuilder();
                     foodMarkup.append("Group {\n");
                     foodMarkup.append("  Anchor: (Width: 24, Height: 24, Right: 2);\n");
                     if (foodIconPath != null) {
                        foodMarkup.append("  Background: (TexturePath: \"").append(foodIconPath).append("\");\n");
                     } else {
                        foodMarkup.append("  Background: #3a3a3a;\n");
                     }

                     foodMarkup.append("}");
                     cmd.appendInline(sel + " #foodIcons", foodMarkup.toString());
                  }

                  if (foods.size() > maxIcons) {
                     cmd.appendInline(sel + " #foodIcons", "Label { Anchor: (Width: 20, Height: 24); Text: \"...\"; }");
                  }

                  if (!this.isListView()) {
                     events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #selectBtn", this.buildDetailEventData(sel + " #selectAction.Value"));
                     events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #breedToggle", this.buildDetailEventData(sel + " #breedAction.Value"));
                     events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #tameToggle", this.buildDetailEventData(sel + " #tameAction.Value"));
                  }

                  ++uiIndex;
               }
            }
         }
      }

      if (!this.isListView()) {
         String addSel = "#animalRows[" + uiIndex + "]";
         cmd.append("#animalRows", "Pages/AnimalRow.ui");
         cmd.set(addSel + " #selectBtn.Text", "+ ADD CUSTOM ANIMAL");
         cmd.set(addSel + " #selectAction.Value", "ADD_CUSTOM_SHOW");
         cmd.set(addSel + ".Background", "#2a4a2a");
         cmd.set(addSel + " #breedToggle.Text", "");
         cmd.set(addSel + " #breedToggle.Background", "#00000000");
         cmd.set(addSel + " #tameToggle.Text", "");
         cmd.set(addSel + " #tameToggle.Background", "#00000000");
         events.addEventBinding(CustomUIEventBindingType.Activating, addSel + " #selectBtn", this.buildDetailEventData(addSel + " #selectAction.Value"));
      }

   }

   private void populateDetailPanel(UICommandBuilder cmd, ConfigManager config) {
      if (this.showingRolePicker) {
         String title = "baby".equals(this.rolePickerMode) ? "SET BABY VARIANT" : "ADD CUSTOM ANIMAL";
         cmd.set("#detailTitle.Text", title);
         cmd.set("#detailCooldown.Value", "");
         cmd.set("#detailGrowth.Value", "");
      } else if (this.selectedAnimalName == null) {
         cmd.set("#detailTitle.Text", "Select an animal");
         cmd.set("#detailCooldown.Value", "");
         cmd.set("#detailGrowth.Value", "");
      } else if (this.selectedAnimalName.startsWith("CUSTOM:")) {
         String modelId = this.selectedAnimalName.substring("CUSTOM:".length());
         CustomAnimalConfig custom = config.getCustomAnimal(modelId);
         if (custom != null) {
            cmd.set("#detailTitle.Text", "[C] " + custom.getDisplayName());
            cmd.set("#detailCooldown.Value", String.valueOf(custom.getBreedCooldownMinutes()));
            cmd.set("#detailGrowth.Value", String.valueOf(custom.getGrowthTimeMinutes()));
         } else {
            cmd.set("#detailTitle.Text", "Unknown custom animal");
            cmd.set("#detailCooldown.Value", "");
            cmd.set("#detailGrowth.Value", "");
         }

      } else {
         try {
            AnimalType animal = AnimalType.valueOf(this.selectedAnimalName);
            ConfigManager.AnimalConfig animalConfig = config.getAnimalConfig(animal);
            cmd.set("#detailTitle.Text", animal.getModelAssetId());
            if (animalConfig != null) {
               cmd.set("#detailCooldown.Value", String.valueOf(animalConfig.breedCooldownMinutes));
               cmd.set("#detailGrowth.Value", String.valueOf(animalConfig.growthTimeMinutes));
            } else {
               cmd.set("#detailCooldown.Value", "0");
               cmd.set("#detailGrowth.Value", "0");
            }
         } catch (Exception var5) {
            cmd.set("#detailTitle.Text", "Unknown animal");
            cmd.set("#detailCooldown.Value", "");
            cmd.set("#detailGrowth.Value", "");
         }

      }
   }

   private void populateFoodPicker(UICommandBuilder cmd, UIEventBuilder events, ConfigManager config) {
      cmd.clear("#foodList");
      cmd.clear("#customActions");
      if (this.showingRolePicker) {
         this.populateRolePicker(cmd, events, config);
      } else if (this.selectedAnimalName != null) {
         if (!this.selectedAnimalName.startsWith("CUSTOM:")) {
            List<String> enabledFoods;
            try {
               AnimalType animal = AnimalType.valueOf(this.selectedAnimalName);
               ConfigManager.AnimalConfig ac = config.getAnimalConfig(animal);
               enabledFoods = (List<String>)(ac != null ? ac.getEffectiveBreedingFoods() : new ArrayList());
            } catch (Exception var18) {
               enabledFoods = new ArrayList();
            }

            List<String> discoveredFoods = discoverFoodItems(enabledFoods);
            List<String[]> filtered = this.getFilteredFoods(enabledFoods, discoveredFoods);

            for(int i = 0; i < filtered.size(); ++i) {
               String[] food = (String[])filtered.get(i);
               String foodId = food[0];
               String displayName = food[1];
               if (foodId.equals("__SEPARATOR__")) {
                  cmd.appendInline("#foodList", "Group { Anchor: (Height: 24, Bottom: 2); Background: #1a1a2a; Label { Anchor: (Left: 8); Text: \"" + displayName + "\"; } }");
               } else {
                  boolean enabled = enabledFoods.contains(foodId);
                  String sel = "#foodList[" + i + "]";
                  cmd.append("#foodList", "Pages/FoodRow.ui");
                  String foodIconPath = this.getFoodIconPath(foodId);
                  if (foodIconPath != null) {
                     cmd.appendInline(sel + " #foodIcon", "Group { Anchor: (Width: 24, Height: 24); Background: \"" + foodIconPath + "\"; }");
                  } else {
                     cmd.set(sel + " #foodIcon.Background", enabled ? "#2a6a2a" : "#1a1a2a");
                  }

                  String label = displayName + (enabled ? "  [ON]" : "  [OFF]");
                  cmd.set(sel + " #foodBtn.Text", label);
                  cmd.set(sel + " #foodAction.Value", "TOGGLE_FOOD:" + foodId);
                  if (enabled) {
                     cmd.set(sel + ".Background", "#3a5a8a");
                  }

                  if (!this.readOnly) {
                     events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #foodBtn", (new EventData()).append("@action", sel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
                  }
               }
            }

         } else {
            String modelId = this.selectedAnimalName.substring("CUSTOM:".length());
            CustomAnimalConfig custom = config.getCustomAnimal(modelId);
            if (custom != null) {
               List<String> enabledFoods = custom.getBreedingFoods();
               List<String> discoveredFoods = discoverFoodItems(enabledFoods);
               List<String[]> filtered = this.getFilteredFoods(enabledFoods, discoveredFoods);
               int idx = 0;

               for(int i = 0; i < filtered.size(); ++i) {
                  String[] food = (String[])filtered.get(i);
                  String foodId = food[0];
                  String displayName = food[1];
                  if (foodId.equals("__SEPARATOR__")) {
                     cmd.appendInline("#foodList", "Group { Anchor: (Height: 24, Bottom: 2); Background: #1a1a2a; Label { Anchor: (Left: 8); Text: \"" + displayName + "\"; } }");
                     ++idx;
                  } else {
                     boolean enabled = enabledFoods.contains(foodId);
                     String sel = "#foodList[" + idx + "]";
                     cmd.append("#foodList", "Pages/FoodRow.ui");
                     String foodIconPath = this.getFoodIconPath(foodId);
                     if (foodIconPath != null) {
                        cmd.appendInline(sel + " #foodIcon", "Group { Anchor: (Width: 24, Height: 24); Background: \"" + foodIconPath + "\"; }");
                     } else {
                        cmd.set(sel + " #foodIcon.Background", enabled ? "#2a6a2a" : "#1a1a2a");
                     }

                     String label = displayName + (enabled ? "  [ON]" : "  [OFF]");
                     cmd.set(sel + " #foodBtn.Text", label);
                     cmd.set(sel + " #foodAction.Value", "TOGGLE_FOOD_CUSTOM:" + foodId);
                     if (enabled) {
                        cmd.set(sel + ".Background", "#3a5a8a");
                     }

                     if (!this.readOnly) {
                        events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #foodBtn", (new EventData()).append("@action", sel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
                     }

                     ++idx;
                  }
               }

               int caIdx = 0;
               String babyRole = custom.getBabyNpcRoleId();
               String babyDisplay = babyRole != null && !babyRole.isEmpty() ? babyRole : "None (uses scaling)";
               String babySel = "#customActions[" + caIdx + "]";
               cmd.append("#customActions", "Pages/FoodRow.ui");
               cmd.set(babySel + " #foodBtn.Text", "Baby: " + babyDisplay);
               cmd.set(babySel + " #foodAction.Value", "SET_BABY_SHOW");
               cmd.set(babySel + " #foodIcon.Background", "#2a3a5a");
               if (!this.readOnly) {
                  events.addEventBinding(CustomUIEventBindingType.Activating, babySel + " #foodBtn", (new EventData()).append("@action", babySel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
               }

               ++caIdx;
               if (babyRole != null && !babyRole.isEmpty()) {
                  String clearSel = "#customActions[" + caIdx + "]";
                  cmd.append("#customActions", "Pages/FoodRow.ui");
                  cmd.set(clearSel + " #foodBtn.Text", "Clear Baby Variant");
                  cmd.set(clearSel + " #foodAction.Value", "CLEAR_BABY");
                  cmd.set(clearSel + " #foodIcon.Background", "#4a2a2a");
                  if (!this.readOnly) {
                     events.addEventBinding(CustomUIEventBindingType.Activating, clearSel + " #foodBtn", (new EventData()).append("@action", clearSel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
                  }

                  ++caIdx;
               }

               String delSel = "#customActions[" + caIdx + "]";
               cmd.append("#customActions", "Pages/FoodRow.ui");
               cmd.set(delSel + " #foodBtn.Text", "[X] DELETE CUSTOM ANIMAL");
               cmd.set(delSel + " #foodAction.Value", "DELETE_CUSTOM:" + modelId);
               cmd.set(delSel + " #foodIcon.Background", "#6a2a2a");
               if (!this.readOnly) {
                  events.addEventBinding(CustomUIEventBindingType.Activating, delSel + " #foodBtn", (new EventData()).append("@action", delSel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
               }

            }
         }
      }
   }

   private void populateRolePicker(UICommandBuilder cmd, UIEventBuilder events, ConfigManager config) {
      Map<String, List<String>> rolesByMod = EcsReflectionUtil.enumerateNpcRolesByMod();
      Set<String> existingCustom = config.getCustomAnimals().keySet();
      String filter = this.roleSearchFilter != null ? this.roleSearchFilter.toLowerCase() : "";
      String actionPrefix = "baby".equals(this.rolePickerMode) ? "SET_BABY:" : "ADD_CUSTOM_CONFIRM:";
      this.log("populateRolePicker: mode=" + this.rolePickerMode + " filter='" + filter + "' rolesByMod.size=" + rolesByMod.size());

      for(Map.Entry<String, List<String>> e : rolesByMod.entrySet()) {
         String var10001 = (String)e.getKey();
         this.log("  mod='" + var10001 + "' roles=" + ((List)e.getValue()).size());
      }

      Set<String> hiddenMods = Set.of("Hytale", "HyTame", "Config_HyTame", "Hytalor-Overrides", "Unknown");
      Set<String> effectiveHidden = !filter.isEmpty() ? Set.of("HyTame", "Config_HyTame", "Hytalor-Overrides", "Unknown") : hiddenMods;
      Map<String, List<String>> filteredByMod = new LinkedHashMap();
      int totalFiltered = 0;

      for(Map.Entry<String, List<String>> modEntry : rolesByMod.entrySet()) {
         if (!effectiveHidden.contains(modEntry.getKey())) {
            List<String> modFiltered = new ArrayList();

            for(String role : (List)modEntry.getValue()) {
               if (!role.startsWith("Template_")) {
                  if ("baby".equals(this.rolePickerMode)) {
                     if (filter.isEmpty() || role.toLowerCase().contains(filter)) {
                        modFiltered.add(role);
                     }
                  } else if (AnimalType.fromModelAssetId(role) == null) {
                     boolean isBaby = false;

                     for(String suffix : BABY_SUFFIXES) {
                        if (role.endsWith(suffix)) {
                           isBaby = true;
                           break;
                        }
                     }

                     if (!isBaby && (!"add".equals(this.rolePickerMode) || !existingCustom.contains(role)) && (filter.isEmpty() || role.toLowerCase().contains(filter))) {
                        modFiltered.add(role);
                     }
                  }
               }
            }

            if (!modFiltered.isEmpty()) {
               filteredByMod.put((String)modEntry.getKey(), modFiltered);
               totalFiltered += modFiltered.size();
            }
         }
      }

      int var38 = filteredByMod.size();
      this.log("populateRolePicker: filteredByMod.size=" + var38 + " totalFiltered=" + totalFiltered);

      for(Map.Entry<String, List<String>> e : filteredByMod.entrySet()) {
         String var39 = (String)e.getKey();
         this.log("  filtered mod='" + var39 + "' roles=" + ((List)e.getValue()).size());
      }

      if (rolesByMod.isEmpty()) {
         cmd.appendInline("#foodList", "Group { Anchor: (Height: 30); Background: #2a2a3a; Padding: (Left: 8); Label { Text: \"No NPC roles found. Type a role name in search.\"; Style: (FontSize: 11, TextColor: #aaaaaa); } }");
      } else if (filteredByMod.isEmpty()) {
         cmd.appendInline("#foodList", "Group { Anchor: (Height: 30); Background: #2a2a3a; Padding: (Left: 8); Label { Text: \"No matching roles. Try a different search.\"; Style: (FontSize: 11, TextColor: #aaaaaa); } }");
      } else {
         int maxRoles = 50;
         boolean truncated = totalFiltered > maxRoles;
         int uiIdx = 0;
         if (truncated) {
            cmd.appendInline("#foodList", "Group { Anchor: (Height: 24, Bottom: 2); Background: #2a2a3a; Padding: (Left: 8); Label { Text: \"Showing up to " + maxRoles + " of " + totalFiltered + " — use search to narrow\"; Style: (FontSize: 10, TextColor: #aaaaaa); } }");
            ++uiIdx;
         }

         int remaining = maxRoles;

         for(Map.Entry<String, List<String>> modEntry : filteredByMod.entrySet()) {
            if (remaining <= 0) {
               break;
            }

            String modName = (String)modEntry.getKey();
            List<String> roles = (List)modEntry.getValue();
            int showCount = Math.min(roles.size(), remaining);
            cmd.appendInline("#foodList", "Group { Anchor: (Height: 26, Bottom: 2, Top: 4); Background: #2a2a3a; Padding: (Left: 8); Label { Text: \"" + modName + " (" + roles.size() + " roles)\"; Style: (FontSize: 11, RenderBold: true, TextColor: #ff9933, VerticalAlignment: Center); } }");
            ++uiIdx;
            if ("add".equals(this.rolePickerMode)) {
               String addAllSel = "#foodList[" + uiIdx + "]";
               cmd.append("#foodList", "Pages/FoodRow.ui");
               cmd.set(addAllSel + " #foodBtn.Text", "[+] Add All " + modName);
               cmd.set(addAllSel + " #foodAction.Value", "ADD_ALL_MOD:" + modName);
               cmd.set(addAllSel + " #foodIcon.Background", "#2a5a2a");
               if (!this.readOnly) {
                  events.addEventBinding(CustomUIEventBindingType.Activating, addAllSel + " #foodBtn", (new EventData()).append("@action", addAllSel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
               }

               ++uiIdx;
            }

            for(int i = 0; i < showCount; ++i) {
               String roleName = (String)roles.get(i);
               String sel = "#foodList[" + uiIdx + "]";
               cmd.append("#foodList", "Pages/FoodRow.ui");
               if ("baby".equals(this.rolePickerMode)) {
                  cmd.appendInline(sel + " #foodIcon", "Group { Anchor: (Width: 24, Height: 24); Background: (TexturePath: \"Pages/Icons/Npc_Default.png\"); }");
               } else {
                  cmd.set(sel + " #foodIcon.Background", "#2a4a2a");
               }

               cmd.set(sel + " #foodBtn.Text", roleName.replace("_", " "));
               cmd.set(sel + " #foodAction.Value", actionPrefix + roleName);
               if (!this.readOnly) {
                  events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #foodBtn", (new EventData()).append("@action", sel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
               }

               ++uiIdx;
            }

            remaining -= showCount;
         }

         String cancelSel = "#foodList[" + uiIdx + "]";
         cmd.append("#foodList", "Pages/FoodRow.ui");
         cmd.set(cancelSel + " #foodBtn.Text", "[<] CANCEL");
         cmd.set(cancelSel + " #foodAction.Value", "CANCEL_ROLE_PICKER");
         cmd.set(cancelSel + " #foodIcon.Background", "#4a3a1a");
         if (!this.readOnly) {
            events.addEventBinding(CustomUIEventBindingType.Activating, cancelSel + " #foodBtn", (new EventData()).append("@action", cancelSel + " #foodAction.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
         }

      }
   }

   private List<String[]> getFilteredFoods(List<String> enabledFoods) {
      return this.getFilteredFoods(enabledFoods, Collections.emptyList());
   }

   private List<String[]> getFilteredFoods(List<String> enabledFoods, List<String> discoveredFoods) {
      String filter = this.foodSearchFilter != null ? this.foodSearchFilter.toLowerCase() : "";
      List<String[]> foodEnabled = new ArrayList();
      List<String[]> foodDisabled = new ArrayList();

      for(String[] food : KNOWN_FOODS) {
         if (filter.isEmpty() || food[1].toLowerCase().contains(filter) || food[0].toLowerCase().contains(filter)) {
            if (enabledFoods.contains(food[0])) {
               foodEnabled.add(food);
            } else {
               foodDisabled.add(food);
            }
         }
      }

      List<String[]> matEnabled = new ArrayList();
      List<String[]> matDisabled = new ArrayList();

      for(String[] mat : KNOWN_MATERIALS) {
         if (filter.isEmpty() || mat[1].toLowerCase().contains(filter) || mat[0].toLowerCase().contains(filter)) {
            if (enabledFoods.contains(mat[0])) {
               matEnabled.add(mat);
            } else {
               matDisabled.add(mat);
            }
         }
      }

      foodEnabled.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
      foodDisabled.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
      matEnabled.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
      matDisabled.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
      Set<String> customSeen = new HashSet();
      List<String[]> customEnabled = new ArrayList();
      List<String[]> customDisabled = new ArrayList();

      for(String foodId : enabledFoods) {
         if (!ALL_KNOWN_ITEM_IDS.contains(foodId)) {
            String displayName = formatItemDisplayName(foodId);
            if (filter.isEmpty() || displayName.toLowerCase().contains(filter) || foodId.toLowerCase().contains(filter)) {
               customEnabled.add(new String[]{foodId, displayName});
               customSeen.add(foodId);
            }
         }
      }

      for(String foodId : discoveredFoods) {
         if (!customSeen.contains(foodId)) {
            String displayName = formatItemDisplayName(foodId);
            if (filter.isEmpty() || displayName.toLowerCase().contains(filter) || foodId.toLowerCase().contains(filter)) {
               customDisabled.add(new String[]{foodId, displayName});
               customSeen.add(foodId);
            }
         }
      }

      customEnabled.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
      customDisabled.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
      List<String[]> result = new ArrayList(foodEnabled);
      result.addAll(foodDisabled);
      boolean hasMaterials = !matEnabled.isEmpty() || !matDisabled.isEmpty();
      if (hasMaterials) {
         result.add(SEPARATOR_SENTINEL);
         result.addAll(matEnabled);
         result.addAll(matDisabled);
      }

      if (!customEnabled.isEmpty() || !customDisabled.isEmpty()) {
         result.add(new String[]{"__SEPARATOR__", "Custom / Mod Items"});
         result.addAll(customEnabled);
         result.addAll(customDisabled);
      }

      return result;
   }

   private static String formatItemDisplayName(String itemId) {
      String name = itemId;
      if (itemId.endsWith("_Item")) {
         name = itemId.substring(0, itemId.length() - 5);
      }

      String[] prefixes = new String[]{"Food_", "Plant_Crop_", "Plant_Fruit_", "Plant_", "Ingredient_"};

      for(String prefix : prefixes) {
         if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
            break;
         }
      }

      return name.replace("_", " ");
   }

   private static List<String> discoverFoodItems(List<String> enabledFoods) {
      List<String> allItems = EcsReflectionUtil.enumerateAllItemIds();
      List<String> discovered = new ArrayList();

      for(String itemId : allItems) {
         if (!ALL_KNOWN_ITEM_IDS.contains(itemId) && !enabledFoods.contains(itemId)) {
            boolean isFood = false;

            for(String prefix : FOOD_ITEM_PREFIXES) {
               if (itemId.startsWith(prefix)) {
                  isFood = true;
                  break;
               }
            }

            if (isFood) {
               discovered.add(itemId);
            }
         }
      }

      return discovered;
   }

   private void populateTamedAnimalList(UICommandBuilder cmd, UIEventBuilder events, Ref<EntityStore> ref, Store<EntityStore> store) {
      cmd.clear("#animalRows");
      this.tamedUiIndexMap.clear();
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null) {
         TamingManager tamingManager = plugin.getTamingManager();
         if (tamingManager != null) {
            UUID playerUuid = null;

            try {
               Player player = (Player)store.getComponent(ref, Player.getComponentType());
               if (player != null) {
                  playerUuid = player.getUuid();
               }
            } catch (Exception e) {
               this.log("Failed to get player UUID for tamed tab: " + e.getMessage());
            }

            Collection<TamedAnimalData> animals;
            if (!this.readOnly) {
               animals = tamingManager.getAllTamedAnimals();
            } else if (playerUuid != null) {
               animals = tamingManager.getPlayerAnimals(playerUuid);
            } else {
               animals = Collections.emptyList();
            }

            List<TamedAnimalData> filtered = new ArrayList();
            String filterLower = this.tamedSearchFilter != null && !this.tamedSearchFilter.isEmpty() ? this.tamedSearchFilter.toLowerCase() : null;

            for(TamedAnimalData data : animals) {
               if (filterLower == null) {
                  filtered.add(data);
               } else {
                  boolean matches = false;
                  if (data.getCustomName() != null && !data.getCustomName().equals("_UNDEFINED") && data.getCustomName().toLowerCase().contains(filterLower)) {
                     matches = true;
                  } else if (data.getAnimalType() != null && data.getAnimalType().name().toLowerCase().contains(filterLower)) {
                     matches = true;
                  } else if (data.getModelAssetId() != null && data.getModelAssetId().toLowerCase().contains(filterLower)) {
                     matches = true;
                  } else if (data.getOwnerName() != null && data.getOwnerName().toLowerCase().contains(filterLower)) {
                     matches = true;
                  }

                  if (matches) {
                     filtered.add(data);
                  }
               }
            }

            filtered.sort(Comparator.comparing((d) -> d.getAnimalType() != null ? d.getAnimalType().getCategory().ordinal() : Integer.MAX_VALUE).thenComparing((d) -> getDisplayName(d.getCustomName(), "")));
            int var10002 = filtered.size();
            cmd.set("#tamedSubtitle.Text", "Showing " + var10002 + " of " + animals.size() + " tamed animals.");
            boolean backfilled = false;

            for(TamedAnimalData data : filtered) {
               if (data.getAnimalType() == null && data.getModelAssetId() == null) {
                  Ref<EntityStore> entityRef = data.getEntityRef();
                  if (entityRef != null && entityRef.isValid()) {
                     Store<EntityStore> eStore = entityRef.getStore();
                     if (eStore != null) {
                        String modelId = EcsReflectionUtil.getEntityModelAssetId(eStore, entityRef);
                        if (modelId != null) {
                           data.setModelAssetId(modelId);
                           backfilled = true;
                        }
                     }
                  }
               }
            }

            if (backfilled) {
               tamingManager.saveImmediately();
            }

            int uiIndex = 0;
            AnimalType.Category lastCategory = null;

            for(TamedAnimalData data : filtered) {
               AnimalType.Category cat = data.getAnimalType() != null ? data.getAnimalType().getCategory() : null;
               if (cat != lastCategory) {
                  lastCategory = cat;
                  String catName;
                  if (cat != null) {
                     catName = getCategoryDisplayName(cat);
                  } else {
                     String modName = data.getModelAssetId() != null ? EcsReflectionUtil.getModNameForNpcRole(data.getModelAssetId()) : null;
                     catName = modName != null ? modName : "Custom";
                  }

                  cmd.appendInline("#animalRows", "Group { Anchor: (Height: 30, Bottom: 4, Top: 4); Background: #2a2a3a; Padding: (Left: 10); Label { Text: \"" + catName + "\"; Style: (FontSize: 13, RenderBold: true, TextColor: #ffaa00, VerticalAlignment: Center); } }");
                  ++uiIndex;
               }

               String sel = "#animalRows[" + uiIndex + "]";
               String hytameId = data.getHytameId() != null ? data.getHytameId().toString() : "";
               if (!hytameId.isEmpty()) {
                  this.tamedUiIndexMap.put(hytameId, uiIndex);
               }

               cmd.append("#animalRows", "Pages/TamedRow.ui");
               String displayName = getDisplayName(data.getCustomName(), "Unnamed");
               if (!this.readOnly) {
                  String listOwner = data.getOwnerName();
                  if ((listOwner == null || "Unknown".equals(listOwner)) && data.getOwnerUuid() != null) {
                     String resolved = this.resolvePlayerName(data.getOwnerUuid());
                     if (resolved != null) {
                        data.setOwnerName(resolved);
                        listOwner = resolved;
                     }
                  }

                  if (listOwner != null) {
                     displayName = displayName + " (" + listOwner + ")";
                  }
               }

               cmd.set(sel + " #selectBtn.Text", displayName);
               String iconPath = data.getAnimalType() != null ? this.getAnimalIconPath(data.getAnimalType()) : "Pages/Icons/Npc_Default.png";
               String bgColor = data.getAnimalType() != null ? this.getAnimalBackgroundColor(data.getAnimalType()) : "#3a3a3a";
               cmd.appendInline(sel + " #icon", animalIconMarkup(36, iconPath, bgColor));
               String statusText;
               String statusColor;
               if (data.isCaptured()) {
                  statusText = "CAPTURED";
                  statusColor = "#5599ff";
               } else if (data.isDead()) {
                  statusText = "DEAD";
                  statusColor = "#ff5555";
               } else if (data.isDespawned()) {
                  statusText = "DESPAWNED";
                  statusColor = "#ffaa00";
               } else {
                  statusText = "ALIVE";
                  statusColor = "#55ff55";
               }

               cmd.appendInline(sel + " #statusContainer", "Label { Text: \"" + statusText + "\"; Style: (FontSize: 11, TextColor: " + statusColor + ", Alignment: Center, VerticalAlignment: Center); }");
               String growthText = data.getGrowthStage() != null ? data.getGrowthStage().getDisplayName() : "Adult";
               cmd.set(sel + " #growthLabel.Text", growthText);
               if (hytameId.equals(this.selectedTamedId)) {
                  cmd.set(sel + ".Background", "#3a5a8a");
               }

               cmd.set(sel + " #selectAction.Value", "SELECT_TAMED:" + hytameId);
               events.addEventBinding(CustomUIEventBindingType.Activating, sel + " #selectBtn", (new EventData()).append("@action", sel + " #selectAction.Value").append("@tamedSearch", "#tamedSearch.Value"));
               ++uiIndex;
            }

         }
      }
   }

   private void populateTamedDetailPanel(UICommandBuilder cmd) {
      if (this.selectedTamedId == null) {
         cmd.set("#tamedDetailTitle.Text", "Select a tamed animal");
         cmd.set("#tamedDetailType.Text", "-");
         cmd.set("#tamedDetailOwner.Text", "-");
         cmd.appendInline("#tamedDetailStatus", "Label { Text: \"-\"; Style: (FontSize: 12, TextColor: #cccccc); }");
         cmd.set("#tamedDetailGrowth.Text", "-");
         cmd.set("#tamedDetailPosition.Text", "-");
         cmd.set("#tamedDetailTamedTime.Text", "-");
         cmd.set("#tamedRenameInput.Value", "");
      } else {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            TamingManager tamingManager = plugin.getTamingManager();
            if (tamingManager != null) {
               TamedAnimalData data;
               try {
                  data = tamingManager.findByHytameId(UUID.fromString(this.selectedTamedId));
               } catch (Exception var13) {
                  data = null;
               }

               if (data == null) {
                  cmd.set("#tamedDetailTitle.Text", "Animal not found");
                  cmd.set("#tamedDetailType.Text", "-");
                  cmd.set("#tamedDetailOwner.Text", "-");
                  cmd.appendInline("#tamedDetailStatus", "Label { Text: \"-\"; Style: (FontSize: 12, TextColor: #cccccc); }");
                  cmd.set("#tamedDetailGrowth.Text", "-");
                  cmd.set("#tamedDetailPosition.Text", "-");
                  cmd.set("#tamedDetailTamedTime.Text", "-");
                  cmd.set("#tamedRenameInput.Value", "");
               } else {
                  String title = getDisplayName(data.getCustomName(), "Unnamed");
                  cmd.set("#tamedDetailTitle.Text", title);
                  String typeName;
                  if (data.getAnimalType() != null) {
                     typeName = data.getAnimalType().getModelAssetId().replace("_", " ");
                  } else if (data.getModelAssetId() != null) {
                     ConfigManager config = plugin.getConfigManager();
                     CustomAnimalConfig custom = config != null ? config.getCustomAnimal(data.getModelAssetId()) : null;
                     typeName = custom != null ? custom.getDisplayName() : data.getModelAssetId().replace("_", " ");
                  } else {
                     typeName = "Unknown";
                  }

                  cmd.set("#tamedDetailType.Text", typeName);
                  String ownerDisplay = data.getOwnerName();
                  if (ownerDisplay == null && data.getOwnerUuid() != null) {
                     ownerDisplay = this.resolvePlayerName(data.getOwnerUuid());
                     if (ownerDisplay != null) {
                        data.setOwnerName(ownerDisplay);
                        tamingManager.saveImmediately();
                     }
                  }

                  cmd.set("#tamedDetailOwner.Text", ownerDisplay != null ? ownerDisplay : "Unknown");
                  String statusColor;
                  String statusText;
                  if (data.isCaptured()) {
                     statusText = "Captured";
                     statusColor = "#5599ff";
                  } else if (data.isDead()) {
                     statusText = "Dead";
                     statusColor = "#ff5555";
                  } else if (data.isDespawned()) {
                     statusText = "Despawned";
                     statusColor = "#ffaa00";
                  } else {
                     statusText = "Alive";
                     statusColor = "#55ff55";
                  }

                  cmd.appendInline("#tamedDetailStatus", "Label { Text: \"" + statusText + "\"; Style: (FontSize: 12, TextColor: " + statusColor + "); }");
                  String growth = data.getGrowthStage() != null ? data.getGrowthStage().getDisplayName() : "Adult";
                  cmd.set("#tamedDetailGrowth.Text", growth);
                  String pos = String.format("%.0f, %.0f, %.0f", data.getLastX(), data.getLastY(), data.getLastZ());
                  cmd.set("#tamedDetailPosition.Text", pos);
                  if (data.getTamedTime() > 0L) {
                     String formatted = Instant.ofEpochMilli(data.getTamedTime()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                     cmd.set("#tamedDetailTamedTime.Text", formatted);
                  } else {
                     cmd.set("#tamedDetailTamedTime.Text", "Unknown");
                  }

                  cmd.set("#tamedRenameInput.Value", getDisplayName(data.getCustomName(), ""));
               }
            }
         }
      }
   }

   private void setupTamedEventBindings(UICommandBuilder cmd, UIEventBuilder events) {
      cmd.set("#actionSwitchToList.Value", "SWITCH_TAB:list");
      cmd.set("#actionSwitchToTamed.Value", "SWITCH_TAB:tamed");
      cmd.set("#actionSearchAnimals.Value", "SEARCH_TAMED");
      cmd.set("#actionTamedRename.Value", "TAMED_RENAME");
      cmd.set("#actionTamedRelease.Value", "TAMED_RELEASE");
      if (!this.readOnly) {
         cmd.set("#actionSwitchToEdit.Value", "SWITCH_TAB:edit");
         events.addEventBinding(CustomUIEventBindingType.Activating, "#tabEditBtn", (new EventData()).append("@action", "#actionSwitchToEdit.Value").append("@tamedSearch", "#tamedSearch.Value"));
      }

      events.addEventBinding(CustomUIEventBindingType.Activating, "#tabListBtn", (new EventData()).append("@action", "#actionSwitchToList.Value").append("@tamedSearch", "#tamedSearch.Value"));
      events.addEventBinding(CustomUIEventBindingType.Activating, "#tabTamedBtn", (new EventData()).append("@action", "#actionSwitchToTamed.Value").append("@tamedSearch", "#tamedSearch.Value"));
      events.addEventBinding(CustomUIEventBindingType.Activating, "#tamedSearchBtn", (new EventData()).append("@action", "#actionSearchAnimals.Value").append("@tamedSearch", "#tamedSearch.Value"));
      events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#tamedSearch", (new EventData()).append("@action", "#actionSearchAnimals.Value").append("@tamedSearch", "#tamedSearch.Value"));
      events.addEventBinding(CustomUIEventBindingType.Activating, "#tamedRenameBtn", (new EventData()).append("@action", "#actionTamedRename.Value").append("@tamedRename", "#tamedRenameInput.Value").append("@tamedSearch", "#tamedSearch.Value"));
      events.addEventBinding(CustomUIEventBindingType.Activating, "#tamedReleaseBtn", (new EventData()).append("@action", "#actionTamedRelease.Value").append("@tamedSearch", "#tamedSearch.Value"));
   }

   private EventData buildDetailEventData(String actionRef) {
      EventData data = (new EventData()).append("@action", actionRef).append("@animalSearch", "#animalSearch.Value");
      if (!this.isListView()) {
         data.append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@presetRename", "#presetRenameInput.Value").append("@perPlayerLimit", "#perPlayerLimitInput.Value");
         if (HyTamePlugin.getInstance() != null && HyTamePlugin.getInstance().isSimpleClaimsInstalled()) {
            data.append("@perClaimLimit", "#perClaimLimitInput.Value");
         }
      }

      return data;
   }

   private void setupEventBindings(UICommandBuilder cmd, UIEventBuilder events) {
      cmd.set("#actionSearchAnimals.Value", "SEARCH_ANIMALS");
      if (!this.isListView()) {
         cmd.set("#actionSearchFoods.Value", "SEARCH_FOODS");
         cmd.set("#actionToggleGrowth.Value", "TOGGLE_GROWTH");
         cmd.set("#actionSave.Value", "SAVE");
         cmd.set("#actionSaveAsPreset.Value", "SAVE_AS_PRESET");
         cmd.set("#actionAddPreset.Value", "ADD_PRESET");
         cmd.set("#actionSearchPresets.Value", "SEARCH_PRESETS");
         cmd.set("#actionApplyGrowthAll.Value", "APPLY_GROWTH_ALL");
         cmd.set("#actionApplyCooldownAll.Value", "APPLY_COOLDOWN_ALL");
         cmd.set("#actionRestorePresets.Value", "RESTORE_ALL_PRESETS");
         cmd.set("#actionTogglePerPlayerLimit.Value", "TOGGLE_PER_PLAYER_LIMIT");
         if (HyTamePlugin.getInstance() != null && HyTamePlugin.getInstance().isSimpleClaimsInstalled()) {
            cmd.set("#actionTogglePerClaimLimit.Value", "TOGGLE_PER_CLAIM_LIMIT");
         }
      }

      if (!this.isListView()) {
         events.addEventBinding(CustomUIEventBindingType.Activating, "#applyGrowthAllBtn", this.buildDetailEventData("#actionApplyGrowthAll.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#applyCooldownAllBtn", this.buildDetailEventData("#actionApplyCooldownAll.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#growthToggleBtn", this.buildDetailEventData("#actionToggleGrowth.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#perPlayerLimitToggleBtn", this.buildDetailEventData("#actionTogglePerPlayerLimit.Value"));
         if (HyTamePlugin.getInstance() != null && HyTamePlugin.getInstance().isSimpleClaimsInstalled()) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#perClaimLimitToggleBtn", this.buildDetailEventData("#actionTogglePerClaimLimit.Value"));
         }

         events.addEventBinding(CustomUIEventBindingType.Activating, "#saveBtn", this.buildDetailEventData("#actionSave.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#renamePresetBtn", (new EventData()).append("@action", "#actionRenamePreset.Value").append("@presetRename", "#presetRenameInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#saveAsPresetBtn", (new EventData()).append("@action", "#actionSaveAsPreset.Value").append("@detailCooldown", "#detailCooldown.Value").append("@detailGrowth", "#detailGrowth.Value").append("@foodSearch", "#foodSearch.Value").append("@growthTime", "#growthTimeInput.Value").append("@cooldown", "#cooldownInput.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value").append("@presetRename", "#presetRenameInput.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#addPresetBtn", (new EventData()).append("@action", "#actionAddPreset.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value"));
         events.addEventBinding(CustomUIEventBindingType.Activating, "#restorePresetsBtn", (new EventData()).append("@action", "#actionRestorePresets.Value").append("@presetSearch", "#presetSearch.Value").append("@animalSearch", "#animalSearch.Value"));
      }

      if (!this.isListView()) {
         events.addEventBinding(CustomUIEventBindingType.Activating, "#presetSearchBtn", this.buildDetailEventData("#actionSearchPresets.Value"));
         events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#presetSearch", this.buildDetailEventData("#actionSearchPresets.Value"));
      }

      events.addEventBinding(CustomUIEventBindingType.Activating, "#animalSearchBtn", this.buildDetailEventData("#actionSearchAnimals.Value"));
      events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#animalSearch", this.buildDetailEventData("#actionSearchAnimals.Value"));
      if (!this.isListView()) {
         events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#foodSearch", this.buildDetailEventData("#actionSearchFoods.Value"));
      }

      cmd.set("#actionSwitchToList.Value", "SWITCH_TAB:list");
      cmd.set("#actionSwitchToTamed.Value", "SWITCH_TAB:tamed");
      events.addEventBinding(CustomUIEventBindingType.Activating, "#tabListBtn", this.buildDetailEventData("#actionSwitchToList.Value"));
      events.addEventBinding(CustomUIEventBindingType.Activating, "#tabTamedBtn", this.buildDetailEventData("#actionSwitchToTamed.Value"));
      if (!this.readOnly) {
         cmd.set("#actionSwitchToEdit.Value", "SWITCH_TAB:edit");
         events.addEventBinding(CustomUIEventBindingType.Activating, "#tabEditBtn", this.buildDetailEventData("#actionSwitchToEdit.Value"));
      }

   }

   private boolean applyGlobalSettings(ConfigEventData data, ConfigManager config) {
      boolean changed = false;
      if (data.growthTime != null && !data.growthTime.isEmpty()) {
         try {
            double val = Double.parseDouble(data.growthTime.trim());
            if (val > (double)0.0F && val != config.getDefaultGrowthTimeMinutes()) {
               config.setDefaultGrowthTime(val);
               changed = true;
            }
         } catch (NumberFormatException var9) {
         }
      }

      if (data.cooldown != null && !data.cooldown.isEmpty()) {
         try {
            double val = Double.parseDouble(data.cooldown.trim());
            if (val >= (double)0.0F && val != config.getDefaultBreedCooldownMinutes()) {
               config.setDefaultBreedCooldown(val);
               changed = true;
            }
         } catch (NumberFormatException var8) {
         }
      }

      if (data.perPlayerLimit != null && !data.perPlayerLimit.isEmpty()) {
         try {
            int val = Integer.parseInt(data.perPlayerLimit.trim());
            if (val > 0 && val != config.getPerPlayerTameLimit()) {
               config.setPerPlayerTameLimit(val);
               changed = true;
            }
         } catch (NumberFormatException var7) {
         }
      }

      if (data.perClaimLimit != null && !data.perClaimLimit.isEmpty()) {
         try {
            int val = Integer.parseInt(data.perClaimLimit.trim());
            if (val > 0 && val != config.getPerClaimTameLimit()) {
               config.setPerClaimTameLimit(val);
               changed = true;
            }
         } catch (NumberFormatException var6) {
         }
      }

      return changed;
   }

   private void preserveDetailEdits(ConfigEventData data, ConfigManager config) {
      if (this.selectedAnimalName != null) {
         boolean changed = false;
         if (this.applyGlobalSettings(data, config)) {
            changed = true;
            this.globalDirty = true;
         }

         if (this.selectedAnimalName.startsWith("CUSTOM:")) {
            String modelId = this.selectedAnimalName.substring("CUSTOM:".length());
            CustomAnimalConfig custom = config.getCustomAnimal(modelId);
            if (custom == null) {
               if (changed) {
                  this.dirty = true;
               }

            } else {
               if (data.detailCooldown != null && !data.detailCooldown.isEmpty()) {
                  try {
                     double val = Double.parseDouble(data.detailCooldown.trim());
                     if (val >= (double)0.0F && val != custom.getBreedCooldownMinutes()) {
                        config.setCustomAnimalCooldown(modelId, val);
                        changed = true;
                     }
                  } catch (NumberFormatException var10) {
                  }
               }

               if (data.detailGrowth != null && !data.detailGrowth.isEmpty()) {
                  try {
                     double val = Double.parseDouble(data.detailGrowth.trim());
                     if (val > (double)0.0F && val != custom.getGrowthTimeMinutes()) {
                        config.setCustomAnimalGrowthTime(modelId, val);
                        changed = true;
                     }
                  } catch (NumberFormatException var9) {
                  }
               }

               if (changed) {
                  this.dirty = true;
               }

            }
         } else {
            try {
               AnimalType animal = AnimalType.valueOf(this.selectedAnimalName);
               ConfigManager.AnimalConfig ac = config.getAnimalConfig(animal);
               if (ac == null) {
                  if (changed) {
                     this.dirty = true;
                  }

                  return;
               }

               boolean animalChanged = false;
               if (data.detailCooldown != null && !data.detailCooldown.isEmpty()) {
                  try {
                     double val = Double.parseDouble(data.detailCooldown.trim());
                     if (val >= (double)0.0F && val != ac.breedCooldownMinutes) {
                        config.setBreedingCooldown(animal, val);
                        animalChanged = true;
                     }
                  } catch (NumberFormatException var12) {
                  }
               }

               if (data.detailGrowth != null && !data.detailGrowth.isEmpty()) {
                  try {
                     double val = Double.parseDouble(data.detailGrowth.trim());
                     if (val > (double)0.0F && val != ac.growthTimeMinutes) {
                        config.setGrowthTime(animal, val);
                        animalChanged = true;
                     }
                  } catch (NumberFormatException var11) {
                  }
               }

               if (animalChanged) {
                  changed = true;
                  this.dirtyAnimals.add(animal);
               }
            } catch (Exception var13) {
            }

            if (changed) {
               this.dirty = true;
            }

         }
      }
   }

   public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ConfigEventData data) {
      try {
         Player player = (Player)store.getComponent(ref, Player.getComponentType());
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            this.closePage(player, ref, store);
            return;
         }

         ConfigManager config = plugin.getConfigManager();
         if (config == null) {
            this.closePage(player, ref, store);
            return;
         }

         if (data.presetSearch != null) {
            this.presetSearchFilter = data.presetSearch;
         }

         if (data.animalSearch != null) {
            this.animalSearchFilter = data.animalSearch;
         }

         if (data.foodSearch != null) {
            this.foodSearchFilter = data.foodSearch;
         }

         String action = data.action;
         if (action == null) {
            action = "";
         }

         action = action.trim();
         this.log("Config panel action: " + action);
         if (data.tamedSearch != null) {
            this.tamedSearchFilter = data.tamedSearch;
         }

         if (this.readOnly && !action.equals("SEARCH_PRESETS") && !action.equals("SEARCH_ANIMALS") && !action.equals("SEARCH_FOODS") && !action.startsWith("SELECT_ANIMAL:") && !action.startsWith("SELECT_CUSTOM:") && !action.equals("SEARCH_TAMED") && !action.startsWith("SELECT_TAMED:") && !action.equals("TAMED_RENAME") && !action.equals("TAMED_RELEASE") && !action.startsWith("SWITCH_TAB:")) {
            if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Settings are read-only.").color("#FF5555"));
            }

            return;
         }

         if (action.equals("SEARCH_PRESETS")) {
            this.preserveDetailEdits(data, config);
            this.presetSearchFilter = data.presetSearch != null ? data.presetSearch : "";
            List<String> presets = this.getFilteredPresets(config);
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populatePresetList(cmd, ev, presets, config.getActivePreset(), config);
            cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.equals("SEARCH_ANIMALS")) {
            if (!this.isListView()) {
               this.preserveDetailEdits(data, config);
            }

            this.animalSearchFilter = data.animalSearch != null ? data.animalSearch : "";
            List<AnimalType> animals = this.getFilteredAnimals();
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateAnimalTable(cmd, ev, animals, config);
            if (!this.isListView()) {
               this.populateDetailPanel(cmd, config);
               this.populateFoodPicker(cmd, ev, config);
               cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
            }

            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.equals("SEARCH_FOODS")) {
            if (this.showingRolePicker) {
               this.roleSearchFilter = data.foodSearch != null ? data.foodSearch : "";
            } else {
               this.foodSearchFilter = data.foodSearch != null ? data.foodSearch : "";
            }

            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateFoodPicker(cmd, ev, config);
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("TOGGLE_FOOD:")) {
            String foodId = action.substring("TOGGLE_FOOD:".length());
            this.preserveDetailEdits(data, config);
            if (this.selectedAnimalName != null) {
               try {
                  AnimalType animal = AnimalType.valueOf(this.selectedAnimalName);
                  ConfigManager.AnimalConfig ac = config.getAnimalConfig(animal);
                  if (ac != null) {
                     List<String> foods = new ArrayList(ac.getEffectiveBreedingFoods());
                     if (foods.contains(foodId)) {
                        foods.remove(foodId);
                     } else {
                        foods.add(foodId);
                     }

                     config.setBreedingFoods(animal, foods);
                     this.dirty = true;
                     this.dirtyAnimals.add(animal);
                     UICommandBuilder cmd = new UICommandBuilder();
                     UIEventBuilder ev = new UIEventBuilder();
                     this.populateFoodPicker(cmd, ev, config);
                     cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
                     this.sendPartialUpdate(cmd, ev);
                  }
               } catch (Exception e) {
                  this.log("Error toggling food: " + e.getMessage());
               }
            }

            return;
         }

         if (action.startsWith("SELECT_ANIMAL:")) {
            String animalName = action.substring("SELECT_ANIMAL:".length());
            if (!this.isListView()) {
               this.preserveDetailEdits(data, config);
            }

            int oldIdx = this.findAnimalIndex(this.selectedAnimalName);
            int newIdx = this.findAnimalIndex(animalName);
            this.selectedAnimalName = animalName;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            if (oldIdx >= 0) {
               cmd.set("#animalRows[" + oldIdx + "].Background", "#00000000");
            }

            if (newIdx >= 0) {
               cmd.set("#animalRows[" + newIdx + "].Background", "#3a5a8a");
            }

            if (!this.isListView()) {
               this.foodSearchFilter = "";
               this.populateDetailPanel(cmd, config);
               this.populateFoodPicker(cmd, ev, config);
               cmd.set("#foodSearch.Value", "");
               cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
            }

            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("SWITCH_TAB:")) {
            String newTab = action.substring("SWITCH_TAB:".length());
            if (this.readOnly && "edit".equals(newTab)) {
               newTab = "list";
            }

            if (!this.isListView() && !this.isTamedView()) {
               this.preserveDetailEdits(data, config);
            }

            this.activeTab = newTab;
            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("SEARCH_TAMED")) {
            this.tamedSearchFilter = data.tamedSearch != null ? data.tamedSearch : "";
            this.reopenPage(player, ref, store);
            return;
         }

         if (action.startsWith("SELECT_TAMED:")) {
            String tamedId = action.substring("SELECT_TAMED:".length());
            int oldIdx = this.findTamedIndex(this.selectedTamedId);
            int newIdx = this.findTamedIndex(tamedId);
            this.selectedTamedId = tamedId;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            if (oldIdx >= 0) {
               cmd.set("#animalRows[" + oldIdx + "].Background", "#00000000");
            }

            if (newIdx >= 0) {
               cmd.set("#animalRows[" + newIdx + "].Background", "#3a5a8a");
            }

            cmd.clear("#tamedDetailStatus");
            this.populateTamedDetailPanel(cmd);
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.equals("TAMED_RENAME")) {
            if (this.selectedTamedId != null && data.tamedRename != null && !data.tamedRename.trim().isEmpty()) {
               TamingManager tamingManager = plugin.getTamingManager();
               if (tamingManager == null) {
                  return;
               }

               TamedAnimalData tamedData;
               try {
                  tamedData = tamingManager.findByHytameId(UUID.fromString(this.selectedTamedId));
               } catch (Exception var33) {
                  return;
               }

               if (tamedData == null) {
                  if (player != null) {
                     player.getPlayerRef().sendMessage(Message.raw("Animal not found.").color("#FF5555"));
                  }

                  return;
               }

               UUID playerUuid = null;

               try {
                  playerUuid = player != null ? player.getUuid() : null;
               } catch (Exception var32) {
               }

               UUID renameAsUuid = !this.readOnly ? tamedData.getOwnerUuid() : playerUuid;
               boolean success = tamingManager.renameAnimal(tamedData.getAnimalUuid(), renameAsUuid, data.tamedRename.trim());
               if (success && player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Renamed to: " + data.tamedRename.trim()).color("#55FF55"));
               } else if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Cannot rename: not the owner.").color("#FF5555"));
               }

               this.reopenPage(player, ref, store);
               return;
            }

            return;
         }

         if (action.equals("TAMED_RELEASE")) {
            if (this.selectedTamedId == null) {
               return;
            }

            TamingManager tamingManager = plugin.getTamingManager();
            if (tamingManager == null) {
               return;
            }

            TamedAnimalData tamedData;
            try {
               tamedData = tamingManager.findByHytameId(UUID.fromString(this.selectedTamedId));
            } catch (Exception var31) {
               return;
            }

            if (tamedData == null) {
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Animal not found.").color("#FF5555"));
               }

               return;
            }

            UUID playerUuid = null;

            try {
               playerUuid = player != null ? player.getUuid() : null;
            } catch (Exception var30) {
            }

            UUID releaseAsUuid = !this.readOnly ? tamedData.getOwnerUuid() : playerUuid;
            boolean success = tamingManager.releaseAnimal(UUID.fromString(this.selectedTamedId), releaseAsUuid);
            if (success) {
               String animalName = getDisplayName(tamedData.getCustomName(), "animal");
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Released " + animalName + ".").color("#55FF55"));
               }

               this.selectedTamedId = null;
            } else if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Cannot release: not the owner.").color("#FF5555"));
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.startsWith("TOGGLE_BREED:")) {
            String animalName = action.substring("TOGGLE_BREED:".length());
            this.preserveDetailEdits(data, config);

            try {
               AnimalType animal = AnimalType.valueOf(animalName);
               ConfigManager.AnimalConfig animalConfig = config.getAnimalConfig(animal);
               if (animalConfig != null) {
                  boolean newState = !animalConfig.breedingEnabled;
                  config.setAnimalEnabled(animal, newState);
                  this.dirty = true;
                  this.dirtyAnimals.add(animal);
                  int idx = this.findAnimalIndex(animalName);
                  UICommandBuilder cmd = new UICommandBuilder();
                  if (idx >= 0) {
                     cmd.set("#animalRows[" + idx + "] #breedToggle.Text", newState ? "ON" : "OFF");
                     cmd.set("#animalRows[" + idx + "] #breedToggle.Background", newState ? "#2a6a2a" : "#6a2a2a");
                  }

                  cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
                  this.sendUpdate(cmd);
                  if (player != null) {
                     PlayerRef var173 = player.getPlayerRef();
                     String var176 = animal.getModelAssetId();
                     var173.sendMessage(Message.raw(var176 + " breeding " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
                  }
               }
            } catch (Exception e) {
               this.log("Error toggling animal breeding: " + e.getMessage());
            }

            return;
         }

         if (action.startsWith("TOGGLE_TAMING:")) {
            String animalName = action.substring("TOGGLE_TAMING:".length());
            this.preserveDetailEdits(data, config);

            try {
               AnimalType animal = AnimalType.valueOf(animalName);
               if (animal.usesVanillaTaming()) {
                  if (player != null) {
                     player.getPlayerRef().sendMessage(Message.raw(animal.getModelAssetId() + " uses vanilla taming (always on)").color("#AAAAAA"));
                  }

                  this.sendUpdate(new UICommandBuilder());
                  return;
               }

               ConfigManager.AnimalConfig animalConfig = config.getAnimalConfig(animal);
               if (animalConfig != null) {
                  boolean newState = !animalConfig.tamingEnabled;
                  config.setTamingEnabled(animal, newState);
                  this.dirty = true;
                  this.dirtyAnimals.add(animal);
                  int idx = this.findAnimalIndex(animalName);
                  UICommandBuilder cmd = new UICommandBuilder();
                  if (idx >= 0) {
                     cmd.set("#animalRows[" + idx + "] #tameToggle.Text", newState ? "ON" : "OFF");
                     cmd.set("#animalRows[" + idx + "] #tameToggle.Background", newState ? "#2a6a2a" : "#6a2a2a");
                  }

                  cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
                  this.sendUpdate(cmd);
                  if (player != null) {
                     PlayerRef var172 = player.getPlayerRef();
                     String var175 = animal.getModelAssetId();
                     var172.sendMessage(Message.raw(var175 + " taming " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
                  }
               }
            } catch (Exception e) {
               this.log("Error toggling animal taming: " + e.getMessage());
            }

            return;
         }

         if (action.startsWith("SELECT_PRESET:")) {
            String presetName = action.substring("SELECT_PRESET:".length());
            String oldPreset = config.getActivePreset();
            if (config.applyPreset(presetName) && player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Applied preset: " + presetName).color("#55FF55"));
            }

            this.dirty = true;
            this.globalDirty = true;
            this.dirtyAnimals.clear();
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            int oldPresetIdx = this.findPresetIndex(oldPreset);
            int newPresetIdx = this.findPresetIndex(presetName);
            if (oldPresetIdx >= 0) {
               cmd.set("#presetList[" + oldPresetIdx + "].Background", "#00000000");
            }

            if (newPresetIdx >= 0) {
               cmd.set("#presetList[" + newPresetIdx + "].Background", "#2a3a5a");
            }

            this.populatePresetSettings(cmd, config);

            for(int i = 0; i < this.currentFilteredAnimals.size(); ++i) {
               AnimalType animal = (AnimalType)this.currentFilteredAnimals.get(i);
               int rowIdx = this.findAnimalIndex(animal.name());
               if (rowIdx >= 0) {
                  String rowSel = "#animalRows[" + rowIdx + "]";
                  ConfigManager.AnimalConfig ac = config.getAnimalConfig(animal);
                  boolean breedEnabled = ac != null && ac.breedingEnabled;
                  boolean tameEnabled = ac != null && ac.tamingEnabled;
                  cmd.set(rowSel + " #breedToggle.Text", breedEnabled ? "ON" : "OFF");
                  cmd.set(rowSel + " #breedToggle.Background", breedEnabled ? "#2a6a2a" : "#6a2a2a");
                  if (!animal.usesVanillaTaming()) {
                     cmd.set(rowSel + " #tameToggle.Text", tameEnabled ? "ON" : "OFF");
                     cmd.set(rowSel + " #tameToggle.Background", tameEnabled ? "#2a6a2a" : "#6a2a2a");
                  }

                  cmd.clear(rowSel + " #foodIcons");
                  List<String> foods = ac != null ? ac.getEffectiveBreedingFoods() : Collections.singletonList(animal.getDefaultBreedingFood());
                  int maxIcons = 3;

                  for(int f = 0; f < Math.min(foods.size(), maxIcons); ++f) {
                     String foodIconPath = this.getFoodIconPath((String)foods.get(f));
                     StringBuilder foodMarkup = new StringBuilder();
                     foodMarkup.append("Group {\n");
                     foodMarkup.append("  Anchor: (Width: 24, Height: 24, Right: 2);\n");
                     if (foodIconPath != null) {
                        foodMarkup.append("  Background: (TexturePath: \"").append(foodIconPath).append("\");\n");
                     } else {
                        foodMarkup.append("  Background: #3a3a3a;\n");
                     }

                     foodMarkup.append("}");
                     cmd.appendInline(rowSel + " #foodIcons", foodMarkup.toString());
                  }

                  if (foods.size() > maxIcons) {
                     cmd.appendInline(rowSel + " #foodIcons", "Label { Anchor: (Width: 20, Height: 24); Text: \"...\"; }");
                  }
               }
            }

            this.populateDetailPanel(cmd, config);
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
            if (!config.isBuiltinPreset(presetName)) {
               cmd.set("#saveAsPresetBtn.Text", "SAVE AS \"" + presetName + "\" PRESET");
            } else {
               cmd.set("#saveAsPresetBtn.Text", "SAVE AS NEW PRESET");
            }

            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("COPY_PRESET:")) {
            String sourceName = action.substring("COPY_PRESET:".length());
            List<String> existing = config.getAvailablePresets();
            String copyName = sourceName + "_copy";

            for(int counter = 1; existing.contains(copyName); ++counter) {
               copyName = sourceName + "_copy_" + counter;
            }

            if (config.copyPreset(sourceName, copyName)) {
               config.applyPreset(copyName);
               this.dirty = false;
               this.globalDirty = false;
               this.dirtyAnimals.clear();
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Copied preset: " + sourceName + " -> " + copyName).color("#55FF55"));
               }
            } else if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Failed to copy preset.").color("#FF5555"));
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("RESTORE_PRESET")) {
            String presetName = config.getActivePreset();
            if (presetName != null && config.isBuiltinPreset(presetName)) {
               if (config.restorePreset(presetName)) {
                  config.applyPreset(presetName);
                  this.dirty = false;
                  this.globalDirty = false;
                  this.dirtyAnimals.clear();
                  if (player != null) {
                     player.getPlayerRef().sendMessage(Message.raw("Restored built-in preset: " + presetName).color("#55FF55"));
                  }
               } else if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Failed to restore preset.").color("#FF5555"));
               }
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("RENAME_PRESET")) {
            String newName = data.presetRename != null ? data.presetRename.trim() : "";
            String oldName = config.getActivePreset();
            this.log("Rename: old='" + oldName + "' new='" + newName + "'");
            if (newName.isEmpty()) {
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Enter a new name for the preset.").color("#FF5555"));
               }

               this.reopenPage(player, ref, store);
               return;
            }

            if (newName.equals(oldName)) {
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Name unchanged ('" + newName + "').").color("#FFAA00"));
               }

               this.reopenPage(player, ref, store);
               return;
            }

            if (config.isBuiltinPreset(oldName)) {
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Cannot rename built-in preset: " + oldName).color("#FF5555"));
               }

               this.reopenPage(player, ref, store);
               return;
            }

            if (config.renamePreset(oldName, newName)) {
               config.saveToFile();
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Renamed preset: " + oldName + " -> " + newName).color("#55FF55"));
               }
            } else if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Failed to rename. Use only letters, numbers, _ and - (no spaces).").color("#FF5555"));
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("APPLY_GROWTH_ALL")) {
            this.preserveDetailEdits(data, config);
            if (data.growthTime != null && !data.growthTime.isEmpty()) {
               try {
                  double val = Double.parseDouble(data.growthTime.trim());
                  if (val > (double)0.0F) {
                     config.setDefaultGrowthTime(val);
                     this.dirty = true;
                     this.globalDirty = true;
                     if (player != null) {
                        player.getPlayerRef().sendMessage(Message.raw("Growth time set to " + val + " min for all animals").color("#55FF55"));
                     }
                  }
               } catch (NumberFormatException var27) {
               }
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("APPLY_COOLDOWN_ALL")) {
            this.preserveDetailEdits(data, config);
            if (data.cooldown != null && !data.cooldown.isEmpty()) {
               try {
                  double val = Double.parseDouble(data.cooldown.trim());
                  if (val >= (double)0.0F) {
                     config.setDefaultBreedCooldown(val);
                     this.dirty = true;
                     this.globalDirty = true;
                     if (player != null) {
                        player.getPlayerRef().sendMessage(Message.raw("Breed cooldown set to " + val + " min for all animals").color("#55FF55"));
                     }
                  }
               } catch (NumberFormatException var26) {
               }
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("TOGGLE_GROWTH")) {
            this.preserveDetailEdits(data, config);
            boolean newState = !config.isGrowthEnabled();
            config.setGrowthEnabled(newState);
            if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Baby growth " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
            }

            this.dirty = true;
            this.globalDirty = true;
            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("TOGGLE_PER_PLAYER_LIMIT")) {
            this.preserveDetailEdits(data, config);
            boolean newState = !config.isUsePerPlayerLimit();
            config.setUsePerPlayerLimit(newState);
            if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Per-player tame limit " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
            }

            this.dirty = true;
            this.globalDirty = true;
            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("TOGGLE_PER_CLAIM_LIMIT")) {
            if (plugin != null && plugin.isSimpleClaimsInstalled()) {
               this.preserveDetailEdits(data, config);
               boolean newState = !config.isUsePerClaimLimit();
               config.setUsePerClaimLimit(newState);
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Per-claim tame limit " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
               }

               this.dirty = true;
               this.globalDirty = true;
               this.reopenPage(player, ref, store);
               return;
            }

            return;
         }

         if (action.equals("SAVE")) {
            this.applyGlobalSettings(data, config);
            this.preserveDetailEdits(data, config);
            config.saveToFile();
            PatchSyncService patchSync = plugin.getPatchSyncService();
            if (patchSync != null) {
               if (this.globalDirty) {
                  patchSync.syncAllPatches();
               } else {
                  for(AnimalType animal : this.dirtyAnimals) {
                     patchSync.syncForAnimal(animal);
                  }

                  for(CustomAnimalConfig custom : config.getCustomAnimals().values()) {
                     patchSync.syncForCustomAnimal(custom);
                  }
               }
            }

            this.dirty = false;
            this.globalDirty = false;
            this.dirtyAnimals.clear();
            if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Configuration saved and patches synced!").color("#55FF55"));
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("SAVE_AS_PRESET")) {
            this.applyGlobalSettings(data, config);
            this.preserveDetailEdits(data, config);
            String presetName = config.getActivePreset();
            if (presetName != null && !config.isBuiltinPreset(presetName)) {
               if (config.saveAsPreset(presetName)) {
                  if (player != null) {
                     player.getPlayerRef().sendMessage(Message.raw("Saved preset: " + presetName).color("#55FF55"));
                  }
               } else if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Failed to save preset.").color("#FF5555"));
               }
            } else {
               String baseName = "custom";
               List<String> existing = config.getAvailablePresets();
               String newName = baseName;

               for(int counter = 1; existing.contains(newName); ++counter) {
                  newName = baseName + "_" + counter;
               }

               if (config.saveAsPreset(newName)) {
                  config.applyPreset(newName);
                  if (player != null) {
                     player.getPlayerRef().sendMessage(Message.raw("Created preset: " + newName).color("#55FF55"));
                  }
               } else if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Failed to create preset.").color("#FF5555"));
               }
            }

            this.dirty = false;
            this.globalDirty = false;
            this.dirtyAnimals.clear();
            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("ADD_PRESET")) {
            this.preserveDetailEdits(data, config);
            String baseName = "custom";
            List<String> existing = config.getAvailablePresets();
            String newName = baseName;

            for(int counter = 1; existing.contains(newName); ++counter) {
               newName = baseName + "_" + counter;
            }

            if (config.saveAsPreset(newName)) {
               config.applyPreset(newName);
               this.dirty = false;
               this.globalDirty = false;
               this.dirtyAnimals.clear();
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Created preset: " + newName).color("#55FF55"));
               }
            } else if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Failed to create preset.").color("#FF5555"));
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("RESTORE_ALL_PRESETS")) {
            String[] builtins = new String[]{"default", "default_extended", "lait_curated", "zoo", "all"};
            int restored = 0;

            for(String name : builtins) {
               if (config.restorePreset(name)) {
                  ++restored;
               }
            }

            if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("Restored " + restored + " built-in presets to defaults.").color("#55FF55"));
            }

            String active = config.getActivePreset();
            if (active != null && config.isBuiltinPreset(active)) {
               config.applyPreset(active);
               this.dirty = false;
               this.globalDirty = false;
               this.dirtyAnimals.clear();
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.startsWith("SELECT_CUSTOM:")) {
            String modelId = action.substring("SELECT_CUSTOM:".length());
            if (!this.isListView()) {
               this.preserveDetailEdits(data, config);
            }

            String mapKey = "CUSTOM:" + modelId;
            int oldIdx = this.findAnimalIndex(this.selectedAnimalName);
            int newIdx = this.findAnimalIndex(mapKey);
            this.selectedAnimalName = mapKey;
            this.showingRolePicker = false;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            if (oldIdx >= 0) {
               cmd.set("#animalRows[" + oldIdx + "].Background", "#00000000");
            }

            if (newIdx >= 0) {
               cmd.set("#animalRows[" + newIdx + "].Background", "#3a5a8a");
            }

            if (!this.isListView()) {
               this.foodSearchFilter = "";
               this.populateDetailPanel(cmd, config);
               this.populateFoodPicker(cmd, ev, config);
               cmd.set("#foodSearch.Value", "");
               cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
            }

            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("TOGGLE_BREED_CUSTOM:")) {
            String modelId = action.substring("TOGGLE_BREED_CUSTOM:".length());
            this.preserveDetailEdits(data, config);
            CustomAnimalConfig custom = config.getCustomAnimal(modelId);
            if (custom != null) {
               boolean newState = !custom.isBreedingEnabled();
               config.setCustomAnimalBreedingEnabled(modelId, newState);
               this.dirty = true;
               int idx = this.findAnimalIndex("CUSTOM:" + modelId);
               UICommandBuilder cmd = new UICommandBuilder();
               if (idx >= 0) {
                  cmd.set("#animalRows[" + idx + "] #breedToggle.Text", newState ? "ON" : "OFF");
                  cmd.set("#animalRows[" + idx + "] #breedToggle.Background", newState ? "#2a6a2a" : "#6a2a2a");
               }

               cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
               this.sendUpdate(cmd);
               if (player != null) {
                  PlayerRef var171 = player.getPlayerRef();
                  String var174 = custom.getDisplayName();
                  var171.sendMessage(Message.raw(var174 + " breeding " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
               }
            }

            return;
         }

         if (action.startsWith("TOGGLE_TAMING_CUSTOM:")) {
            String modelId = action.substring("TOGGLE_TAMING_CUSTOM:".length());
            this.preserveDetailEdits(data, config);
            CustomAnimalConfig custom = config.getCustomAnimal(modelId);
            if (custom != null) {
               boolean newState = !custom.isTamingEnabled();
               config.setCustomAnimalTamingEnabled(modelId, newState);
               this.dirty = true;
               int idx = this.findAnimalIndex("CUSTOM:" + modelId);
               UICommandBuilder cmd = new UICommandBuilder();
               if (idx >= 0) {
                  cmd.set("#animalRows[" + idx + "] #tameToggle.Text", newState ? "ON" : "OFF");
                  cmd.set("#animalRows[" + idx + "] #tameToggle.Background", newState ? "#2a6a2a" : "#6a2a2a");
               }

               cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
               this.sendUpdate(cmd);
               if (player != null) {
                  PlayerRef var10000 = player.getPlayerRef();
                  String var10001 = custom.getDisplayName();
                  var10000.sendMessage(Message.raw(var10001 + " taming " + (newState ? "enabled" : "disabled")).color(newState ? "#55FF55" : "#FF9900"));
               }
            }

            return;
         }

         if (action.startsWith("TOGGLE_FOOD_CUSTOM:")) {
            String foodId = action.substring("TOGGLE_FOOD_CUSTOM:".length());
            this.preserveDetailEdits(data, config);
            if (this.selectedAnimalName != null && this.selectedAnimalName.startsWith("CUSTOM:")) {
               String modelId = this.selectedAnimalName.substring("CUSTOM:".length());
               CustomAnimalConfig custom = config.getCustomAnimal(modelId);
               if (custom != null) {
                  List<String> foods = custom.getBreedingFoods();
                  if (foods.contains(foodId)) {
                     config.removeCustomAnimalFood(modelId, foodId);
                  } else {
                     config.addCustomAnimalFood(modelId, foodId);
                  }

                  this.dirty = true;
                  UICommandBuilder cmd = new UICommandBuilder();
                  UIEventBuilder ev = new UIEventBuilder();
                  this.populateFoodPicker(cmd, ev, config);
                  cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
                  this.sendPartialUpdate(cmd, ev);
               }
            }

            return;
         }

         if (action.equals("ADD_CUSTOM_SHOW")) {
            this.preserveDetailEdits(data, config);
            this.showingRolePicker = true;
            this.rolePickerMode = "add";
            this.roleSearchFilter = "";
            int oldIdx = this.findAnimalIndex(this.selectedAnimalName);
            this.selectedAnimalName = null;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            if (oldIdx >= 0) {
               cmd.set("#animalRows[" + oldIdx + "].Background", "#00000000");
            }

            this.populateDetailPanel(cmd, config);
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#foodSearch.Value", "");
            cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("ADD_CUSTOM_CONFIRM:")) {
            String roleName = action.substring("ADD_CUSTOM_CONFIRM:".length());
            if (config.isCustomAnimal(roleName)) {
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("'" + roleName + "' is already registered.").color("#FFAA00"));
               }
            } else {
               List<String> defaultFoods = EcsReflectionUtil.readRoleLovedItems(roleName);
               if (defaultFoods.isEmpty()) {
                  defaultFoods = new ArrayList();
                  defaultFoods.add("Plant_Crop_Wheat_Item");
               }

               config.addCustomAnimal(roleName, defaultFoods);
               config.setCustomAnimalNpcRole(roleName, roleName);
               PatchSyncService patchSync = plugin.getPatchSyncService();
               if (patchSync != null) {
                  CustomAnimalConfig custom = config.getCustomAnimal(roleName);
                  if (custom != null) {
                     patchSync.syncForCustomAnimal(custom);
                  }
               }

               this.dirty = true;
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Added: " + roleName).color("#55FF55"));
               }
            }

            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateDetailPanel(cmd, config);
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#saveBtn.Text", this.dirty ? "SAVE AS CURRENT CONFIG *" : "SAVE AS CURRENT CONFIG");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("ADD_ALL_MOD:")) {
            String modName = action.substring("ADD_ALL_MOD:".length());
            Map<String, List<String>> rolesByMod = EcsReflectionUtil.enumerateNpcRolesByMod();
            List<String> modRoles = (List)rolesByMod.get(modName);
            int added = 0;
            if (modRoles != null) {
               for(String role : modRoles) {
                  if (!role.startsWith("Template_") && AnimalType.fromModelAssetId(role) == null) {
                     boolean isBaby = false;

                     for(String suffix : BABY_SUFFIXES) {
                        if (role.endsWith(suffix)) {
                           isBaby = true;
                           break;
                        }
                     }

                     if (!isBaby && !config.isCustomAnimal(role)) {
                        List<String> defaultFoods = EcsReflectionUtil.readRoleLovedItems(role);
                        if (defaultFoods.isEmpty()) {
                           defaultFoods = new ArrayList();
                           defaultFoods.add("Plant_Crop_Wheat_Item");
                        }

                        config.addCustomAnimal(role, defaultFoods);
                        config.setCustomAnimalNpcRole(role, role);
                        ++added;
                     }
                  }
               }
            }

            if (added > 0) {
               this.dirty = true;
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Added " + added + " animals from " + modName).color("#55FF55"));
               }
            } else if (player != null) {
               player.getPlayerRef().sendMessage(Message.raw("All " + modName + " animals already registered.").color("#FFAA00"));
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.startsWith("DELETE_CUSTOM:")) {
            String modelId = action.substring("DELETE_CUSTOM:".length());
            this.preserveDetailEdits(data, config);
            if (config.removeCustomAnimal(modelId)) {
               this.dirty = true;
               this.selectedAnimalName = null;
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Removed custom animal: " + modelId).color("#FF9900"));
               }
            }

            this.reopenPage(player, ref, store);
            return;
         }

         if (action.equals("SET_BABY_SHOW")) {
            this.preserveDetailEdits(data, config);
            this.showingRolePicker = true;
            this.rolePickerMode = "baby";
            this.roleSearchFilter = "";
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateDetailPanel(cmd, config);
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#foodSearch.Value", "");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.startsWith("SET_BABY:")) {
            String babyRole = action.substring("SET_BABY:".length());
            this.showingRolePicker = false;
            if (this.selectedAnimalName != null && this.selectedAnimalName.startsWith("CUSTOM:")) {
               String modelId = this.selectedAnimalName.substring("CUSTOM:".length());
               config.setCustomAnimalBabyRole(modelId, babyRole);
               this.dirty = true;
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Baby variant set to: " + babyRole).color("#55FF55"));
               }
            }

            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateDetailPanel(cmd, config);
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#foodSearch.Value", "");
            cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.equals("CLEAR_BABY")) {
            this.preserveDetailEdits(data, config);
            if (this.selectedAnimalName != null && this.selectedAnimalName.startsWith("CUSTOM:")) {
               String modelId = this.selectedAnimalName.substring("CUSTOM:".length());
               config.setCustomAnimalBabyRole(modelId, (String)null);
               this.dirty = true;
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Baby variant cleared.").color("#FF9900"));
               }
            }

            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#saveBtn.Text", "SAVE AS CURRENT CONFIG *");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         if (action.equals("CANCEL_ROLE_PICKER")) {
            this.showingRolePicker = false;
            this.roleSearchFilter = "";
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            this.populateDetailPanel(cmd, config);
            this.populateFoodPicker(cmd, ev, config);
            cmd.set("#foodSearch.Value", "");
            this.sendPartialUpdate(cmd, ev);
            return;
         }

         this.closePage(player, ref, store);
      } catch (Exception e) {
         this.log("Error in handleDataEvent: " + e.getMessage());
      }

   }

   private void reopenPage(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
      if (player != null) {
         try {
            PlayerRef playerRef = player.getPlayerRef();
            player.getPageManager().openCustomPage(ref, store, new ConfigPanelUIPage(playerRef, this.presetSearchFilter, this.animalSearchFilter, this.selectedAnimalName, this.dirty, this.readOnly, this.activeTab, this.selectedTamedId, this.tamedSearchFilter));
         } catch (Exception e) {
            this.log("Failed to reopen config page: " + e.getMessage());
         }
      }

   }

   private void closePage(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
      if (player != null) {
         try {
            player.getPageManager().setPage(ref, store, Page.None);
         } catch (Exception e) {
            this.log("Failed to close page: " + e.getMessage());
         }
      }

   }

   private void applyReadOnlyStyle(UICommandBuilder cmd) {
      cmd.set("#addPresetBtn.Background", "#1a1a1a");
      cmd.set("#renamePresetBtn.Background", "#1a1a1a");
      cmd.set("#growthToggleBtn.Background", "#1a1a1a");
      cmd.set("#presetRenameInput.Background", "#1a1a1a");
      cmd.set("#growthTimeInput.Background", "#1a1a1a");
      cmd.set("#cooldownInput.Background", "#1a1a1a");
      cmd.set("#detailCooldown.Background", "#1a1a1a");
      cmd.set("#detailGrowth.Background", "#1a1a1a");
      cmd.set("#perPlayerLimitToggleBtn.Background", "#1a1a1a");
      cmd.set("#perPlayerLimitInput.Background", "#1a1a1a");
      if (HyTamePlugin.getInstance() != null && HyTamePlugin.getInstance().isSimpleClaimsInstalled()) {
         cmd.set("#perClaimLimitToggleBtn.Background", "#1a1a1a");
         cmd.set("#perClaimLimitInput.Background", "#1a1a1a");
      }

   }

   private String getAbbreviatedName(AnimalType animal) {
      String name = animal.getModelAssetId();
      if (name.contains("_")) {
         String[] parts = name.split("_");
         String base = parts[0].toUpperCase();
         String variant = parts.length > 1 ? parts[1] : "";
         if (base.length() <= 7 && !variant.isEmpty()) {
            return base + " " + variant.substring(0, 1).toUpperCase();
         } else {
            return base.length() > 9 ? base.substring(0, 8) + ".." : base;
         }
      } else {
         name = name.toUpperCase();
         return name.length() > 9 ? name.substring(0, 8) + ".." : name;
      }
   }

   private String getAnimalIconPath(AnimalType animal) {
      String id = animal.getModelAssetId();
      if (NO_MEMORY_ICON.contains(id)) {
         return "Pages/Icons/Npc_Default.png";
      } else {
         String fallback = (String)ICON_FALLBACK.get(id);
         String iconId = fallback != null ? fallback : id;
         return "Pages/Memories/npcs/" + iconId + ".png";
      }
   }

   private static String animalIconMarkup(int size, String texturePath, String fallbackColor) {
      StringBuilder b = new StringBuilder();
      b.append("Group {\n");
      b.append("  Anchor: (Width: ").append(size).append(", Height: ").append(size).append(");\n");
      if (texturePath != null) {
         b.append("  Background: (TexturePath: \"").append(texturePath).append("\");\n");
      } else {
         b.append("  Background: ").append(fallbackColor).append(";\n");
      }

      b.append("}");
      return b.toString();
   }

   private String getFoodIconPath(String foodId) {
      String iconId = foodId.endsWith("_Item") ? foodId.substring(0, foodId.length() - 5) : foodId;
      return KNOWN_ICON_IDS.contains(iconId) ? "Pages/Icons/" + iconId + ".png" : "Pages/Icons/Food_Default.png";
   }

   private String getAnimalBackgroundColor(AnimalType animal) {
      switch (animal.getCategory()) {
         case LIVESTOCK:
            return "#2a4a2a";
         case MAMMAL:
            return "#4a3a2a";
         case CRITTER:
            return "#4a4a2a";
         case AVIAN:
            return "#2a3a4a";
         case REPTILE:
            return "#3a4a2a";
         case VERMIN:
         case SCARAK:
            return "#4a2a4a";
         case AQUATIC:
            return "#2a4a4a";
         case MYTHIC:
            return "#4a2a3a";
         case DINOSAUR:
         case BOSS:
         case UNDEAD:
         case GOLEM:
         case SPIRIT:
         case GOBLIN:
         case TRORK:
         default:
            return "#2a2a3a";
      }
   }

   private int findAnimalIndex(String animalName) {
      if (animalName == null) {
         return -1;
      } else {
         Integer idx = (Integer)this.animalUiIndexMap.get(animalName);
         return idx != null ? idx : -1;
      }
   }

   private int findTamedIndex(String hytameId) {
      if (hytameId == null) {
         return -1;
      } else {
         Integer idx = (Integer)this.tamedUiIndexMap.get(hytameId);
         return idx != null ? idx : -1;
      }
   }

   private int findPresetIndex(String presetName) {
      if (presetName != null && this.currentFilteredPresets != null) {
         for(int i = 0; i < this.currentFilteredPresets.size(); ++i) {
            if (((String)this.currentFilteredPresets.get(i)).equals(presetName)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private void sendPartialUpdate(UICommandBuilder cmd, UIEventBuilder events) {
      Ref<EntityStore> ref = this.playerRef.getReference();
      if (ref != null) {
         Store<EntityStore> store = ref.getStore();
         Player playerComponent = (Player)store.getComponent(ref, Player.getComponentType());
         playerComponent.getPageManager().updateCustomPage(new CustomPage(this.getClass().getName(), false, false, this.getLifetime(), cmd.getCommands(), events.getEvents()));
      }
   }

   private static String formatMinutes(double minutes) {
      if (minutes < (double)60.0F) {
         return minutes == (double)((int)minutes) ? (int)minutes + "m" : String.format("%.1fm", minutes);
      } else {
         int hours = (int)(minutes / (double)60.0F);
         int mins = (int)(minutes % (double)60.0F);
         return mins == 0 ? hours + "h" : hours + "h " + mins + "m";
      }
   }

   private static String getDisplayName(String customName, String fallback) {
      return customName != null && !customName.equals("_UNDEFINED") ? customName : fallback;
   }

   private String resolvePlayerName(UUID ownerUuid) {
      if (ownerUuid == null) {
         return null;
      } else {
         try {
            Ref<EntityStore> ref = this.playerRef.getReference();
            if (ref == null) {
               return null;
            }

            Store<EntityStore> store = ref.getStore();
            if (store == null) {
               return null;
            }

            Player currentPlayer = (Player)store.getComponent(ref, Player.getComponentType());
            if (currentPlayer != null) {
               try {
                  if (ownerUuid.equals(currentPlayer.getUuid())) {
                     return currentPlayer.getLegacyDisplayName();
                  }
               } catch (Exception var6) {
               }
            }
         } catch (Exception e) {
            this.log("resolvePlayerName error: " + e.getMessage());
         }

         return null;
      }
   }

   private void log(String message) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null && HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[ConfigPanel] " + message);
      }

   }

   static {
      Set<String> ids = new HashSet();

      for(String[] food : KNOWN_FOODS) {
         String id = food[0].endsWith("_Item") ? food[0].substring(0, food[0].length() - 5) : food[0];
         ids.add(id);
      }

      for(String[] mat : KNOWN_MATERIALS) {
         String id = mat[0].endsWith("_Item") ? mat[0].substring(0, mat[0].length() - 5) : mat[0];
         ids.add(id);
      }

      KNOWN_ICON_IDS = ids;
      ids = new HashSet();

      for(String[] food : KNOWN_FOODS) {
         ids.add(food[0]);
      }

      for(String[] mat : KNOWN_MATERIALS) {
         ids.add(mat[0]);
      }

      ALL_KNOWN_ITEM_IDS = ids;
      BABY_SUFFIXES = Set.of("_Calf", "_Lamb", "_Chick", "_Piglet", "_Kitten", "_Pup", "_Foal", "_Cub", "_Hatchling", "_Fawn", "_Kid", "_Joey", "_Bunny");
      HIDDEN_CATEGORIES = Set.of(AnimalType.Category.GOBLIN, AnimalType.Category.TRORK, AnimalType.Category.SCARAK, AnimalType.Category.KWEEBEC, AnimalType.Category.OUTLANDER, AnimalType.Category.UNDEAD, AnimalType.Category.GOLEM);
      SEPARATOR_SENTINEL = new String[]{"__SEPARATOR__", "Special Items"};
      FOOD_ITEM_PREFIXES = new String[]{"Food_", "Plant_Crop_", "Plant_Fruit_", "Plant_Cactus_", "Ingredient_"};
      ICON_FALLBACK = Map.of("Dragon_Fire", "Dragon_Frost", "Skeleton", "Shadow_Knight", "Kweebec_Razorleaf", "Kweebec_Sapling", "Kweebec_Elder", "Kweebec_Sapling");
      NO_MEMORY_ICON = Set.of("Hatworm");
   }

   public static class ConfigEventData {
      public String action;
      public String growthTime;
      public String cooldown;
      public String presetSearch;
      public String animalSearch;
      public String presetRename;
      public String detailCooldown;
      public String detailGrowth;
      public String foodSearch;
      public String perPlayerLimit;
      public String perClaimLimit;
      public String tamedRename;
      public String tamedSearch;
      public static final BuilderCodec<ConfigEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(ConfigEventData.class, ConfigEventData::new).append(new KeyedCodec("@action", new StringCodec()), (obj, val) -> obj.action = val, (obj) -> obj.action).add()).append(new KeyedCodec("@growthTime", new StringCodec()), (obj, val) -> obj.growthTime = val, (obj) -> obj.growthTime).add()).append(new KeyedCodec("@cooldown", new StringCodec()), (obj, val) -> obj.cooldown = val, (obj) -> obj.cooldown).add()).append(new KeyedCodec("@presetSearch", new StringCodec()), (obj, val) -> obj.presetSearch = val, (obj) -> obj.presetSearch).add()).append(new KeyedCodec("@animalSearch", new StringCodec()), (obj, val) -> obj.animalSearch = val, (obj) -> obj.animalSearch).add()).append(new KeyedCodec("@presetRename", new StringCodec()), (obj, val) -> obj.presetRename = val, (obj) -> obj.presetRename).add()).append(new KeyedCodec("@detailCooldown", new StringCodec()), (obj, val) -> obj.detailCooldown = val, (obj) -> obj.detailCooldown).add()).append(new KeyedCodec("@detailGrowth", new StringCodec()), (obj, val) -> obj.detailGrowth = val, (obj) -> obj.detailGrowth).add()).append(new KeyedCodec("@foodSearch", new StringCodec()), (obj, val) -> obj.foodSearch = val, (obj) -> obj.foodSearch).add()).append(new KeyedCodec("@perPlayerLimit", new StringCodec()), (obj, val) -> obj.perPlayerLimit = val, (obj) -> obj.perPlayerLimit).add()).append(new KeyedCodec("@perClaimLimit", new StringCodec()), (obj, val) -> obj.perClaimLimit = val, (obj) -> obj.perClaimLimit).add()).append(new KeyedCodec("@tamedRename", new StringCodec()), (obj, val) -> obj.tamedRename = val, (obj) -> obj.tamedRename).add()).append(new KeyedCodec("@tamedSearch", new StringCodec()), (obj, val) -> obj.tamedSearch = val, (obj) -> obj.tamedSearch).add()).build();
   }
}
