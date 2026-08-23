package com.hytame.listeners;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.systems.NPCSystems;
import com.hytame.HyTamePlugin;
import com.hytame.managers.TamingManager;
import com.hytame.util.EcsReflectionUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

public class DetectTamedDeath extends NPCSystems.OnDeathSystem {
   private static final List<UUID> lastDetectedDeaths = Collections.synchronizedList(new ArrayList());
   private static final int MAX_TRACKED = 10;
   @Nonnull
   private static final Query<EntityStore> QUERY = Archetype.of(new ComponentType[0]);

   public static List<UUID> getLastDetectedDeaths() {
      synchronized(lastDetectedDeaths) {
         return new ArrayList(lastDetectedDeaths);
      }
   }

   public static void clearTrackedDeaths() {
      lastDetectedDeaths.clear();
   }

   private static void trackDetectedDeath(UUID uuid) {
      synchronized(lastDetectedDeaths) {
         lastDetectedDeaths.add(0, uuid);

         while(lastDetectedDeaths.size() > 10) {
            lastDetectedDeaths.remove(lastDetectedDeaths.size() - 1);
         }

      }
   }

   @Nonnull
   public Query<EntityStore> getQuery() {
      return QUERY;
   }

   public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
      try {
         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
         if (uuidComp == null) {
            this.log("UUIDComponent is null for entity with DeathComponent");
            return;
         }

         UUID entityId = uuidComp.getUuid();
         trackDetectedDeath(entityId);
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            this.log("Plugin instance is null");
            return;
         }

         TamingManager tamingManager = plugin.getTamingManager();
         if (tamingManager == null) {
            this.log("TamingManager is null");
            return;
         }

         boolean isTamed = tamingManager.isTamed(entityId);
         if (!isTamed) {
            return;
         }

         tamingManager.onTamedAnimalDeath(entityId);
      } catch (Exception e) {
         this.log("Exception in onComponentAdded: " + e.getMessage());
         e.printStackTrace();
      }

   }

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[DetectTamedDeath] " + message);
         }

      }
   }
}
