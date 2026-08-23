package com.hytame.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.commands.HytamePermissions;
import com.hytame.ui.ConfigPanelUIPage;
import com.hytame.util.EcsReflectionUtil;

public class OpenConfigPanelInteraction extends SimpleInteraction {
   public static final BuilderCodec<OpenConfigPanelInteraction> CODEC;
   private boolean shouldFail = false;

   protected void tick0(boolean firstRun, float time, InteractionType type, InteractionContext context, CooldownHandler cooldownHandler) {
      if (firstRun) {
         this.shouldFail = false;

         try {
            Player player = this.getPlayerFromContext(context);
            if (player == null) {
               this.shouldFail = true;
               return;
            }

            World world = player.getWorld();
            if (world == null) {
               this.shouldFail = true;
               return;
            }

            boolean isAdmin = HytamePermissions.hasAdminAccess(player);
            world.execute(() -> {
               try {
                  Ref<EntityStore> playerEntityRef = player.getReference();
                  Store<EntityStore> store = playerEntityRef.getStore();
                  ConfigPanelUIPage configPage = new ConfigPanelUIPage(player.getPlayerRef(), !isAdmin);
                  player.getPageManager().openCustomPage(playerEntityRef, store, configPage);
               } catch (Exception e) {
                  this.log("Failed to open config panel: " + e.getMessage());
               }

            });
         } catch (Exception e) {
            this.log("Error in OpenConfigPanelInteraction: " + e.getMessage());
            this.shouldFail = true;
         }
      }

      if (!this.shouldFail) {
         super.tick0(firstRun, time, type, context, cooldownHandler);
      }

   }

   private Player getPlayerFromContext(InteractionContext context) {
      try {
         Ref<EntityStore> entityRef = context.getEntity();
         if (entityRef == null) {
            return null;
         } else {
            Store<EntityStore> store = entityRef.getStore();
            return store == null ? null : (Player)store.getComponent(entityRef, EcsReflectionUtil.PLAYER_TYPE);
         }
      } catch (Exception var4) {
         return null;
      }
   }

   private void log(String msg) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log("[OpenConfigPanel] " + msg);
      }

   }

   static {
      CODEC = BuilderCodec.builder(OpenConfigPanelInteraction.class, OpenConfigPanelInteraction::new, SimpleInteraction.CODEC).build();
   }
}
