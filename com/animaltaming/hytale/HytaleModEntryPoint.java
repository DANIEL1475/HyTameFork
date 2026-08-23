package com.animaltaming.hytale;

import com.animaltaming.AnimalTamingPlugin;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.World;
import java.nio.file.Path;
import java.util.Objects;

public class HytaleModEntryPoint {
   private final Path pluginFolder;
   private final HytaleEntityAdapter entityAdapter;
   private final HytaleSystemContext systemContext;
   private final AnimalTamingPlugin plugin;
   private boolean initialized = false;
   private long lastTickTime = System.nanoTime();

   public HytaleModEntryPoint(Path pluginFolder) {
      this.pluginFolder = (Path)Objects.requireNonNull(pluginFolder, "pluginFolder required");
      this.entityAdapter = new HytaleEntityAdapter();
      this.systemContext = new HytaleSystemContext(this.entityAdapter);
      this.plugin = new AnimalTamingPlugin(pluginFolder);
   }

   public void initialize(World world) {
      Objects.requireNonNull(world, "world required");
      this.systemContext.setWorld(world);
      this.plugin.onEnable();
      this.initialized = true;
      System.out.println("[AnimalTaming] Mod initialized with world");
   }

   public void onTick() {
      if (this.initialized) {
         long currentTime = System.nanoTime();
         float deltaTime = (float)(currentTime - this.lastTickTime) / 1.0E9F;
         this.lastTickTime = currentTime;
         this.systemContext.tick();
         this.plugin.onTick(this.systemContext, deltaTime);
      }
   }

   public void onEntitySpawn(Entity entity) {
      if (entity != null) {
         long entityId = this.entityAdapter.registerEntity(entity);
         if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            String speciesId = this.getSpeciesId(entity);
            if (speciesId != null && this.plugin.getConfigRegistry().contains(speciesId)) {
               this.systemContext.registerTameableAnimal(entityId, speciesId);
            }
         }

      }
   }

   public void onEntityDespawn(Entity entity) {
      if (entity != null) {
         this.entityAdapter.getEntityId(entity).ifPresent((entityId) -> {
            this.systemContext.unregisterTameableAnimal(entityId);
            this.entityAdapter.unregisterEntity(entity);
         });
      }
   }

   public void onPlayerInteract(Entity player, Entity target, String interactionType) {
      if (player != null && target != null) {
         this.entityAdapter.getEntityId(player).ifPresent((playerId) -> this.entityAdapter.getEntityId(target).ifPresent((targetId) -> this.systemContext.queueInteraction(playerId, targetId, interactionType)));
      }
   }

   public void shutdown() {
      if (this.initialized) {
         this.plugin.onDisable();
         this.entityAdapter.clear();
         this.initialized = false;
         System.out.println("[AnimalTaming] Mod shutdown complete");
      }

   }

   private String getSpeciesId(Entity entity) {
      return entity == null ? null : EntityModule.get().getIdentifier(entity.getClass());
   }

   public AnimalTamingPlugin getPlugin() {
      return this.plugin;
   }

   public HytaleEntityAdapter getEntityAdapter() {
      return this.entityAdapter;
   }

   public HytaleSystemContext getSystemContext() {
      return this.systemContext;
   }

   public boolean isInitialized() {
      return this.initialized;
   }
}
