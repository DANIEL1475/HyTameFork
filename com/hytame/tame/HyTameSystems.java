package com.hytame.tame;

import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.config.AttitudeGroup;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.hypixel.hytale.server.npc.systems.RoleBuilderSystem;
import com.hytame.HyTamePlugin;
import com.hytame.managers.BreedingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.tame.utils.Debug;
import com.hytame.util.ConfigManager;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class HyTameSystems {
   public static class HyTameActivateSystem extends HolderSystem<EntityStore> {
      @Nonnull
      private final ComponentType<EntityStore, NPCEntity> npcComponentType = (ComponentType)Objects.requireNonNull(NPCEntity.getComponentType());
      private volatile ComponentType<EntityStore, HyTameComponent> hyTameComponentType = HyTameComponent.getComponentType();
      private final Query<EntityStore> query;
      private final Set<Dependency<EntityStore>> dependencies;
      private final Set<String> validGroups;

      public HyTameActivateSystem() {
         this.query = Query.and(new Query[]{this.npcComponentType, Query.not(NPCMountComponent.getComponentType())});
         this.dependencies = Set.of(new SystemDependency(Order.AFTER, RoleBuilderSystem.class));
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null && plugin.getConfigManager() != null) {
            this.validGroups = plugin.getConfigManager().getTameableAnimalGroups();
         } else {
            this.validGroups = Set.of("Livestock", "PreyBig", "Prey");
         }

      }

      @Nonnull
      public Query<EntityStore> getQuery() {
         return this.query;
      }

      @Nonnull
      public Set<Dependency<EntityStore>> getDependencies() {
         return this.dependencies;
      }

      public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store) {
         NPCEntity npcEntity = (NPCEntity)holder.getComponent(this.npcComponentType);
         if (npcEntity != null) {
            Role role = npcEntity.getRole();
            if (role != null) {
               WorldSupport worldSupport = role.getWorldSupport();
               AttitudeGroup attitudeGroup = (AttitudeGroup)AttitudeGroup.getAssetMap().getAsset(worldSupport.getAttitudeGroup());
               boolean groupMatch = attitudeGroup != null && this.validGroups.contains(attitudeGroup.getId());
               if (!groupMatch) {
                  String appearance = npcEntity.getRoleName();
                  if (AnimalType.fromModelAssetId(appearance) == null) {
                     return;
                  }
               }

               if (this.hyTameComponentType == null) {
                  this.hyTameComponentType = HyTameComponent.getComponentType();
                  if (this.hyTameComponentType == null) {
                     Debug.log("HyTameComponentType not yet registered, skipping entity setup", Level.WARNING);
                     return;
                  }

                  Debug.log("HyTameComponentType resolved on retry", Level.INFO);
               }

               HyTameComponent hyTameComponent = (HyTameComponent)holder.ensureAndGetComponent(this.hyTameComponentType);
               if (hyTameComponent.getGrowthStage() == GrowthStage.ADULT) {
                  String roleName = npcEntity.getRoleName();
                  if (roleName != null && AnimalType.isBabyVariant(roleName)) {
                     hyTameComponent.setGrowthStage(GrowthStage.BABY);
                     Debug.log("Set growthStage=BABY for baby variant: " + roleName, Level.INFO);
                  }
               }

               if (hyTameComponent.isTamed()) {
                  try {
                     HyTamePlugin.getAttitudeField().set(worldSupport, Attitude.REVERED);
                  } catch (IllegalAccessException e) {
                     HyTamePlugin pluginRef = HyTamePlugin.getInstance();
                     if (pluginRef != null) {
                        ((HytaleLogger.Api)pluginRef.getLogger().atSevere()).log("Failed to override attitude for NPC", e);
                     }
                  }

                  boolean oldState = npcEntity.updateSpawnTrackingState(false);
                  if (oldState) {
                     Debug.log("Stopped tracking entity " + npcEntity.getRoleName(), Level.INFO);
                  }

                  npcEntity.setSpawnConfiguration(Integer.MIN_VALUE);
               }

            }
         }
      }

      public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store) {
      }
   }

   public static class HyTameTickSystem extends EntityTickingSystem<EntityStore> {
      private static final ComponentType<EntityStore, HyTameComponent> HYTAME_TYPE = HyTameComponent.getComponentType();
      private static final ComponentType<EntityStore, UUIDComponent> UUID_TYPE = UUIDComponent.getComponentType();
      private static final int TICK_INTERVAL = 20;
      private int tickCounter = 0;

      public Query<EntityStore> getQuery() {
         return HYTAME_TYPE;
      }

      public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
         ++this.tickCounter;
         if (this.tickCounter >= 20) {
            this.tickCounter = 0;

            try {
               HyTameComponent hyTame = (HyTameComponent)chunk.getComponent(index, HYTAME_TYPE);
               if (hyTame == null) {
                  return;
               }

               Ref<EntityStore> ref = chunk.getReferenceTo(index);
               if (ref == null || !ref.isValid()) {
                  return;
               }

               boolean isTamed = hyTame.isTamed();
               if (!isTamed) {
                  if (!hyTame.isActionReady()) {
                     hyTame.setActionReady(true);
                  }

                  return;
               }

               HyTamePlugin plugin = HyTamePlugin.getInstance();
               if (plugin == null) {
                  return;
               }

               BreedingManager manager = plugin.getBreedingManager();
               ConfigManager config = plugin.getConfigManager();
               if (manager == null || config == null) {
                  return;
               }

               UUIDComponent uuidComp = (UUIDComponent)chunk.getComponent(index, UUID_TYPE);
               if (uuidComp == null) {
                  if (!hyTame.isActionReady()) {
                     hyTame.setActionReady(true);
                  }

                  return;
               }

               UUID animalId = uuidComp.getUuid();
               BreedingData data = manager.getData(animalId);
               if (data == null) {
                  if (!hyTame.isActionReady()) {
                     hyTame.setActionReady(true);
                  }

                  return;
               }

               if (data.isInLove()) {
                  if (hyTame.isActionReady()) {
                     hyTame.setActionReady(false);
                  }

                  return;
               }

               long cooldownMs = config.getBreedingCooldown(data.getAnimalType());
               if (!data.canBreed(cooldownMs)) {
                  if (hyTame.isActionReady()) {
                     hyTame.setActionReady(false);
                  }

                  return;
               }

               if (!hyTame.isActionReady()) {
                  hyTame.setActionReady(true);
               }
            } catch (Exception var17) {
            }

         }
      }
   }
}
