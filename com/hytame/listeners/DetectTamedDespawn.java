package com.hytame.listeners;

import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytame.HyTamePlugin;
import com.hytame.managers.TamingManager;
import com.hytame.util.EcsReflectionUtil;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class DetectTamedDespawn extends EntityTickingSystem<EntityStore> {
   private final Query<EntityStore> QUERY;

   public DetectTamedDespawn() {
      this.QUERY = Query.and(new Query[]{EcsReflectionUtil.NPC_TYPE, EcsReflectionUtil.DESPAWN_TYPE});
   }

   public Query<EntityStore> getQuery() {
      return this.QUERY;
   }

   public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
      try {
         NPCEntity npcEntity = (NPCEntity)archetypeChunk.getComponent(index, EcsReflectionUtil.NPC_TYPE);
         boolean entityExists = true;
         if (npcEntity == null || npcEntity.isDespawning()) {
            entityExists = false;
         }

         if (archetypeChunk.getComponent(index, EcsReflectionUtil.DESPAWN_TYPE) != null) {
            entityExists = false;
         }

         if (entityExists) {
            return;
         }

         TransformComponent transformComp = (TransformComponent)archetypeChunk.getComponent(index, EcsReflectionUtil.TRANSFORM_TYPE);
         Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
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

         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
         if (uuidComp == null) {
            this.log("UUIDComponent is null for despawned entity");
            return;
         }

         UUID entityId = uuidComp.getUuid();
         boolean isTamed = tamingManager.isTamed(entityId);
         this.log("Is entity tamed? " + isTamed);
         if (isTamed) {
            try {
               CoopResidentComponent coopComp = (CoopResidentComponent)store.getComponent(ref, EcsReflectionUtil.COOP_RESIDENT_TYPE);
               if (coopComp != null) {
                  this.log("Skipping despawn - animal in coop: " + String.valueOf(entityId));
                  return;
               }
            } catch (Exception var22) {
            }

            Vector3d position = transformComp.getPosition();
            double x = position.x();
            double y = position.y();
            double z = position.z();
            tamingManager.onTamedAnimalDespawn(entityId, x, y, z);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[DetectTamedDespawn] " + message);
         }

      }
   }
}
