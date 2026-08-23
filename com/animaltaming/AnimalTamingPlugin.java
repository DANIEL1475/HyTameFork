package com.animaltaming;

import com.animaltaming.api.TamingService;
import com.animaltaming.api.event.TamingEvents;
import com.animaltaming.api.model.TamedAnimal;
import com.animaltaming.api.model.TamingConfig;
import com.animaltaming.config.ConfigLoader;
import com.animaltaming.core.handler.BehaviorHandler;
import com.animaltaming.core.handler.CalmingHandler;
import com.animaltaming.core.handler.FeedingHandler;
import com.animaltaming.core.handler.MountingHandler;
import com.animaltaming.core.registry.TamedAnimalRegistry;
import com.animaltaming.core.registry.TamingConfigRegistry;
import com.animaltaming.core.service.CachedPlayerLookupService;
import com.animaltaming.core.service.DefaultTamingService;
import com.animaltaming.core.service.PlayerLookupService;
import com.animaltaming.persistence.JsonTamingRepository;
import com.animaltaming.persistence.TamingRepository;
import com.animaltaming.persistence.codec.TamedAnimalCodec;
import com.animaltaming.system.SystemContext;
import com.animaltaming.system.TamingTickSystem;
import com.animaltaming.util.EventBus;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AnimalTamingPlugin {
   private final Path pluginFolder;
   private final EventBus eventBus;
   private final TamingConfigRegistry configRegistry;
   private final TamedAnimalRegistry animalRegistry;
   private final PlayerLookupService playerLookup;
   private final TamingRepository repository;
   private final CalmingHandler calmingHandler;
   private final FeedingHandler feedingHandler;
   private final MountingHandler mountingHandler;
   private final BehaviorHandler behaviorHandler;
   private final DefaultTamingService tamingService;
   private final TamingTickSystem tickSystem;
   private boolean enabled = false;

   public AnimalTamingPlugin(Path pluginFolder) {
      this.pluginFolder = (Path)Objects.requireNonNull(pluginFolder, "pluginFolder required");
      this.eventBus = new EventBus();
      this.configRegistry = new TamingConfigRegistry();
      this.animalRegistry = new TamedAnimalRegistry();
      this.playerLookup = new CachedPlayerLookupService();
      this.repository = new JsonTamingRepository(pluginFolder, new TamedAnimalCodec());
      this.calmingHandler = new CalmingHandler(this.playerLookup, this.configRegistry, this.eventBus);
      this.feedingHandler = new FeedingHandler(this.playerLookup, this.configRegistry, this.eventBus, this.calmingHandler);
      this.mountingHandler = new MountingHandler(this.playerLookup, this.configRegistry, this.eventBus, this.calmingHandler);
      this.behaviorHandler = new BehaviorHandler(this.playerLookup, this.animalRegistry, this.eventBus);
      this.tamingService = new DefaultTamingService(this.animalRegistry, this.configRegistry, this.playerLookup, this.eventBus, this.calmingHandler, this.feedingHandler, this.behaviorHandler);
      this.tickSystem = new TamingTickSystem(this.playerLookup, this.calmingHandler, this.feedingHandler, this.mountingHandler, this.behaviorHandler, this.tamingService);
   }

   public void onEnable() {
      System.out.println("[AnimalTaming] Enabling Animal Taming Plugin v1.0.0...");
      this.loadConfigurations();
      this.loadPersistedAnimals();
      this.subscribeToEvents();
      this.enabled = true;
      System.out.println("[AnimalTaming] Animal Taming Plugin enabled!");
      System.out.println("[AnimalTaming] Loaded " + this.configRegistry.size() + " tameable species.");
      System.out.println("[AnimalTaming] Loaded " + this.animalRegistry.size() + " tamed animals.");
   }

   public void onDisable() {
      System.out.println("[AnimalTaming] Disabling Animal Taming Plugin...");
      this.savePersistedAnimals();
      this.eventBus.clear();
      this.enabled = false;
      System.out.println("[AnimalTaming] Animal Taming Plugin disabled!");
   }

   public void onTick(SystemContext context, float deltaTime) {
      if (this.enabled) {
         this.tickSystem.update(context, deltaTime);
      }
   }

   private void loadConfigurations() {
      ConfigLoader loader = new ConfigLoader();
      List<TamingConfig> resourceConfigs = loader.loadFromResources("tameable");
      this.configRegistry.registerAll(resourceConfigs);
      Path customConfigFolder = this.pluginFolder.resolve("tameable");

      for(TamingConfig config : loader.loadFromDirectory(customConfigFolder)) {
         if (!this.configRegistry.contains(config.speciesId())) {
            this.configRegistry.register(config);
         }
      }

   }

   private void loadPersistedAnimals() {
      for(TamedAnimal animal : this.repository.loadAll()) {
         this.animalRegistry.register(animal, 0L);
      }

   }

   private void savePersistedAnimals() {
      for(TamedAnimal animal : this.animalRegistry.getAll()) {
         this.repository.save(animal);
      }

      System.out.println("[AnimalTaming] Saved " + this.animalRegistry.size() + " tamed animals.");
   }

   private void subscribeToEvents() {
      this.eventBus.subscribe(TamingEvents.AnimalTamedEvent.class, (event) -> {
         PrintStream var10000 = System.out;
         String var10001 = event.ownerName();
         var10000.println("[AnimalTaming] " + var10001 + " tamed a " + event.speciesId() + "!");
      });
      this.eventBus.subscribe(TamingEvents.TamedAnimalLostEvent.class, (event) -> {
         PrintStream var10000 = System.out;
         String var10001 = event.speciesId();
         var10000.println("[AnimalTaming] Tamed animal lost: " + var10001 + " (" + event.reason() + ")");
      });
      this.eventBus.subscribe(TamingEvents.AnimalTamedEvent.class, (event) -> {
         Optional var10000 = this.animalRegistry.getByAnimalId(event.animalId());
         TamingRepository var10001 = this.repository;
         Objects.requireNonNull(var10001);
         var10000.ifPresent(var10001::save);
      });
      this.eventBus.subscribe(TamingEvents.BehaviorModeChangedEvent.class, (event) -> {
         Optional var10000 = this.animalRegistry.getByAnimalId(event.animalId());
         TamingRepository var10001 = this.repository;
         Objects.requireNonNull(var10001);
         var10000.ifPresent(var10001::save);
      });
   }

   public TamingService getTamingService() {
      return this.tamingService;
   }

   public EventBus getEventBus() {
      return this.eventBus;
   }

   public TamingConfigRegistry getConfigRegistry() {
      return this.configRegistry;
   }

   public TamedAnimalRegistry getAnimalRegistry() {
      return this.animalRegistry;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public Path getPluginFolder() {
      return this.pluginFolder;
   }
}
