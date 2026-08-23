package com.animaltaming.hytale;

import com.animaltaming.AnimalTamingPlugin;
import com.animaltaming.api.model.TamingConfig;
import com.animaltaming.api.model.TamingProgress;
import com.animaltaming.api.model.TamingState;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public class HyTamePlugin extends JavaPlugin {
   private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
   private HytaleModEntryPoint entryPoint;
   private ScheduledFuture<?> tickTask;

   public HyTamePlugin(@Nonnull JavaPluginInit init) {
      super(init);
   }

   protected void setup() {
      ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Setting up event handlers...");
      this.getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdd);
      this.getEventRegistry().registerGlobal(EntityRemoveEvent.class, this::onEntityRemove);
      this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, this::onPlayerInteract);
      ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Event handlers registered");
   }

   protected void start() {
      ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Starting plugin...");
      this.entryPoint = new HytaleModEntryPoint(this.getDataDirectory());
      this.tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
         if (this.entryPoint != null && this.entryPoint.isInitialized()) {
            this.entryPoint.onTick();
         }

      }, 0L, 33L, TimeUnit.MILLISECONDS);
      ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Plugin started, waiting for world initialization...");
   }

   protected void shutdown() {
      ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Shutting down...");
      if (this.tickTask != null) {
         this.tickTask.cancel(false);
         this.tickTask = null;
      }

      if (this.entryPoint != null) {
         this.entryPoint.shutdown();
         this.entryPoint = null;
      }

      ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Shutdown complete");
   }

   private void onWorldAdd(AddWorldEvent event) {
      if (this.entryPoint != null && !this.entryPoint.isInitialized()) {
         this.entryPoint.initialize(event.getWorld());
         ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Initialized with world");
      }

   }

   private void onEntityRemove(EntityRemoveEvent event) {
      if (this.entryPoint != null && this.entryPoint.isInitialized()) {
         Entity entity = event.getEntity();
         if (entity != null) {
            this.entryPoint.onEntityDespawn(entity);
         }
      }

   }

   private void onPlayerInteract(PlayerInteractEvent event) {
      if (this.entryPoint != null && this.entryPoint.isInitialized()) {
         Entity player = event.getPlayer();
         Entity target = event.getTargetEntity();
         if (player != null && target != null) {
            this.entryPoint.onEntitySpawn(target);
            HytaleEntityAdapter adapter = this.entryPoint.getEntityAdapter();
            Optional<Long> playerIdOpt = adapter.getEntityId(player);
            Optional<Long> targetIdOpt = adapter.getEntityId(target);
            if (!playerIdOpt.isEmpty() && !targetIdOpt.isEmpty()) {
               long playerId = (Long)playerIdOpt.get();
               long targetId = (Long)targetIdOpt.get();
               if (this.shouldInterceptForFeeding(playerId, targetId)) {
                  event.setCancelled(true);
                  ((HytaleLogger.Api)LOGGER.atInfo()).log("HyTame: Intercepted feeding interaction for entity %d", targetId);
               }

               String actionType = event.getActionType() != null ? event.getActionType().name().toLowerCase() : "interact";
               this.entryPoint.onPlayerInteract(player, target, actionType);
            }
         }
      }
   }

   private boolean shouldInterceptForFeeding(long playerId, long targetId) {
      AnimalTamingPlugin plugin = this.entryPoint.getPlugin();
      HytaleSystemContext context = this.entryPoint.getSystemContext();
      Optional<TamingProgress> progressOpt = plugin.getTamingService().getTamingProgress(targetId);
      if (progressOpt.isEmpty()) {
         return false;
      } else {
         TamingProgress progress = (TamingProgress)progressOpt.get();
         TamingState state = progress.state();
         if (state != TamingState.CALMED && state != TamingState.BONDING_FEED) {
            return false;
         } else {
            Optional<String> heldItem = context.getHeldItemId(playerId);
            if (heldItem.isEmpty()) {
               return false;
            } else {
               Optional<TamingConfig> configOpt = plugin.getConfigRegistry().get(progress.speciesId());
               return configOpt.isEmpty() ? false : ((TamingConfig)configOpt.get()).acceptsFood((String)heldItem.get());
            }
         }
      }
   }

   public HytaleModEntryPoint getEntryPoint() {
      return this.entryPoint;
   }
}
