package com.hytame;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.AllNPCsLoadedEvent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.hytame.commands.BreedCommand;
import com.hytame.commands.BreedingConfigCommand;
import com.hytame.commands.CustomAnimalCommand;
import com.hytame.commands.HytameCommand;
import com.hytame.commands.LegacyCommands;
import com.hytame.components.HyTameInteractionComponent;
import com.hytame.coop.CoopCodecExtender;
import com.hytame.effects.EffectsManager;
import com.hytame.handlers.MouseInteractionHandler;
import com.hytame.integration.SimpleClaimsIntegration;
import com.hytame.interactions.FeedAnimalInteraction;
import com.hytame.interactions.HyTameCaptureInteraction;
import com.hytame.interactions.InteractionStateCache;
import com.hytame.interactions.NameAnimalInteraction;
import com.hytame.interactions.OpenConfigPanelInteraction;
import com.hytame.listeners.CoopResidentTracker;
import com.hytame.listeners.DetectTamedDeath;
import com.hytame.listeners.NewAnimalSpawnDetector;
import com.hytame.listeners.UseBlockHandler;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.BreedingTickManager;
import com.hytame.managers.GrowthManager;
import com.hytame.managers.InteractionSetupManager;
import com.hytame.managers.PersistenceManager;
import com.hytame.managers.SpawningManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.TamedAnimalData;
import com.hytame.patch.PatchSyncService;
import com.hytame.tame.HyTameComponent;
import com.hytame.tame.HyTameSystems;
import com.hytame.tame.actions.BuilderActionClearBreedCooldownFlag;
import com.hytame.tame.actions.BuilderActionGrowToNextStage;
import com.hytame.tame.actions.BuilderActionHyTameFeedInteraction;
import com.hytame.tame.actions.BuilderActionRemovePlayerHeldItems;
import com.hytame.tame.sensors.BuilderSensorGrowthReady;
import com.hytame.tame.sensors.BuilderSensorIsTameable;
import com.hytame.tame.sensors.BuilderSensorNeedsBreedCooldown;
import com.hytame.tame.sensors.BuilderSensorScaledBaby;
import com.hytame.tame.sensors.BuilderSensorTamed;
import com.hytame.test.HyTameSelfTest;
import com.hytame.util.AnimalFinder;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.joml.Vector3d;

public class HyTamePlugin extends JavaPlugin {
   public static final String VERSION = "1.5.3";
   private static HyTamePlugin instance;
   private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClassFull();
   private static final Field ATTITUDE_FIELD;
   private ConfigManager configManager;
   private BreedingManager breedingManager;
   private GrowthManager growthManager;
   private TamingManager tamingManager;
   private PersistenceManager persistenceManager;
   private BreedingTickManager breedingTickManager;
   private EffectsManager effectsManager;
   private SpawningManager spawningManager;
   private InteractionSetupManager interactionSetupManager;
   private MouseInteractionHandler mouseInteractionHandler;
   private PatchSyncService patchSyncService;
   private Path configDirectory;
   private boolean hytalorInstalled = false;
   private static final String HYTALOR_WARNING = "[Warning] Hytalor not detected. Some features like taming hints and asset-based roles require Hytalor to be installed.";
   private boolean simpleClaimsInstalled = false;
   private SimpleClaimsIntegration simpleClaimsIntegration;
   private ComponentType<EntityStore, HyTameComponent> hyTameComponentType;
   private ComponentType<EntityStore, HyTameInteractionComponent> hyTameInteractionComponentType;
   private ScheduledExecutorService tickScheduler;
   private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList();
   private NewAnimalSpawnDetector spawnDetector;
   private volatile boolean firstPlayerConnected = false;
   private static final List<UUID> lastDetectedDespawns;
   private static final int MAX_DESPAWN_TRACKED = 10;
   private static boolean verboseLogging;
   private static boolean devMode;
   private static final boolean USE_ENTITY_BASED_INTERACTIONS = false;
   private static final boolean USE_LEGACY_FEED_INTERACTION = false;
   private static final boolean SHOW_ABILITY2_HINTS_ON_ENTITIES = true;
   private static final boolean USE_JAVA_BASED_GROWTH = false;
   private static final boolean USE_ALARM_BASED_BREED_COOLDOWN = true;

   public static Field getAttitudeField() {
      return ATTITUDE_FIELD;
   }

   public ScheduledExecutorService getTickScheduler() {
      return this.tickScheduler;
   }

   public NewAnimalSpawnDetector getSpawnDetector() {
      return this.spawnDetector;
   }

   public static List<UUID> getLastDetectedDespawns() {
      synchronized(lastDetectedDespawns) {
         return new ArrayList(lastDetectedDespawns);
      }
   }

   public static void clearTrackedDespawns() {
      lastDetectedDespawns.clear();
   }

   private static void trackDetectedDespawn(UUID uuid) {
      synchronized(lastDetectedDespawns) {
         lastDetectedDespawns.add(0, uuid);

         while(lastDetectedDespawns.size() > 10) {
            lastDetectedDespawns.remove(lastDetectedDespawns.size() - 1);
         }

      }
   }

   public static boolean isVerboseLogging() {
      return verboseLogging;
   }

   public static boolean isAlarmBasedBreedCooldown() {
      return true;
   }

   public static void setVerboseLogging(boolean enabled) {
      verboseLogging = enabled;
   }

   public static boolean isDevMode() {
      return devMode;
   }

   public static void setDevMode(boolean enabled) {
      devMode = enabled;
   }

   private void broadcastToChat(String message) {
      try {
         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               world.getPlayers().forEach((player) -> {
                  try {
                     player.getPlayerRef().sendMessage(Message.raw("[Breeding] " + message).color("#AAAAAA"));
                  } catch (Exception var3) {
                  }

               });
            }
         }
      } catch (Exception var5) {
      }

   }

   private void logVerbose(String message) {
      if (verboseLogging) {
         ((HytaleLogger.Api)LOGGER.atInfo()).log("[HyTame] " + message);
         if (devMode) {
            this.broadcastToChat(message);
         }
      }

   }

   private void logWarning(String message) {
      ((HytaleLogger.Api)LOGGER.atWarning()).log("[HyTame] " + message);
   }

   private void refreshMemories() {
      try {
         Class<?> memoriesPluginClass = Class.forName("com.hypixel.hytale.builtin.adventure.memories.MemoriesPlugin");
         Object memoriesPlugin = memoriesPluginClass.getMethod("get").invoke((Object)null);
         if (memoriesPlugin == null) {
            ((HytaleLogger.Api)this.getLogger().atWarning()).log("[HyTame] MemoriesPlugin.get() returned null");
            return;
         }

         Method onAssetsLoad = memoriesPluginClass.getDeclaredMethod("onAssetsLoad");
         onAssetsLoad.setAccessible(true);
         onAssetsLoad.invoke(memoriesPlugin);
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("[HyTame] Memories catalog refreshed after builder validation");
      } catch (ClassNotFoundException var4) {
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("[HyTame] MemoriesPlugin not found (memories feature may be disabled)");
      } catch (Exception e) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("[HyTame] Failed to refresh memories: %s", e.getMessage());
      }

   }

   private boolean detectHytalor() {
      try {
         PluginManager pm = HytaleServer.get().getPluginManager();
         if (pm == null) {
            ((HytaleLogger.Api)this.getLogger().atInfo()).log("PluginManager not available for Hytalor detection");
            return false;
         } else {
            for(PluginBase plugin : pm.getPlugins()) {
               String name = plugin.getName();
               if (name != null && name.toLowerCase().contains("hytalor")) {
                  ((HytaleLogger.Api)this.getLogger().atInfo()).log("Hytalor plugin detected: %s", name);
                  return true;
               }
            }

            return false;
         }
      } catch (Exception e) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("Failed to detect Hytalor: %s", e.getMessage());
         return false;
      }
   }

   public boolean isHytalorInstalled() {
      return this.hytalorInstalled;
   }

   public void warnHytalorRequired(String feature) {
      if (!this.hytalorInstalled) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("[HyTame] Feature '%s' requires Hytalor to be installed.", feature);
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("[HyTame] Without Hytalor, asset patches in Server/Patch/ are not applied.");
      }

   }

   private boolean detectSimpleClaims() {
      try {
         Class.forName("com.buuz135.simpleclaims.Main");
         return true;
      } catch (ClassNotFoundException var2) {
         return false;
      }
   }

   public boolean isSimpleClaimsInstalled() {
      return this.simpleClaimsInstalled;
   }

   public SimpleClaimsIntegration getSimpleClaimsIntegration() {
      return this.simpleClaimsIntegration;
   }

   private void registerNpcBuilders() {
      try {
         NPCPlugin.get().registerCoreComponentType("Tamed", BuilderSensorTamed::new);
         NPCPlugin.get().registerCoreComponentType("IsTameable", BuilderSensorIsTameable::new);
         NPCPlugin.get().registerCoreComponentType("HyTameFeedInteraction", BuilderActionHyTameFeedInteraction::new);
         NPCPlugin.get().registerCoreComponentType("RemovePlayerHeldItems", BuilderActionRemovePlayerHeldItems::new);
         NPCPlugin.get().registerCoreComponentType("GrowthReady", BuilderSensorGrowthReady::new);
         NPCPlugin.get().registerCoreComponentType("ScaledBaby", BuilderSensorScaledBaby::new);
         NPCPlugin.get().registerCoreComponentType("GrowToNextStage", BuilderActionGrowToNextStage::new);
         NPCPlugin.get().registerCoreComponentType("NeedsBreedCooldown", BuilderSensorNeedsBreedCooldown::new);
         NPCPlugin.get().registerCoreComponentType("ClearBreedCooldownFlag", BuilderActionClearBreedCooldownFlag::new);
         this.logVerbose("NPC builders registered (before asset loading)");
      } catch (Exception e) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("Failed to register NPC builders early: %s", e.getMessage());
      }

   }

   private void forceEarlyHytalorPatches() {
      Class<?> patchManagerClass;
      try {
         patchManagerClass = Class.forName("com.hypersonicsharkz.PatchManager");
      } catch (ClassNotFoundException var6) {
         return;
      }

      try {
         Object patchManager = patchManagerClass.getMethod("get").invoke((Object)null);
         Method loadPatchAssets = patchManagerClass.getMethod("loadPatchAssets", AssetPack.class);

         for(AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (!pack.getName().contains("Config_HyTame")) {
               loadPatchAssets.invoke(patchManager, pack);
            }
         }

         Class<?> hytalorClass = Class.forName("com.hypersonicsharkz.HytalorPlugin");
         Object hytalor = hytalorClass.getMethod("get").invoke((Object)null);
         hytalorClass.getMethod("initializePatches").invoke(hytalor);
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("[HyTame] Forced early Hytalor patch cycle (before NPC loading)");
      } catch (Exception e) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("[HyTame] Could not force early Hytalor patches: %s", e.getMessage());
      }

   }

   public HyTamePlugin(JavaPluginInit init) {
      super(init);
      instance = this;
   }

   protected void setup() {
      ((HytaleLogger.Api)this.getLogger().atInfo()).log("=== Lait's Animal Breeding v%s ===", "1.6.0");
      ((HytaleLogger.Api)this.getLogger().atInfo()).log("Build variant: %s", "default");
      ((HytaleLogger.Api)this.getLogger().atInfo()).log("Feeding mode: %s", "Asset-based patches (F key)");
      CoopCodecExtender.extend();
      this.configManager = new ConfigManager();
      this.configDirectory = this.resolveConfigDirectory();
      this.configManager.loadFromFile(this.configDirectory.resolve("config.json"));
      this.patchSyncService = new PatchSyncService();
      this.getEventRegistry().register((short)-20, LoadAssetEvent.class, (event) -> this.registerNpcBuilders());
      this.getEventRegistry().register((short)-15, LoadAssetEvent.class, (event) -> this.patchSyncService.initialize(this.configManager));
      this.getEventRegistry().register((short)129, LoadAssetEvent.class, (event) -> this.patchSyncService.ensurePackRegistered());
      this.getEventRegistry().register(AllNPCsLoadedEvent.class, (event) -> CompletableFuture.runAsync(() -> {
            try {
               Thread.sleep(500L);
               this.refreshMemories();
            } catch (Exception e) {
               ((HytaleLogger.Api)LOGGER.atWarning()).log("[HyTame] Failed to refresh memories: %s", e.getMessage());
            }

         }));
      this.breedingManager = new BreedingManager(this.configManager);
      this.growthManager = new GrowthManager(this.configManager, this.breedingManager);
      this.tamingManager = new TamingManager();
      this.persistenceManager = new PersistenceManager();
      this.persistenceManager.initialize(this.configDirectory);
      this.tamingManager.setPersistenceManager(this.persistenceManager);
      List<TamedAnimalData> savedAnimals = this.persistenceManager.loadData();
      this.tamingManager.loadFromPersistence(savedAnimals);
      this.breedingTickManager = new BreedingTickManager(this.breedingManager, this.configManager);
      this.breedingTickManager.setVerboseLogging(verboseLogging);
      this.effectsManager = new EffectsManager();
      this.spawningManager = new SpawningManager();
      this.spawningManager.setBreedingManager(this.breedingManager);
      this.spawningManager.setTamingManager(this.tamingManager);
      this.spawningManager.setHyTameTypeSupplier(() -> this.hyTameComponentType);
      this.spawningManager.setModelAssetIdGetter((args) -> this.getEntityModelAssetId((Store)args[0], (Ref)args[1]));
      this.breedingManager.setOnCustomBirthCallback((event) -> {
         try {
            String modelId = event.getModelAssetId();
            CustomAnimalConfig customConfig = this.configManager.getCustomAnimal(modelId);
            if (customConfig == null) {
               ((HytaleLogger.Api)this.getLogger().atWarning()).log("[CustomBreed] No config for model: %s", modelId);
               return;
            }

            Vector3d pos1 = EntityUtil.getPositionFromRef(event.getParent1EntityRef());
            Vector3d pos2 = EntityUtil.getPositionFromRef(event.getParent2EntityRef());
            if (pos1 == null && pos2 == null) {
               ((HytaleLogger.Api)this.getLogger().atWarning()).log("[CustomBreed] Could not get any parent position");
               return;
            }

            Vector3d spawnPos;
            if (pos1 != null && pos2 != null) {
               spawnPos = new Vector3d((pos1.x() + pos2.x()) / (double)2.0F, (pos1.y() + pos2.y()) / (double)2.0F, (pos1.z() + pos2.z()) / (double)2.0F);
            } else {
               spawnPos = pos1 != null ? pos1 : pos2;
            }

            String worldName = null;
            BreedingManager.CustomAnimalLoveData loveData = this.breedingManager.getCustomAnimalLoveData(event.getParent1Id());
            if (loveData != null) {
               worldName = loveData.getWorldName();
            }

            this.spawningManager.spawnCustomAnimalBaby(modelId, customConfig, spawnPos, worldName, event.getParent1Id(), event.getParent2Id());
            if (verboseLogging) {
               ((HytaleLogger.Api)this.getLogger().atInfo()).log("[CustomBreed] Spawning baby %s at midpoint", modelId);
            }
         } catch (Exception e) {
            ((HytaleLogger.Api)this.getLogger().atWarning()).log("[CustomBreed] Error in birth callback: %s", e.getMessage());
         }

      });
      this.interactionSetupManager = new InteractionSetupManager(this.configManager, this.breedingManager);
      this.interactionSetupManager.setHyTameInteractionTypeSupplier(() -> this.hyTameInteractionComponentType);
      this.interactionSetupManager.setUseEntityBasedInteractions(false);
      this.interactionSetupManager.setUseLegacyFeedInteraction(false);
      this.interactionSetupManager.setShowAbility2HintsOnEntities(true);
      this.mouseInteractionHandler = new MouseInteractionHandler(this.configManager, this.breedingManager, this.effectsManager, this.interactionSetupManager);
      this.mouseInteractionHandler.setTamingManager(this.tamingManager);
      this.breedingTickManager.setOnBreedingComplete((type, animals) -> {
         Vector3d pos1 = this.spawningManager.getPositionFromBreedingData(animals[0]);
         Vector3d pos2 = this.spawningManager.getPositionFromBreedingData(animals[1]);
         if (pos1 != null && pos2 != null) {
            Vector3d midpoint = new Vector3d((pos1.x() + pos2.x()) / (double)2.0F, (pos1.y() + pos2.y()) / (double)2.0F, (pos1.z() + pos2.z()) / (double)2.0F);
            String worldName = animals[0].getWorldName();
            if (worldName == null) {
               worldName = animals[1].getWorldName();
            }

            this.spawningManager.spawnBabyAnimal(type, midpoint, animals[0].getAnimalId(), animals[1].getAnimalId(), worldName);
         }

      });
      this.breedingTickManager.setOnCustomBreedingComplete((modelAssetId, animals) -> {
         Vector3d pos1 = EntityUtil.getPositionFromRef(animals[0].getEntityRef());
         Vector3d pos2 = EntityUtil.getPositionFromRef(animals[1].getEntityRef());
         if (pos1 != null && pos2 != null) {
            Vector3d midpoint = new Vector3d((pos1.x() + pos2.x()) / (double)2.0F, (pos1.y() + pos2.y()) / (double)2.0F, (pos1.z() + pos2.z()) / (double)2.0F);
            String worldName = animals[0].getWorldName();
            if (worldName == null) {
               worldName = animals[1].getWorldName();
            }

            CustomAnimalConfig customConfig = this.configManager.getCustomAnimal(modelAssetId);
            this.spawningManager.spawnCustomAnimalBaby(modelAssetId, customConfig, midpoint, worldName, animals[0].getAnimalId(), animals[1].getAnimalId());
         }

      });
      this.breedingTickManager.setHeartParticleSpawner((entityRef) -> {
         World targetWorld = null;
         if (entityRef instanceof Ref<EntityStore> ref) {
            Store<EntityStore> entityStore = ref.getStore();
            if (entityStore != null) {
               for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
                  World w = (World)entry.getValue();
                  if (w != null) {
                     try {
                        if (w.getEntityStore().getStore() == entityStore) {
                           targetWorld = w;
                           break;
                        }
                     } catch (Exception var9) {
                     }
                  }
               }
            }
         }

         if (targetWorld == null) {
            targetWorld = Universe.get().getDefaultWorld();
         }

         if (targetWorld != null) {
            targetWorld.execute(() -> {
               try {
                  Store<EntityStore> store = targetWorld.getEntityStore().getStore();
                  this.effectsManager.spawnHeartParticlesAtRef(store, entityRef);
               } catch (Exception var4) {
               }

            });
         }

      });
      this.breedingTickManager.setHeartParticlePositionSpawner((position, store) -> {
         try {
            Vector3d heartsPos = new Vector3d(position.x(), position.y() + (double)1.5F, position.z());
            ParticleUtil.spawnParticleEffect("BreedingHearts", heartsPos, store);
         } catch (Exception var3) {
         }

      });
      this.registerInteractionHandler();

      try {
         this.getCodecRegistry(Interaction.CODEC).register("FeedAnimal", FeedAnimalInteraction.class, FeedAnimalInteraction.CODEC);
      } catch (Exception e) {
         this.logWarning("FeedAnimalInteraction codec registration skipped (may already exist): " + e.getMessage());
      }

      try {
         this.getCodecRegistry(Interaction.CODEC).register("NameAnimal", NameAnimalInteraction.class, NameAnimalInteraction.CODEC);
      } catch (Exception e) {
         this.logWarning("NameAnimalInteraction codec registration skipped (may already exist): " + e.getMessage());
      }

      try {
         this.getCodecRegistry(Interaction.CODEC).register("UseCaptureCrate", HyTameCaptureInteraction.class, HyTameCaptureInteraction.CODEC);
         this.logVerbose("UseCaptureCrate overridden with HyTameCaptureInteraction");
      } catch (Exception e) {
         this.logWarning("Could not override UseCaptureCrate (" + e.getMessage() + "), registering as HyTameCapture instead");

         try {
            this.getCodecRegistry(Interaction.CODEC).register("HyTameCapture", HyTameCaptureInteraction.class, HyTameCaptureInteraction.CODEC);
         } catch (Exception e2) {
            this.logWarning("HyTameCaptureInteraction codec registration failed: " + e2.getMessage());
         }
      }

      try {
         this.getCodecRegistry(Interaction.CODEC).register("OpenConfigPanel", OpenConfigPanelInteraction.class, OpenConfigPanelInteraction.CODEC);
         this.logVerbose("OpenConfigPanelInteraction registered");
      } catch (Exception e) {
         this.logWarning("OpenConfigPanelInteraction codec registration skipped (may already exist): " + e.getMessage());
      }

      try {
         this.hyTameComponentType = this.getEntityStoreRegistry().registerComponent(HyTameComponent.class, "HyTame", HyTameComponent.CODEC);
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("HyTameComponent registered successfully");
      } catch (Exception e) {
         this.logWarning("HyTameComponent registration failed: " + e.getMessage());
      }

      try {
         this.hyTameInteractionComponentType = this.getEntityStoreRegistry().registerComponent(HyTameInteractionComponent.class, "OriginalInteraction", HyTameInteractionComponent.CODEC);
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("HyTameInteractionComponent registered successfully");
      } catch (Exception e) {
         this.logWarning("HyTameInteractionComponent registration failed: " + e.getMessage());
      }

      try {
         this.getEntityStoreRegistry().registerSystem(new UseBlockHandler());
      } catch (Exception var6) {
      }

      if (this.configManager.isPersistenceEnabled()) {
         try {
            this.getEntityStoreRegistry().registerSystem(new DetectTamedDeath());
            this.logVerbose("DetectTamedDeath system registered");
         } catch (Exception e) {
            this.logWarning("Failed to register DetectTamedDeath: " + e.getMessage());
         }
      }

      try {
         this.getEntityStoreRegistry().registerSystem(new CoopResidentTracker());
         this.logVerbose("CoopResidentTracker system registered");
      } catch (Exception e) {
         this.logWarning("Failed to register CoopResidentTracker: " + e.getMessage());
      }

      this.getCommandRegistry().registerCommand(new HytameCommand());
      this.getCommandRegistry().registerCommand(new BreedCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingHelpCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingStatusCommand());
      this.getCommandRegistry().registerCommand(new BreedingConfigCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingGrowthCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.NameTagCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.TamingInfoCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.TamingSettingsCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.UntameCommand());
      this.getCommandRegistry().registerCommand(new CustomAnimalCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingLogsCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingDevCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingHintCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingScanCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.BreedingCachesCommand());
      this.getCommandRegistry().registerCommand(new LegacyCommands.NoClipCommand());
   }

   protected void start() {
      this.hytalorInstalled = this.detectHytalor();
      if (this.hytalorInstalled) {
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("Hytalor detected - asset patching enabled");
      } else {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("[Warning] Hytalor not detected. Some features like taming hints and asset-based roles require Hytalor to be installed.");
      }

      this.simpleClaimsInstalled = this.detectSimpleClaims();
      if (this.simpleClaimsInstalled) {
         this.simpleClaimsIntegration = new SimpleClaimsIntegration();
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("SimpleClaims detected - per-claim tame limits available");
      }

      RootInteraction rootInt = RootInteraction.getRootInteractionOrUnknown("Root_FeedAnimal");
      String[] ids = rootInt.getInteractionIds();
      if (ids == null || ids.length == 0) {
         String[] newIds = new String[]{"FeedAnimal"};
         rootInt.build(Set.of(newIds));
      }

      this.tickScheduler = Executors.newSingleThreadScheduledExecutor();
      this.scheduledTasks.add(this.tickScheduler.scheduleAtFixedRate(() -> {
         try {
            this.breedingManager.tickPregnancies();
            this.breedingTickManager.tick();
            this.interactionSetupManager.updateTrackedAnimalStates();
         } catch (Exception e) {
            this.logWarning("[Tick] Error: " + e.getMessage());
            e.printStackTrace();
         }

      }, 1L, 1L, TimeUnit.SECONDS));
      if (this.configManager.isPersistenceEnabled() && this.persistenceManager != null && this.tamingManager != null) {
         this.persistenceManager.startAutoSave(this.tickScheduler, () -> this.tamingManager.getAllTamedAnimals(), 5L);
      }

      this.attachInteractionsToAnimals();
      if (this.configManager.isPersistenceEnabled()) {
         this.getEventRegistry().register(PlayerConnectEvent.class, (event) -> {
            if (!this.firstPlayerConnected && this.tamingManager != null) {
               this.firstPlayerConnected = true;
               this.tamingManager.markInitialized();
               this.logVerbose("First player connected - grace period started");
            }

         });
      }

      this.registerEntityRemovalListener();

      try {
         this.spawnDetector = new NewAnimalSpawnDetector();
         this.getEntityStoreRegistry().registerSystem(this.spawnDetector);
         this.logVerbose("NewAnimalSpawnDetector registered (RefSystem pattern)");
         this.getEntityStoreRegistry().registerSystem(new HyTameSystems.HyTameActivateSystem());
         this.logVerbose("HyTameActivateSystem registered");
         this.getEntityStoreRegistry().registerSystem(new HyTameSystems.HyTameTickSystem());
         this.logVerbose("HyTameTickSystem registered");
         this.scheduledTasks.add(this.tickScheduler.scheduleAtFixedRate(() -> {
            try {
               if (this.spawnDetector == null) {
                  return;
               }

               if (Universe.get() == null) {
                  return;
               }

               Map<String, World> worlds = Universe.get().getWorlds();
               if (worlds == null) {
                  return;
               }

               Set<UUID> currentPlayerUuids = ConcurrentHashMap.newKeySet();

               for(Map.Entry<String, World> entry : worlds.entrySet()) {
                  World world = (World)entry.getValue();
                  if (world != null) {
                     for(Player p : world.getPlayers()) {
                        UUID pUuid = EntityUtil.getEntityUUID(p);
                        if (pUuid != null) {
                           currentPlayerUuids.add(pUuid);
                        }
                     }
                  }
               }

               this.spawnDetector.updatePlayerUuids(currentPlayerUuids);
            } catch (Exception var9) {
            }

         }, 2L, 5L, TimeUnit.SECONDS));
         this.scheduledTasks.add(this.tickScheduler.scheduleAtFixedRate(() -> {
            try {
               if (this.spawnDetector != null) {
                  int cacheSize = this.spawnDetector.getProcessedCacheSize();
                  this.spawnDetector.clearProcessedCache();
                  if (cacheSize > 0) {
                     this.logVerbose("Cleared spawn detector cache (" + cacheSize + " entries)");
                  }
               }
            } catch (Exception var2) {
            }

         }, 5L, 5L, TimeUnit.MINUTES));
         this.scheduledTasks.add(this.tickScheduler.scheduleAtFixedRate(() -> {
            try {
               int removed = InteractionStateCache.getInstance().cleanupStaleEntries();
               if (removed > 0) {
                  this.logVerbose("Cleaned " + removed + " stale interaction entries");
               }
            } catch (Exception var2) {
            }

         }, 10L, 10L, TimeUnit.MINUTES));
         this.scheduledTasks.add(this.tickScheduler.scheduleAtFixedRate(() -> {
            try {
               int removed = this.breedingManager.cleanupStaleEntries();
               if (removed > 0) {
                  this.logVerbose("Cleaned " + removed + " stale breeding entries");
               }
            } catch (Exception var2) {
            }

         }, 5L, 5L, TimeUnit.MINUTES));
         this.scheduledTasks.add(this.tickScheduler.scheduleAtFixedRate(() -> {
            try {
               this.scanForUntrackedBabies();
            } catch (Exception var2) {
            }

         }, 30L, 30L, TimeUnit.SECONDS));
      } catch (Exception e) {
         this.logWarning("ECS system registration failed: " + e.getMessage());
         this.spawnDetector = null;
      }

      ((HytaleLogger.Api)this.getLogger().atInfo()).log("[HyTame] Plugin started! Command: /hytame");
      HyTameSelfTest.scheduleIfEnabled(this);
   }

   public void attachInteractionsToAnimals() {
      this.logVerbose("Legacy FeedAnimalInteraction disabled - skipping interaction attachment");
   }

   public void onNewAnimalDetected(Store<EntityStore> store, Ref<EntityStore> entityRef, String modelAssetId, AnimalType animalType, World world) {
      this.interactionSetupManager.onNewAnimalDetected(store, entityRef, modelAssetId, animalType, world);
      if (world != null && this.tamingManager != null && this.hyTameComponentType != null) {
         world.execute(() -> {
            try {
               if (!entityRef.isValid()) {
                  return;
               }

               Store<EntityStore> s = entityRef.getStore();
               if (s == null) {
                  return;
               }

               HyTameComponent hyTame = (HyTameComponent)s.getComponent(entityRef, this.hyTameComponentType);
               if (hyTame == null || !hyTame.isTamed() || hyTame.getHytameId() == null) {
                  return;
               }

               UUID entityUuid = EcsReflectionUtil.getUuidFromRef(entityRef);
               if (entityUuid == null) {
                  return;
               }

               if (this.tamingManager.isTamed(entityUuid)) {
                  TamedAnimalData data = this.tamingManager.getTamedData(entityUuid);
                  if (data != null && data.getEntityRef() == null) {
                     data.setEntityRef(entityRef);
                     data.setDespawned(false);
                  }

                  return;
               }

               TransformComponent transform = (TransformComponent)s.getComponent(entityRef, EcsReflectionUtil.TRANSFORM_TYPE);
               double x = (double)0.0F;
               double y = (double)0.0F;
               double z = (double)0.0F;
               if (transform != null && transform.getPosition() != null) {
                  x = transform.getPosition().x();
                  y = transform.getPosition().y();
                  z = transform.getPosition().z();
               }

               this.tamingManager.syncEntity(entityUuid, hyTame.getHytameId(), true, hyTame.getTamerUUID(), hyTame.getTamerName(), entityRef, x, y, z);
            } catch (Exception var12) {
            }

         });
      }

   }

   private String getEntityModelAssetId(Store<EntityStore> store, Ref<EntityStore> entityRef) {
      return EcsReflectionUtil.getEntityModelAssetId(store, entityRef);
   }

   private void registerEntityRemovalListener() {
      try {
         this.getEventRegistry().registerGlobal(EntityRemoveEvent.class, (event) -> {
            try {
               Entity entity = event.getEntity();
               if (entity == null) {
                  return;
               }

               UUID entityId = EntityUtil.getEntityUUID(entity);

               try {
                  Ref<EntityStore> debugRef = entity.getReference();
                  Integer debugRefIndex = debugRef != null ? debugRef.getIndex() : null;
                  boolean debugIsTamed = this.tamingManager != null && this.tamingManager.isTamed(entityId);
                  this.logVerbose("EntityRemoveEvent: entity removed - refIndex=" + debugRefIndex + ", uuid=" + String.valueOf(entityId) + ", isTamed=" + debugIsTamed);
               } catch (Exception var8) {
               }

               if (this.tamingManager != null && this.tamingManager.isTamed(entityId)) {
                  TamedAnimalData tamedData = this.tamingManager.getTamedData(entityId);
                  if (tamedData != null && tamedData.isCaptured()) {
                     this.logVerbose("Tamed animal captured in crate (skipping cleanup): " + String.valueOf(entityId));
                     return;
                  }

                  trackDetectedDespawn(entityId);
                  this.logVerbose("Tamed animal removed (entity event): " + String.valueOf(entityId));
                  return;
               }

               BreedingData data = this.breedingManager.getData(entityId);
               if (data != null) {
                  this.breedingManager.removeData(entityId);
               }

               try {
                  Ref<EntityStore> ref = entity.getReference();
                  if (ref != null) {
                     InteractionStateCache.getInstance().remove(ref);
                  }
               } catch (Exception var7) {
               }
            } catch (Exception var9) {
            }

         });
      } catch (Exception var2) {
      }

   }

   public void autoSetupNearbyAnimals() {
      if (verboseLogging) {
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("[AutoScan] autoSetupNearbyAnimals CALLED");
      }

      try {
         Set<UUID> playerUuids = new HashSet();

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               for(Player p : world.getPlayers()) {
                  UUID pUuid = EntityUtil.getEntityUUID(p);
                  if (pUuid != null) {
                     playerUuids.add(pUuid);
                  }
               }
            }
         }

         if (verboseLogging) {
            ((HytaleLogger.Api)this.getLogger().atInfo()).log("[AutoScan] Starting animal scan in all worlds (customAnimals registered: %d)", this.configManager.getCustomAnimals().size());
         }

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            String worldName = (String)entry.getKey();
            World world = (World)entry.getValue();
            if (world != null) {
               if (verboseLogging) {
                  ((HytaleLogger.Api)this.getLogger().atInfo()).log("[AutoScan] Scanning world: %s", worldName);
               }

               AnimalFinder.findAnimals(world, false, (animals) -> {
                  try {
                     if (verboseLogging) {
                        ((HytaleLogger.Api)this.getLogger().atInfo()).log("[AutoScan] Found %d animals in world %s", animals.size(), worldName);
                     }

                     if (animals.isEmpty()) {
                        return;
                     }

                     if (verboseLogging && worldName.equals(Universe.get().getWorlds().keySet().iterator().next())) {
                        ((HytaleLogger.Api)this.getLogger().atInfo()).log("[AutoScan] Registered custom animals: %s", String.join(", ", this.configManager.getCustomAnimals().keySet()));
                     }

                     for(AnimalFinder.FoundAnimal animal : animals) {
                        this.scanAnimal(animal, playerUuids);
                     }
                  } catch (Exception e) {
                     this.logWarning("autoSetupNearbyAnimals callback error in " + worldName + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                     if (verboseLogging && e.getCause() != null) {
                        this.logWarning("  Caused by: " + e.getCause().getMessage());
                     }
                  }

               });
            }
         }
      } catch (Exception e) {
         String var10001 = e.getClass().getSimpleName();
         this.logWarning("autoSetupNearbyAnimals setup error: " + var10001 + ": " + e.getMessage());
      }

   }

   private void scanAnimal(AnimalFinder.FoundAnimal animal, Set<UUID> playerUuids) {
      Ref<EntityStore> entityRef = animal.getEntityRef();
      AnimalType animalType = animal.getAnimalType();
      String modelId = animal.getModelAssetId();
      if (this.configManager.isCustomAnimal(modelId) && verboseLogging) {
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("[AutoScan] Processing potential custom animal: '%s' (animalType=%s)", modelId, animalType);
      }

      if (entityRef instanceof Ref) {
         Ref<EntityStore> ref = entityRef;
         if (!entityRef.isValid()) {
            this.logVerbose("[AnimalScan] Skipping stale entity ref for " + animal.getModelAssetId());
            return;
         }

         try {
            Store<EntityStore> refStore = ref.getStore();
            if (refStore != null) {
               UUIDComponent uuidComp = (UUIDComponent)refStore.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
               if (uuidComp != null && uuidComp.getUuid() != null && playerUuids.contains(uuidComp.getUuid())) {
                  this.logVerbose("Skipping player entity with animal model: " + animal.getModelAssetId());
                  return;
               }
            }
         } catch (Exception e) {
            if (this.configManager.isCustomAnimal(modelId)) {
               this.logVerbose("[CustomAnimal] " + modelId + " has no UUID component (expected for custom NPCs), proceeding");
            } else {
               String var10001 = animal.getModelAssetId();
               this.logVerbose("[AnimalScan] UUID check failed for " + var10001 + " (proceeding anyway): " + e.getMessage());
            }
         }
      }

      CustomAnimalConfig customAnimal = null;
      if (animalType == null) {
         customAnimal = this.configManager.getCustomAnimal(modelId);
         if (customAnimal != null) {
            if (verboseLogging) {
               ((HytaleLogger.Api)this.getLogger().atInfo()).log("[CustomAnimal] Found match for '%s' (enabled=%s)", modelId, customAnimal.isEnabled());
            }
         } else if (this.configManager.getCustomAnimals().size() > 0 && verboseLogging) {
            ((HytaleLogger.Api)this.getLogger().atInfo()).log("[CustomAnimal] No match for '%s' (registered: %s)", modelId, String.join(", ", this.configManager.getCustomAnimals().keySet()));
         }
      } else if (this.configManager.isCustomAnimal(modelId)) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("[CustomAnimal] '%s' matched as built-in %s instead of custom!", modelId, animalType);
      }

      if (animalType != null || customAnimal != null) {
         if (animalType != null && !this.configManager.isBreedingEnabled(animalType) && !this.configManager.isTamingEnabled(animalType)) {
            this.logVerbose("Skipping disabled animal: " + String.valueOf(animalType));
         } else if (customAnimal != null && !customAnimal.isBreedingEnabled() && !customAnimal.isTamingEnabled()) {
            this.logVerbose("Skipping disabled custom animal: " + animal.getModelAssetId());
         } else {
            if (animal.isBaby() && animalType != null) {
               UUID babyId = UUID.nameUUIDFromBytes(entityRef.toString().getBytes());
               if (this.breedingManager.getData(babyId) == null) {
                  this.breedingManager.registerBaby(babyId, animalType, entityRef);
               }
            }

            if (!animal.isBaby()) {
            }

            if (customAnimal != null && verboseLogging) {
               ((HytaleLogger.Api)this.getLogger().atInfo()).log("[CustomAnimal] Skipping baby custom animal: %s", animal.getModelAssetId());
            }

         }
      }
   }

   private void registerInteractionHandler() {
      try {
         EventRegistry var10000 = this.getEventRegistry();
         MouseInteractionHandler var10002 = this.mouseInteractionHandler;
         Objects.requireNonNull(var10002);
         var10000.register(PlayerMouseButtonEvent.class, var10002::onMouseButton);
      } catch (Exception var3) {
      }

      try {
         EventRegistry var4 = this.getEventRegistry();
         MouseInteractionHandler var5 = this.mouseInteractionHandler;
         Objects.requireNonNull(var5);
         var4.registerGlobal(PlayerInteractEvent.class, var5::onPlayerInteract);
      } catch (Exception var2) {
      }

   }

   public int scanForUntrackedBabies() {
      int registered = 0;

      try {
         List<BreedingManager.UntrackedBaby> untrackedBabies = new ArrayList();

         for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
            World world = (World)entry.getValue();
            if (world != null) {
               Store<EntityStore> store = world.getEntityStore().getStore();
               if (store != null) {
                  world.execute(() -> store.forEachChunk((chunk, buffer) -> {
                        int size = chunk.size();

                        for(int i = 0; i < size; ++i) {
                           try {
                              Ref<EntityStore> ref = chunk.getReferenceTo(i);
                              String modelAssetId = this.getEntityModelAssetId(store, ref);
                              if (modelAssetId != null && AnimalType.isBabyVariant(modelAssetId)) {
                                 AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
                                 if (animalType != null) {
                                    UUID refUuid = UUID.nameUUIDFromBytes(ref.toString().getBytes());
                                    if (!this.breedingManager.isBabyTracked(ref, refUuid)) {
                                       synchronized(untrackedBabies) {
                                          untrackedBabies.add(new BreedingManager.UntrackedBaby(ref, modelAssetId, animalType));
                                       }
                                    }
                                 }
                              }
                           } catch (Exception var14) {
                           }
                        }

                     }));
               }
            }
         }

         for(BreedingManager.UntrackedBaby baby : untrackedBabies) {
            UUID babyId = UUID.nameUUIDFromBytes(baby.getEntityRef().toString().getBytes());
            this.breedingManager.registerBaby(babyId, baby.getAnimalType(), baby.getEntityRef());
            ++registered;
            this.logVerbose("[BabyScan] Registered untracked baby: " + baby.getModelAssetId());
         }

         if ((registered > 0 || verboseLogging) && registered > 0) {
            ((HytaleLogger.Api)this.getLogger().atInfo()).log("[BabyScan] Found %d untracked babies across all worlds, registered all", registered);
         }
      } catch (Exception e) {
         this.logVerbose("[BabyScan] Error: " + e.getMessage());
      }

      return registered;
   }

   private Path resolveConfigDirectory() {
      Path parent = this.getDataDirectory().getParent();
      Path configDir = parent.resolve("Config_HyTame");
      Path oldDir = parent.resolve("Lait_AnimalBreeding");
      if (!Files.exists(configDir, new LinkOption[0]) && Files.exists(oldDir, new LinkOption[0])) {
         try {
            Files.createDirectories(configDir);
            Stream<Path> stream = Files.walk(oldDir);

            try {
               stream.forEach((source) -> {
                  Path target = configDir.resolve(oldDir.relativize(source));

                  try {
                     if (Files.isDirectory(source, new LinkOption[0])) {
                        Files.createDirectories(target);
                     } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                     }
                  } catch (IOException e) {
                     HytaleLogger.Api var10000 = (HytaleLogger.Api)this.getLogger().atWarning();
                     String var10001 = String.valueOf(source);
                     var10000.log("Failed to migrate file: " + var10001 + " -> " + e.getMessage());
                  }

               });
            } catch (Throwable var11) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var9) {
                     var11.addSuppressed(var9);
                  }
               }

               throw var11;
            }

            if (stream != null) {
               stream.close();
            }

            ((HytaleLogger.Api)this.getLogger().atInfo()).log("Migrated config from Lait_AnimalBreeding to Config_HyTame");
            stream = Files.walk(oldDir);

            try {
               stream.sorted(Comparator.reverseOrder()).forEach((path) -> {
                  try {
                     Files.delete(path);
                  } catch (IOException var2) {
                  }

               });
            } catch (Throwable var10) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var8) {
                     var10.addSuppressed(var8);
                  }
               }

               throw var10;
            }

            if (stream != null) {
               stream.close();
            }

            ((HytaleLogger.Api)this.getLogger().atInfo()).log("Deleted old Lait_AnimalBreeding directory");
         } catch (IOException e) {
            ((HytaleLogger.Api)this.getLogger().atWarning()).log("Config migration failed: " + e.getMessage());
         }
      }

      try {
         Files.createDirectories(configDir);
      } catch (IOException e) {
         ((HytaleLogger.Api)this.getLogger().atWarning()).log("Failed to create config directory: " + e.getMessage());
      }

      return configDir;
   }

   protected void shutdown() {
      ((HytaleLogger.Api)this.getLogger().atInfo()).log("[HyTame] Plugin shutdown");
      if (this.tickScheduler != null) {
         for(ScheduledFuture<?> task : this.scheduledTasks) {
            task.cancel(false);
         }

         this.scheduledTasks.clear();
         this.tickScheduler.shutdown();

         try {
            if (!this.tickScheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
               this.tickScheduler.shutdownNow();
            }
         } catch (InterruptedException var3) {
            this.tickScheduler.shutdownNow();
         }
      }

      if (this.configManager != null && this.configManager.isPersistenceEnabled() && this.persistenceManager != null && this.tamingManager != null) {
         ((HytaleLogger.Api)this.getLogger().atInfo()).log("[Taming] Saving tamed animals on shutdown...");
         this.persistenceManager.stopAutoSave();
         this.persistenceManager.forceSaveSync(this.tamingManager.getAllTamedAnimals());
      }

      if (this.breedingManager != null) {
         this.breedingManager.clearAll();
      }

      instance = null;
   }

   public static HyTamePlugin getInstance() {
      return instance;
   }

   public Path getConfigDirectory() {
      return this.configDirectory;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public BreedingManager getBreedingManager() {
      return this.breedingManager;
   }

   public TamingManager getTamingManager() {
      return this.tamingManager;
   }

   public PersistenceManager getPersistenceManager() {
      return this.persistenceManager;
   }

   public GrowthManager getGrowthManager() {
      return this.growthManager;
   }

   public SpawningManager getSpawningManager() {
      return this.spawningManager;
   }

   public ComponentType<EntityStore, HyTameComponent> getHyTameComponentType() {
      return this.hyTameComponentType;
   }

   public ComponentType<EntityStore, HyTameInteractionComponent> getHyTameInteractionComponentType() {
      return this.hyTameInteractionComponentType;
   }

   public PatchSyncService getPatchSyncService() {
      return this.patchSyncService;
   }

   static {
      try {
         ATTITUDE_FIELD = WorldSupport.class.getDeclaredField("defaultPlayerAttitude");
         ATTITUDE_FIELD.setAccessible(true);
      } catch (NoSuchFieldException e) {
         throw new RuntimeException("Failed to access defaultPlayerAttitude", e);
      }

      lastDetectedDespawns = Collections.synchronizedList(new ArrayList());
      verboseLogging = false;
      devMode = false;
   }
}
