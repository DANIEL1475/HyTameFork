package com.hytame.listeners;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.util.EcsReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UseBlockHandler extends EntityEventSystem<EntityStore, UseBlockEvent> {
   private static int eventCount = 0;
   private static String lastPlayer = "none";
   private static long lastEventTime = 0L;

   public UseBlockHandler() {
      super(UseBlockEvent.class);
   }

   public static int getEventCount() {
      return eventCount;
   }

   public static String getLastPlayer() {
      return lastPlayer;
   }

   public static long getLastEventTime() {
      return lastEventTime;
   }

   public void handle(int entityIndex, @NotNull ArchetypeChunk<EntityStore> chunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer, @NotNull UseBlockEvent event) {
      ++eventCount;
      lastEventTime = System.currentTimeMillis();

      try {
         PlayerRef playerRef = (PlayerRef)chunk.getComponent(entityIndex, EcsReflectionUtil.PLAYER_REF_TYPE);
         if (playerRef != null) {
            lastPlayer = playerRef.getUsername();
         }
      } catch (Exception var7) {
      }

   }

   public @Nullable Query<EntityStore> getQuery() {
      return EcsReflectionUtil.PLAYER_REF_TYPE;
   }
}
