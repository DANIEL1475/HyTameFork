package com.hytame.handlers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.effects.EffectsManager;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.InteractionSetupManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.util.ConfigManager;
import com.hytame.util.EntityUtil;
import java.util.List;
import java.util.UUID;

public class MouseInteractionHandler {
   private final ConfigManager configManager;
   private final BreedingManager breedingManager;
   private final EffectsManager effectsManager;
   private final InteractionSetupManager interactionSetupManager;
   private TamingManager tamingManager;

   public MouseInteractionHandler(ConfigManager configManager, BreedingManager breedingManager, EffectsManager effectsManager, InteractionSetupManager interactionSetupManager) {
      this.configManager = configManager;
      this.breedingManager = breedingManager;
      this.effectsManager = effectsManager;
      this.interactionSetupManager = interactionSetupManager;
   }

   public void setTamingManager(TamingManager tamingManager) {
      this.tamingManager = tamingManager;
   }

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   public void onMouseButton(PlayerMouseButtonEvent event) {
      try {
         this.handleMouseClick(event);
      } catch (Exception var3) {
      }

   }

   public void onPlayerInteract(PlayerInteractEvent event) {
      try {
         Entity targetEntity = event.getTargetEntity();
         if (targetEntity == null) {
            return;
         }

         if (targetEntity instanceof Player) {
            return;
         }

         Player player = event.getPlayer();
         UUID entityId = EntityUtil.getEntityUUID(targetEntity);
         String entityName = EntityUtil.getEntityModelId(targetEntity);
         AnimalType animalType = AnimalType.fromEntityTypeId(entityName);
         if (animalType == null) {
            return;
         }

         if (!this.configManager.isBreedingEnabled(animalType) && !this.configManager.isTamingEnabled(animalType)) {
            return;
         }

         Object entityRef = EntityUtil.getEntityRef(targetEntity);
         if (entityRef != null && entityRef instanceof Ref<EntityStore> ref) {
            World world = targetEntity.getWorld();
            if (world != null) {
               Store<EntityStore> store = world.getEntityStore().getStore();
               this.interactionSetupManager.setupEntityInteractions(store, ref, animalType);
            }
         }
      } catch (Exception var11) {
      }

   }

   private void handleMouseClick(PlayerMouseButtonEvent event) {
      Player player = event.getPlayer();
      if (event.getMouseButton().mouseButtonType == MouseButtonType.Right) {
         Ref<EntityStore> targetRef = event.getTargetEntityRef();
         if (targetRef != null) {
            World clickWorld = player.getWorld();
            if (clickWorld != null) {
               Entity targetEntity = EntityUtils.getEntity(targetRef, clickWorld.getEntityStore().getStore());
               if (targetEntity != null) {
                  if (!(targetEntity instanceof Player)) {
                     this.log("[TamingDebug] handleMouseClick triggered on entity");
                     Item heldItem = event.getItemInHand();
                     String itemId = heldItem != null ? heldItem.getId() : null;
                     String entityName = EntityUtil.getEntityModelId(targetEntity);
                     AnimalType animalType = AnimalType.fromEntityTypeId(entityName);
                     if (animalType != null) {
                        try {
                           Object entityRef = EntityUtil.getEntityRef(targetEntity);
                           if (entityRef != null && entityRef instanceof Ref) {
                              Ref<EntityStore> ref = (Ref)entityRef;
                              World world = targetEntity.getWorld();
                              if (world != null) {
                                 Store<EntityStore> store = world.getEntityStore().getStore();
                                 this.interactionSetupManager.setupEntityInteractions(store, ref, animalType);
                              }
                           }
                        } catch (Exception var17) {
                        }

                        if (heldItem != null) {
                           UUID entityId = EntityUtil.getEntityUUID(targetEntity);
                           BreedingManager.FeedResult result = this.breedingManager.tryFeed(entityId, animalType, itemId);
                           if (result == BreedingManager.FeedResult.SUCCESS || result == BreedingManager.FeedResult.ALREADY_IN_LOVE) {
                              Ref<EntityStore> entityRef = EntityUtil.getEntityRef(targetEntity);
                              if (entityRef != null) {
                                 BreedingData data = this.breedingManager.getData(entityId);
                                 if (data != null && data.getEntityRef() == null) {
                                    data.setEntityRef(entityRef);
                                 }
                              }
                           }

                           String var10001 = animalType.getId();
                           this.log("Feed result for " + var10001 + ": " + String.valueOf(result));
                           if (player != null) {
                              switch (result) {
                                 case SUCCESS:
                                    PlayerRef var25 = player.getPlayerRef();
                                    var10001 = this.capitalize(animalType.getId());
                                    var25.sendMessage(Message.raw("[HyTame] " + var10001 + " is now in love!"));
                                    this.effectsManager.spawnHeartParticlesAtEntity(targetEntity);
                                    break;
                                 case ALREADY_IN_LOVE:
                                    player.getPlayerRef().sendMessage(Message.raw("[HyTame] This " + animalType.getId() + " is already in love!"));
                                    this.effectsManager.spawnHeartParticlesAtEntity(targetEntity);
                                    break;
                                 case WRONG_FOOD:
                                    List<String> validFoods = this.configManager.getBreedingFoods(animalType);
                                    String foodList = String.join(", ", validFoods);
                                    PlayerRef var24 = player.getPlayerRef();
                                    var10001 = this.capitalize(animalType.getId());
                                    var24.sendMessage(Message.raw("[HyTame] Wrong food! " + var10001 + " needs: " + foodList));
                                    break;
                                 case DISABLED:
                                    player.getPlayerRef().sendMessage(Message.raw("[HyTame] Breeding is disabled for " + animalType.getId() + "s"));
                                    break;
                                 case NOT_ADULT:
                                    player.getPlayerRef().sendMessage(Message.raw("[HyTame] This " + animalType.getId() + " is too young to breed"));
                                    break;
                                 case ON_COOLDOWN:
                                    BreedingData data = this.breedingManager.getData(entityId);
                                    if (data != null) {
                                       long remaining = data.getCooldownRemaining(this.configManager.getBreedingCooldown(animalType));
                                       PlayerRef var10000 = player.getPlayerRef();
                                       var10001 = animalType.getId();
                                       var10000.sendMessage(Message.raw("[HyTame] This " + var10001 + " needs to rest (" + remaining / 1000L + "s)"));
                                    } else {
                                       player.getPlayerRef().sendMessage(Message.raw("[HyTame] This " + animalType.getId() + " needs to rest"));
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

   private String capitalize(String str) {
      if (str != null && !str.isEmpty()) {
         String var10000 = str.substring(0, 1).toUpperCase();
         return var10000 + str.substring(1);
      } else {
         return str;
      }
   }
}
