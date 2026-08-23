package com.hytame.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import java.util.function.BiConsumer;
import org.joml.Vector3d;

public class EffectsManager {
   private static final String HEARTS_PARTICLE = "Hearts";
   private static final String FEEDING_SOUND = "SFX_Consume_Bread";
   private static final double PARTICLE_HEIGHT_OFFSET = (double)1.5F;
   private BiConsumer<Store<EntityStore>, Object> positionRetriever;

   private void logWarning(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atWarning()).log(message);
      }

   }

   private void logVerbose(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   public void playFeedingSound(Entity targetEntity) {
      try {
         int soundId = SoundEvent.getAssetMap().getIndex("SFX_Consume_Bread");
         if (soundId < 0) {
            return;
         }

         Vector3d pos = EntityUtil.getEntityPosition(targetEntity);
         if (pos == null) {
            return;
         }

         World world = targetEntity.getWorld();
         if (world == null) {
            return;
         }

         Store<EntityStore> store = world.getEntityStore().getStore();
         if (store == null) {
            return;
         }

         SoundUtil.playSoundEvent3d(soundId, SoundCategory.SFX, pos.x(), pos.y(), pos.z(), store);
      } catch (Exception var6) {
      }

   }

   public void playFeedingSoundAtEntity(Entity entity) {
      this.playFeedingSoundAtEntity(entity, (String)null);
   }

   public void playFeedingSoundAtEntity(Entity entity, String worldName) {
      try {
         Vector3d pos = EntityUtil.getEntityPosition(entity);
         if (pos == null) {
            return;
         }

         World world = null;
         if (worldName != null) {
            world = Universe.get().getWorld(worldName);
         }

         if (world == null) {
            world = Universe.get().getDefaultWorld();
         }

         if (world == null) {
            return;
         }

         Store<EntityStore> store = world.getEntityStore().getStore();
         int soundId = SoundEvent.getAssetMap().getIndex("SFX_Consume_Bread");
         if (soundId < 0) {
            return;
         }

         SoundUtil.playSoundEvent3d(soundId, SoundCategory.SFX, pos.x(), pos.y(), pos.z(), store);
      } catch (Exception var7) {
      }

   }

   public void spawnHeartParticlesAtEntity(Entity entity) {
      this.spawnHeartParticlesAtEntity(entity, (String)null);
   }

   public void spawnHeartParticlesAtEntity(Entity entity, String worldName) {
      try {
         Vector3d position = EntityUtil.getEntityPosition(entity);
         if (position == null) {
            return;
         }

         double x = position.x();
         double y = position.y() + (double)1.5F;
         double z = position.z();
         World world = null;
         if (worldName != null) {
            world = Universe.get().getWorld(worldName);
         }

         if (world == null) {
            world = Universe.get().getDefaultWorld();
         }

         if (world == null) {
            return;
         }

         Store<EntityStore> store = world.getEntityStore().getStore();
         Vector3d heartsPos = new Vector3d(x, y, z);
         ParticleUtil.spawnParticleEffect("Hearts", heartsPos, store);
      } catch (Exception var13) {
      }

   }

   public void spawnHeartParticlesAtRef(Store<EntityStore> store, Object entityRef) {
      try {
         if (entityRef == null) {
            this.logWarning("[Hearts] entityRef is null");
            return;
         }

         Ref<EntityStore> ref = (Ref)entityRef;
         if (!ref.isValid()) {
            this.logVerbose("[Hearts] ref is invalid (entity likely despawned)");
            return;
         }

         Vector3d position = this.getPositionFromRef(store, ref);
         if (position == null) {
            Store<EntityStore> refStore = ref.getStore();
            this.logWarning("[Hearts] position is null - ref.getStore()=" + (refStore != null ? "valid" : "NULL") + ", ref.isValid()=" + ref.isValid() + ", ref.getIndex()=" + ref.getIndex());
            return;
         }

         double x = position.x();
         double y = position.y() + (double)1.5F;
         double z = position.z();
         Vector3d heartsPos = new Vector3d(x, y, z);
         ParticleUtil.spawnParticleEffect("Hearts", heartsPos, store);
      } catch (Exception e) {
         this.logWarning("[Hearts] Error in spawnHeartParticlesAtRef: " + e.getMessage());
      }

   }

   public void spawnHeartParticlesAtPosition(Vector3d position, Store<EntityStore> store) {
      try {
         if (position == null || store == null) {
            return;
         }

         Vector3d heartsPos = new Vector3d(position.x(), position.y() + (double)1.5F, position.z());
         ParticleUtil.spawnParticleEffect("Hearts", heartsPos, store);
      } catch (Exception var4) {
      }

   }

   private Vector3d getPositionFromRef(Store<EntityStore> store, Ref<EntityStore> ref) {
      try {
         if (ref == null) {
            return null;
         } else {
            if (store == null) {
               Store<EntityStore> refStore = ref.getStore();
               store = refStore;
            }

            TransformComponent transform = (TransformComponent)store.getComponent(ref, EcsReflectionUtil.TRANSFORM_TYPE);
            if (transform == null) {
               return null;
            } else {
               Vector3d pos = transform.getPosition();
               return pos;
            }
         }
      } catch (Exception e) {
         String var10001 = e.getMessage();
         this.logWarning("[Hearts] getPositionFromRef error: " + var10001 + " - ref is " + (ref != null ? "valid" : "NULL"));
         return null;
      }
   }

   public static String getHeartsParticleName() {
      return "Hearts";
   }

   public static String getFeedingSoundName() {
      return "SFX_Consume_Bread";
   }
}
