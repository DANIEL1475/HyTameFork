package com.hytame.listeners;

import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import com.hytame.HyTamePlugin;
import com.hytame.coop.CoopCodecExtender;
import com.hytame.coop.HyTameCoopData;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.tame.HyTameComponent;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import com.hytame.util.NameplateUtil;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class CoopResidentTracker extends RefSystem<EntityStore> {
   private static final ComponentType<EntityStore, CoopResidentComponent> COOP_RESIDENT_TYPE = CoopResidentComponent.getComponentType();

   public Query<EntityStore> getQuery() {
      return COOP_RESIDENT_TYPE;
   }

   public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
      try {
         CoopResidentComponent coopComp = (CoopResidentComponent)store.getComponent(ref, COOP_RESIDENT_TYPE);
         if (coopComp == null) {
            return;
         }

         Vector3i coopPos = coopComp.getCoopLocation();
         if (coopPos == null) {
            return;
         }

         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
         if (uuidComp != null) {
            uuidComp.getUuid();
         } else {
            Object var10000 = null;
         }

         World world = ((EntityStore)commandBuffer.getExternalData()).getWorld();
         world.execute(() -> {
            try {
               restoreFromCoop(ref, coopPos, world);
            } catch (Exception e) {
               e.printStackTrace();
            }

         });
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static void restoreFromCoop(Ref<EntityStore> ref, Vector3i coopPos, World world) {
      if (ref.isValid()) {
         Store<EntityStore> store = ref.getStore();
         if (store != null) {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
            UUID entityUuid = uuidComp != null ? uuidComp.getUuid() : null;
            if (entityUuid != null) {
               CoopBlock coopBlock = getCoopBlockAt(world, coopPos);
               if (coopBlock != null) {
                  List<CoopBlock.CoopResident> residents = CoopCodecExtender.getResidents(coopBlock);
                  if (residents != null && !residents.isEmpty()) {
                     CoopBlock.CoopResident matchedResident = null;

                     for(int i = 0; i < residents.size(); ++i) {
                        CoopBlock.CoopResident resident = (CoopBlock.CoopResident)residents.get(i);
                        PersistentRef persistentRef = resident.getPersistentRef();
                        UUID refUuid = persistentRef != null ? persistentRef.getUuid() : null;
                        boolean hasHyTame = CoopCodecExtender.hasHyTameData(resident.getMetadata());
                        if (persistentRef != null && entityUuid.equals(refUuid)) {
                           matchedResident = resident;
                           break;
                        }
                     }

                     if (matchedResident != null) {
                        CapturedNPCMetadata metadata = matchedResident.getMetadata();
                        HyTameCoopData data = CoopCodecExtender.getHyTameData(metadata);
                        if (data != null && data.isTamed()) {
                           HyTamePlugin plugin = HyTamePlugin.getInstance();
                           if (plugin != null) {
                              ComponentType<EntityStore, HyTameComponent> hyTameType = plugin.getHyTameComponentType();
                              if (hyTameType != null) {
                                 try {
                                    HyTameComponent hyTameComp = (HyTameComponent)store.ensureAndGetComponent(ref, hyTameType);
                                    if (hyTameComp != null) {
                                       UUID ownerUuid = data.getOwnerUuid();
                                       String ownerName = data.getOwnerName();
                                       hyTameComp.setTamed(ownerUuid != null ? ownerUuid : UUID.randomUUID(), ownerName != null ? ownerName : "Unknown");
                                       UUID hytameId = data.getHytameId();
                                       if (hytameId != null) {
                                          hyTameComp.setHytameId(hytameId);
                                       }
                                    }
                                 } catch (Exception e) {
                                    e.printStackTrace();
                                 }
                              }

                              String customName = data.getCustomName();
                              if (customName != null && !customName.isEmpty()) {
                                 try {
                                    NameplateUtil.setEntityNameplate(ref, customName);
                                 } catch (Exception e) {
                                    e.printStackTrace();
                                 }
                              }

                              TamingManager tamingManager = plugin.getTamingManager();
                              if (tamingManager != null) {
                                 AnimalType type = null;
                                 if (data.getAnimalType() != null) {
                                    try {
                                       type = AnimalType.valueOf(data.getAnimalType());
                                    } catch (IllegalArgumentException var24) {
                                    }
                                 }

                                 Vector3d entityPos = EntityUtil.getPositionFromRef(ref);
                                 double coopX = entityPos != null ? entityPos.x() : (double)0.0F;
                                 double coopY = entityPos != null ? entityPos.y() : (double)0.0F;
                                 double coopZ = entityPos != null ? entityPos.z() : (double)0.0F;
                                 TamedAnimalData tamedData = tamingManager.tameAnimal(data.getHytameId(), entityUuid, data.getOwnerUuid(), customName != null ? customName : "Tamed Animal", type, ref, coopX, coopY, coopZ, (GrowthStage)null, (String)null);
                                 if (tamedData != null) {
                                    tamedData.setOwnerName(data.getOwnerName());
                                    tamedData.setCaptured(false);
                                    tamingManager.saveImmediately();
                                 }
                              }

                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
      try {
         if (reason == RemoveReason.UNLOAD) {
            return;
         }

         CoopResidentComponent coopComp = (CoopResidentComponent)store.getComponent(ref, COOP_RESIDENT_TYPE);
         CoopResidentComponent coopCompCB = (CoopResidentComponent)commandBuffer.getComponent(ref, COOP_RESIDENT_TYPE);
         if (coopComp == null) {
            coopComp = coopCompCB;
         }

         if (coopComp == null) {
            return;
         }

         boolean markedForDespawn = coopComp.getMarkedForDespawn();
         if (!markedForDespawn) {
            return;
         }

         Vector3i coopPos = coopComp.getCoopLocation();
         if (coopPos == null) {
            return;
         }

         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
         if (uuidComp == null) {
            uuidComp = (UUIDComponent)commandBuffer.getComponent(ref, EcsReflectionUtil.UUID_TYPE);
         }

         UUID entityUuid = uuidComp != null ? uuidComp.getUuid() : null;
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            return;
         }

         ComponentType<EntityStore, HyTameComponent> hyTameType = plugin.getHyTameComponentType();
         if (hyTameType == null) {
            return;
         }

         HyTameComponent hyTameComp = (HyTameComponent)store.getComponent(ref, hyTameType);
         if (hyTameComp == null) {
            hyTameComp = (HyTameComponent)commandBuffer.getComponent(ref, hyTameType);
         }

         if (hyTameComp == null || !hyTameComp.isTamed()) {
            return;
         }

         TamingManager tamingManager = plugin.getTamingManager();
         TamedAnimalData tamedData = null;
         if (tamingManager != null && entityUuid != null) {
            tamedData = tamingManager.getTamedData(entityUuid);
            if (tamedData != null && tamedData.isCaptured()) {
               return;
            }
         }

         HyTameCoopData coopData = HyTameCoopData.from(hyTameComp, tamedData);
         World world = ((EntityStore)commandBuffer.getExternalData()).getWorld();
         CoopBlock coopBlock = getCoopBlockAt(world, coopPos);
         if (coopBlock == null) {
            return;
         }

         List<CoopBlock.CoopResident> residents = CoopCodecExtender.getResidents(coopBlock);
         if (residents == null || residents.isEmpty()) {
            return;
         }

         NPCEntity npcComp = (NPCEntity)store.getComponent(ref, EcsReflectionUtil.NPC_TYPE);
         if (npcComp == null) {
            npcComp = (NPCEntity)commandBuffer.getComponent(ref, EcsReflectionUtil.NPC_TYPE);
         }

         int entityRoleIndex = npcComp != null ? npcComp.getRoleIndex() : -1;
         CoopBlock.CoopResident matchedResident = null;

         for(int i = 0; i < residents.size(); ++i) {
            CoopBlock.CoopResident resident = (CoopBlock.CoopResident)residents.get(i);
            CapturedNPCMetadata meta = resident.getMetadata();
            boolean deployed = resident.getDeployedToWorld();
            PersistentRef pRef = resident.getPersistentRef();
            boolean hasHyTame = CoopCodecExtender.hasHyTameData(meta);
            int metaRole = NPCPlugin.get().getIndex(meta.getNpcNameKey());
            if (!deployed && pRef == null && !hasHyTame && entityRoleIndex >= 0 && metaRole == entityRoleIndex) {
               matchedResident = resident;
               break;
            }
         }

         if (matchedResident == null) {
            return;
         }

         CoopCodecExtender.setHyTameData(matchedResident.getMetadata(), coopData);
         if (tamingManager != null && tamedData != null) {
            tamedData.setCaptured(true);
            tamingManager.saveImmediately();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private static CoopBlock getCoopBlockAt(World world, Vector3i pos) {
      try {
         Object worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
         if (worldChunk == null) {
            return null;
         } else {
            Ref<ChunkStore> blockRef = ((WorldChunk)worldChunk).getBlockComponentEntity(pos.x, pos.y, pos.z);
            if (blockRef == null) {
               blockRef = BlockModule.ensureBlockEntity((WorldChunk)worldChunk, pos.x, pos.y, pos.z);
            }

            if (blockRef == null) {
               return null;
            } else {
               Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
               return (CoopBlock)chunkStore.getComponent(blockRef, CoopBlock.getComponentType());
            }
         }
      } catch (Exception var5) {
         return null;
      }
   }

   private static void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[CoopTracker] " + message);
         }

      }
   }
}
