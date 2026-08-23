package com.hytame.interactions;

import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockFace;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
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
import it.unimi.dsi.fastutil.Pair;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bson.BsonDocument;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public class HyTameCaptureInteraction extends SimpleBlockInteraction {
   public static final BuilderCodec<HyTameCaptureInteraction> CODEC;
   protected String[] acceptedNpcGroupIds;
   protected int[] acceptedNpcGroupIndexes;
   protected String fullIcon;

   protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
      CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
      if (commandBuffer == null) {
         context.getState().state = InteractionState.Failed;
      } else {
         ItemStack item = context.getHeldItem();
         if (item == null) {
            context.getState().state = InteractionState.Failed;
         } else {
            Ref<EntityStore> playerRef = context.getEntity();
            LivingEntity playerEntity = (LivingEntity)EntityUtils.getEntity(playerRef, commandBuffer);
            Inventory playerInventory = playerEntity.getInventory();
            byte activeHotbarSlot = playerInventory.getActiveHotbarSlot();
            ItemStack inHandItemStack = playerInventory.getActiveHotbarItem();
            CapturedNPCMetadata existingMeta = (CapturedNPCMetadata)item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);
            if (existingMeta != null) {
               super.tick0(firstRun, time, type, context, cooldownHandler);
            } else {
               Ref<EntityStore> targetEntity = context.getTargetEntity();
               if (targetEntity == null) {
                  context.getState().state = InteractionState.Failed;
               } else {
                  NPCEntity npc = (NPCEntity)commandBuffer.getComponent(targetEntity, NPCEntity.getComponentType());
                  if (npc == null) {
                     context.getState().state = InteractionState.Failed;
                  } else {
                     TagSetPlugin.TagSetLookup tagSetPlugin = TagSetPlugin.get(NPCGroup.class);
                     boolean tagFound = false;

                     for(int group : this.acceptedNpcGroupIndexes) {
                        if (tagSetPlugin.tagInSet(group, npc.getRoleIndex())) {
                           tagFound = true;
                           break;
                        }
                     }

                     if (!tagFound) {
                        context.getState().state = InteractionState.Failed;
                     } else {
                        PersistentModel persistentModel = (PersistentModel)commandBuffer.getComponent(targetEntity, PersistentModel.getComponentType());
                        if (persistentModel == null) {
                           context.getState().state = InteractionState.Failed;
                        } else {
                           ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset(persistentModel.getModelReference().getModelAssetId());
                           CapturedNPCMetadata meta = (CapturedNPCMetadata)inHandItemStack.getFromMetadataOrDefault("CapturedEntity", CapturedNPCMetadata.CODEC);
                           if (modelAsset != null) {
                              meta.setIconPath(modelAsset.getIcon());
                           }

                           String npcName = NPCPlugin.get().getName(npc.getRoleIndex());
                           if (npcName != null) {
                              meta.setNpcNameKey(npcName);
                           }

                           if (this.fullIcon != null) {
                              meta.setFullItemIcon(this.fullIcon);
                           }

                           ItemStack itemWithNPC = inHandItemStack.withMetadata(CapturedNPCMetadata.KEYED_CODEC, meta);
                           itemWithNPC = this.writeHyTameMetadata(itemWithNPC, targetEntity, commandBuffer);
                           playerInventory.getHotbar().replaceItemStackInSlot((short)activeHotbarSlot, item, itemWithNPC);
                           commandBuffer.removeEntity(targetEntity, RemoveReason.REMOVE);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock, @Nonnull CooldownHandler cooldownHandler) {
      ItemStack item = context.getHeldItem();
      if (item == null) {
         context.getState().state = InteractionState.Failed;
      } else {
         Ref<EntityStore> playerRef = context.getEntity();
         LivingEntity playerEntity = (LivingEntity)EntityUtils.getEntity(playerRef, commandBuffer);
         Inventory playerInventory = playerEntity.getInventory();
         byte activeHotbarSlot = playerInventory.getActiveHotbarSlot();
         CapturedNPCMetadata existingMeta = (CapturedNPCMetadata)item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);
         if (existingMeta == null) {
            context.getState().state = InteractionState.Failed;
         } else {
            BlockPosition pos = context.getTargetBlock();
            if (pos == null) {
               context.getState().state = InteractionState.Failed;
            } else {
               UUID hytameId = (UUID)item.getFromMetadataOrNull("HyTame.Id", Codec.UUID_STRING);
               UUID ownerUuid = (UUID)item.getFromMetadataOrNull("HyTame.OwnerUuid", Codec.UUID_STRING);
               String ownerName = (String)item.getFromMetadataOrNull("HyTame.OwnerName", Codec.STRING);
               String customName = (String)item.getFromMetadataOrNull("HyTame.Name", Codec.STRING);
               String animalTypeStr = (String)item.getFromMetadataOrNull("HyTame.Type", Codec.STRING);
               Boolean isTamed = (Boolean)item.getFromMetadataOrNull("HyTame.Tamed", Codec.BOOLEAN);
               if (Boolean.TRUE.equals(isTamed) && existingMeta != null) {
                  HyTameCoopData coopData = new HyTameCoopData(hytameId, ownerUuid, ownerName, customName, animalTypeStr, true);
                  CoopCodecExtender.setHyTameData(existingMeta, coopData);
                  log("Stored HyTame data on metadata for coop persistence");
               }

               ItemStack noMetaItemStack = item.withMetadata((BsonDocument)null);
               Object worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
               Ref<ChunkStore> blockRef = ((WorldChunk)worldChunk).getBlockComponentEntity(pos.x, pos.y, pos.z);
               if (blockRef == null) {
                  blockRef = BlockModule.ensureBlockEntity((WorldChunk)worldChunk, pos.x, pos.y, pos.z);
               }

               if (blockRef != null) {
                  Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                  CoopBlock coopBlockState = (CoopBlock)chunkStore.getComponent(blockRef, CoopBlock.getComponentType());
                  if (coopBlockState != null) {
                     WorldTimeResource worldTimeResource = (WorldTimeResource)commandBuffer.getResource(WorldTimeResource.getResourceType());
                     if (coopBlockState.tryPutResident(existingMeta, worldTimeResource)) {
                        world.execute(() -> coopBlockState.ensureSpawnResidentsInWorld(world, world.getEntityStore().getStore(), new Vector3d((double)pos.x, (double)pos.y, (double)pos.z), new Vector3d((double)0.0F, (double)0.0F, (double)1.0F)));
                        playerInventory.getHotbar().replaceItemStackInSlot((short)activeHotbarSlot, item, noMetaItemStack);
                     } else {
                        context.getState().state = InteractionState.Failed;
                     }

                     return;
                  }
               }

               Vector3d spawnPos = new Vector3d((double)((float)pos.x + 0.5F), (double)pos.y, (double)((float)pos.z + 0.5F));
               if (context.getClientState() != null) {
                  BlockFace blockFace = BlockFace.fromProtocolFace(context.getClientState().blockFace);
                  if (blockFace != null) {
                     Vector3ic dir = blockFace.getDirection();
                     spawnPos.add((double)dir.x(), (double)dir.y(), (double)dir.z());
                  }
               }

               NPCPlugin npcModule = NPCPlugin.get();
               Store<EntityStore> store = commandBuffer.getStore();
               int roleIndex = NPCPlugin.get().getIndex(existingMeta.getNpcNameKey());
               boolean fIsTamed = Boolean.TRUE.equals(isTamed);
               commandBuffer.run((_store) -> {
                  Pair<Ref<EntityStore>, NPCEntity> result = npcModule.spawnEntity(store, roleIndex, spawnPos, Rotation3f.ZERO, (Model)null, (TriConsumer)null);
                  if (result != null && fIsTamed) {
                     Ref<EntityStore> npcRef = (Ref)result.first();
                     HyTamePlugin plugin = HyTamePlugin.getInstance();
                     if (plugin != null) {
                        ComponentType<EntityStore, HyTameComponent> hyTameType = plugin.getHyTameComponentType();
                        if (hyTameType != null) {
                           try {
                              HyTameComponent comp = (HyTameComponent)store.ensureAndGetComponent(npcRef, hyTameType);
                              if (comp != null) {
                                 if (ownerUuid != null && ownerName != null) {
                                    comp.setTamed(ownerUuid, ownerName);
                                 }

                                 if (hytameId != null) {
                                    comp.setHytameId(hytameId);
                                 }

                                 log("Applied HyTameComponent: owner=" + ownerName + " hytameId=" + String.valueOf(hytameId));
                              }
                           } catch (Exception e) {
                              log("Failed to apply HyTameComponent: " + e.getMessage());
                           }
                        }
                     }

                     world.execute(() -> {
                        try {
                           if (customName != null && !customName.isEmpty()) {
                              NameplateUtil.setEntityNameplate(npcRef, customName);
                              log("Restored nameplate: " + customName);
                           }

                           this.registerInTamingManager(npcRef, hytameId, ownerUuid, ownerName, customName, animalTypeStr);
                        } catch (Exception e) {
                           log("Failed deferred restore: " + e.getMessage());
                        }

                     });
                  }

               });
               playerInventory.getHotbar().replaceItemStackInSlot((short)activeHotbarSlot, item, noMetaItemStack);
            }
         }
      }
   }

   protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
   }

   private ItemStack writeHyTameMetadata(ItemStack itemStack, Ref<EntityStore> targetEntity, CommandBuffer<EntityStore> commandBuffer) {
      try {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            return itemStack;
         }

         ComponentType<EntityStore, HyTameComponent> hyTameType = plugin.getHyTameComponentType();
         if (hyTameType == null) {
            log("writeHyTameMetadata: hyTameType is null");
            return itemStack;
         }

         HyTameComponent hyTameComp = (HyTameComponent)commandBuffer.getComponent(targetEntity, hyTameType);
         if (hyTameComp == null) {
            Store<EntityStore> store = commandBuffer.getStore();
            if (store != null) {
               hyTameComp = (HyTameComponent)store.getComponent(targetEntity, hyTameType);
            }
         }

         if (hyTameComp == null) {
            log("writeHyTameMetadata: HyTameComponent not found on entity");
            return itemStack;
         }

         if (!hyTameComp.isTamed()) {
            log("writeHyTameMetadata: entity is not tamed");
            return itemStack;
         }

         UUID hytameId = hyTameComp.getHytameId();
         UUID ownerUuid = hyTameComp.getTamerUUID();
         String ownerName = hyTameComp.getTamerName();
         String customName = null;
         String animalTypeStr = null;
         TamingManager tamingManager = plugin.getTamingManager();
         if (tamingManager != null && hytameId != null) {
            TamedAnimalData tamedData = tamingManager.findByHytameId(hytameId);
            if (tamedData != null) {
               customName = tamedData.getCustomName();
               AnimalType type = tamedData.getAnimalType();
               if (type != null) {
                  animalTypeStr = type.name();
               }

               tamedData.setCaptured(true);
               tamingManager.saveImmediately();
            }
         }

         itemStack = itemStack.withMetadata("HyTame.Tamed", Codec.BOOLEAN, true);
         if (hytameId != null) {
            itemStack = itemStack.withMetadata("HyTame.Id", Codec.UUID_STRING, hytameId);
         }

         if (ownerUuid != null) {
            itemStack = itemStack.withMetadata("HyTame.OwnerUuid", Codec.UUID_STRING, ownerUuid);
         }

         if (ownerName != null) {
            itemStack = itemStack.withMetadata("HyTame.OwnerName", Codec.STRING, ownerName);
         }

         if (customName != null) {
            itemStack = itemStack.withMetadata("HyTame.Name", Codec.STRING, customName);
         }

         if (animalTypeStr != null) {
            itemStack = itemStack.withMetadata("HyTame.Type", Codec.STRING, animalTypeStr);
         }

         log("Wrote HyTame metadata to capture crate: hytameId=" + String.valueOf(hytameId) + " owner=" + ownerName + " name=" + customName + " type=" + animalTypeStr);
      } catch (Exception e) {
         log("Failed to write HyTame metadata: " + e.getMessage());
      }

      return itemStack;
   }

   private void registerInTamingManager(Ref<EntityStore> npcRef, UUID hytameId, UUID ownerUuid, String ownerName, String customName, String animalTypeStr) {
      try {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin == null) {
            return;
         }

         TamingManager tamingManager = plugin.getTamingManager();
         if (tamingManager == null) {
            return;
         }

         Store<EntityStore> store = npcRef.getStore();
         if (store == null) {
            log("Cannot register in TamingManager: store is null");
            return;
         }

         UUIDComponent uuidComp = (UUIDComponent)store.getComponent(npcRef, EcsReflectionUtil.UUID_TYPE);
         UUID entityUuid = uuidComp != null ? uuidComp.getUuid() : null;
         if (entityUuid == null || ownerUuid == null) {
            String var23 = String.valueOf(entityUuid);
            log("Cannot register in TamingManager: entityUuid=" + var23 + " ownerUuid=" + String.valueOf(ownerUuid));
            return;
         }

         AnimalType type = null;
         if (animalTypeStr != null) {
            try {
               type = AnimalType.valueOf(animalTypeStr);
            } catch (IllegalArgumentException var21) {
            }
         }

         Vector3d capPos = EntityUtil.getPositionFromRef(npcRef);
         double capX = capPos != null ? capPos.x() : (double)0.0F;
         double capY = capPos != null ? capPos.y() : (double)0.0F;
         double capZ = capPos != null ? capPos.z() : (double)0.0F;
         TamedAnimalData data = tamingManager.tameAnimal(hytameId, entityUuid, ownerUuid, customName != null ? customName : ownerName + "'s pet", type, npcRef, capX, capY, capZ, (GrowthStage)null, (String)null);
         if (data != null) {
            data.setCaptured(false);
            tamingManager.saveImmediately();
            String var10000 = String.valueOf(hytameId);
            log("Restored tamed animal from capture crate: hytameId=" + var10000 + " entityUuid=" + String.valueOf(entityUuid) + " owner=" + ownerName + " name=" + customName);
         }
      } catch (Exception e) {
         log("Failed to register in TamingManager: " + e.getMessage());
      }

   }

   private static void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[HyTameCapture] " + message);
         }

      }
   }

   static {
      CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(HyTameCaptureInteraction.class, HyTameCaptureInteraction::new, SimpleInteraction.CODEC).appendInherited(new KeyedCodec("AcceptedNpcGroups", NPCGroup.CHILD_ASSET_CODEC_ARRAY), (o, v) -> o.acceptedNpcGroupIds = v, (o) -> o.acceptedNpcGroupIds, (o, p) -> o.acceptedNpcGroupIds = p.acceptedNpcGroupIds).addValidator(NPCGroup.VALIDATOR_CACHE.getArrayValidator()).add()).appendInherited(new KeyedCodec("FullIcon", Codec.STRING), (o, v) -> o.fullIcon = v, (o) -> o.fullIcon, (o, p) -> o.fullIcon = p.fullIcon).add()).afterDecode((data) -> {
         if (data.acceptedNpcGroupIds != null) {
            data.acceptedNpcGroupIndexes = new int[data.acceptedNpcGroupIds.length];

            for(int i = 0; i < data.acceptedNpcGroupIds.length; ++i) {
               data.acceptedNpcGroupIndexes[i] = NPCGroup.getAssetMap().getIndex(data.acceptedNpcGroupIds[i]);
            }
         }

      })).build();
   }
}
