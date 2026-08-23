package com.hytame.listeners;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hytame.HyTamePlugin;
import com.hytame.util.EcsReflectionUtil;
import java.util.Set;
import javax.annotation.Nonnull;

public class LaitDamageDisabler extends DeathSystems {
   @Nonnull
   private static final Query QUERY;
   @Nonnull
   private static final Set DEPENDENCIES;
   static final boolean $assertionsDisabled;

   public Query getQuery() {
      return QUERY;
   }

   @Nonnull
   public Set getDependencies() {
      return Set.of(new SystemGroupDependency(Order.AFTER, DamageModule.get().getInspectDamageGroup()), new SystemDependency(Order.BEFORE, DeathSystems.ClearHealth.class));
   }

   public void handle(int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer, @Nonnull Damage damage) {
      EntityStatMap entityStatMapComponent = (EntityStatMap)archetypeChunk.getComponent(index, EcsReflectionUtil.ENTITY_STAT_MAP_TYPE);
      if (!$assertionsDisabled && entityStatMapComponent == null) {
         throw new AssertionError();
      } else {
         Archetype archetype = archetypeChunk.getArchetype();
         boolean dead = archetype.contains(EcsReflectionUtil.DEATH_TYPE);
         if (dead) {
            damage.setCancelled(true);
         }

      }
   }

   private void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[HyTame] " + message);
         }

      }
   }

   static {
      QUERY = EcsReflectionUtil.ENTITY_STAT_MAP_TYPE;
      $assertionsDisabled = !DamageSystems.class.desiredAssertionStatus();
      DEPENDENCIES = Set.of(new SystemGroupDependency(Order.AFTER, DamageModule.get().getInspectDamageGroup()), new SystemDependency(Order.BEFORE, DeathSystems.ClearHealth.class));
   }
}
