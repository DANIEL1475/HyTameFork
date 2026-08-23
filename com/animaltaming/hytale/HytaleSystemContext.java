package com.animaltaming.hytale;

import com.animaltaming.system.SystemContext;
import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.joml.Vector3d;

public class HytaleSystemContext implements SystemContext {
   private final HytaleEntityAdapter entityAdapter;
   private World currentWorld;
   private long currentTick = 0L;
   private final Map<String, Set<Long>> tameablesBySpecies = new HashMap();
   private final Map<Long, String> entitySpecies = new HashMap();
   private final List<SystemContext.InteractionEvent> pendingInteractions = new ArrayList();

   public HytaleSystemContext(HytaleEntityAdapter entityAdapter) {
      this.entityAdapter = (HytaleEntityAdapter)Objects.requireNonNull(entityAdapter, "entityAdapter is required");
   }

   public void setWorld(World world) {
      this.currentWorld = world;
   }

   public World getWorld() {
      return this.currentWorld;
   }

   public HytaleEntityAdapter getEntityAdapter() {
      return this.entityAdapter;
   }

   public void tick() {
      ++this.currentTick;
      this.pendingInteractions.clear();
   }

   public long getCurrentTick() {
      return this.currentTick;
   }

   public int getTickRate() {
      return 30;
   }

   public boolean entityExists(long entityId) {
      return (Boolean)this.entityAdapter.getEntity(entityId).map((entity) -> {
         Ref<EntityStore> ref = entity.getReference();
         return ref != null && ref.isValid();
      }).orElse(false);
   }

   private static TransformComponent getTransform(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         Ref<EntityStore> ref = entity.getReference();
         if (ref == null) {
            return null;
         } else {
            Store<EntityStore> store = ref.getStore();
            return store == null ? null : (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
         }
      }
   }

   public double getEntityX(long entityId) {
      return (Double)this.entityAdapter.getEntity(entityId).map((entity) -> {
         TransformComponent tc = getTransform(entity);
         if (tc == null) {
            return (double)0.0F;
         } else {
            Vector3d pos = tc.getPosition();
            return pos != null ? pos.x() : (double)0.0F;
         }
      }).orElse((double)0.0F);
   }

   public double getEntityY(long entityId) {
      return (Double)this.entityAdapter.getEntity(entityId).map((entity) -> {
         TransformComponent tc = getTransform(entity);
         if (tc == null) {
            return (double)0.0F;
         } else {
            Vector3d pos = tc.getPosition();
            return pos != null ? pos.y() : (double)0.0F;
         }
      }).orElse((double)0.0F);
   }

   public double getEntityZ(long entityId) {
      return (Double)this.entityAdapter.getEntity(entityId).map((entity) -> {
         TransformComponent tc = getTransform(entity);
         if (tc == null) {
            return (double)0.0F;
         } else {
            Vector3d pos = tc.getPosition();
            return pos != null ? pos.z() : (double)0.0F;
         }
      }).orElse((double)0.0F);
   }

   public List<SystemContext.PlayerInfo> getAllPlayers() {
      List<SystemContext.PlayerInfo> players = new ArrayList();

      for(Map.Entry<Long, Entity> entry : this.entityAdapter.getAllMappings()) {
         Entity entity = (Entity)entry.getValue();
         if (entity instanceof Player player) {
            players.add(new SystemContext.PlayerInfo((Long)entry.getKey(), player.getUuid(), player.getLegacyDisplayName()));
         }
      }

      return players;
   }

   public List<Long> getPlayersInRadius(double x, double y, double z, double radius) {
      double radiusSquared = radius * radius;
      List<Long> result = new ArrayList();

      for(Map.Entry<Long, Entity> entry : this.entityAdapter.getAllMappings()) {
         Entity entity = (Entity)entry.getValue();
         if (entity instanceof Player) {
            TransformComponent tc = getTransform(entity);
            if (tc != null) {
               Vector3d pos = tc.getPosition();
               if (pos != null) {
                  double dx = pos.x() - x;
                  double dy = pos.y() - y;
                  double dz = pos.z() - z;
                  double distSquared = dx * dx + dy * dy + dz * dz;
                  if (distSquared <= radiusSquared) {
                     result.add((Long)entry.getKey());
                  }
               }
            }
         }
      }

      return result;
   }

   public boolean isPlayerSneaking(long playerId) {
      return (Boolean)this.entityAdapter.getEntity(playerId).filter((e) -> e instanceof Player).map((entity) -> {
         Ref<EntityStore> ref = entity.getReference();
         if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            MovementStatesComponent msc = (MovementStatesComponent)store.getComponent(ref, MovementStatesComponent.getComponentType());
            if (msc == null) {
               return false;
            } else {
               MovementStates states = msc.getMovementStates();
               return states != null && states.crouching;
            }
         } else {
            return false;
         }
      }).orElse(false);
   }

   public Optional<String> getHeldItemId(long playerId) {
      return this.entityAdapter.getEntity(playerId).filter((e) -> e instanceof LivingEntity).map((e) -> (LivingEntity)e).map(LivingEntity::getInventory).map(Inventory::getItemInHand).filter((itemStack) -> itemStack != null && !itemStack.isEmpty()).map(ItemStack::getItemId);
   }

   public boolean consumeHeldItem(long playerId) {
      return (Boolean)this.entityAdapter.getEntity(playerId).filter((e) -> e instanceof LivingEntity).map((e) -> (LivingEntity)e).map((entity) -> {
         Inventory inventory = entity.getInventory();
         ItemStack held = inventory.getItemInHand();
         if (held != null && !held.isEmpty()) {
            ItemStack reduced = held.withQuantity(held.getQuantity() - 1);
            ItemContainer hotbar = inventory.getHotbar();
            byte activeSlot = inventory.getActiveHotbarSlot();
            if (activeSlot >= 0) {
               hotbar.setItemStackForSlot((short)activeSlot, reduced);
            }

            return true;
         } else {
            return false;
         }
      }).orElse(false);
   }

   public List<Long> getRiders(long entityId) {
      return (List)this.entityAdapter.getEntity(entityId).map((entity) -> {
         Ref<EntityStore> ref = entity.getReference();
         if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            MountedByComponent mbc = (MountedByComponent)store.getComponent(ref, MountedByComponent.getComponentType());
            if (mbc == null) {
               return List.of();
            } else {
               List<Ref<EntityStore>> passengers = mbc.getPassengers();
               Stream var10000 = passengers.stream().filter(Ref::isValid);
               HytaleEntityAdapter var10001 = this.entityAdapter;
               Objects.requireNonNull(var10001);
               return var10000.map(var10001::findEntityIdByRef).filter(Optional::isPresent).map(Optional::get).toList();
            }
         } else {
            return List.of();
         }
      }).orElse(List.of());
   }

   public void registerTameableAnimal(long entityId, String speciesId) {
      ((Set)this.tameablesBySpecies.computeIfAbsent(speciesId, (k) -> new HashSet())).add(entityId);
      this.entitySpecies.put(entityId, speciesId);
   }

   public void unregisterTameableAnimal(long entityId) {
      String speciesId = (String)this.entitySpecies.remove(entityId);
      if (speciesId != null) {
         Set<Long> species = (Set)this.tameablesBySpecies.get(speciesId);
         if (species != null) {
            species.remove(entityId);
            if (species.isEmpty()) {
               this.tameablesBySpecies.remove(speciesId);
            }
         }
      }

   }

   public int cleanupStaleTameableAnimals() {
      int removed = 0;
      Iterator<Map.Entry<Long, String>> it = this.entitySpecies.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry<Long, String> entry = (Map.Entry)it.next();
         long entityId = (Long)entry.getKey();
         if (!this.entityExists(entityId)) {
            String speciesId = (String)entry.getValue();
            it.remove();
            ++removed;
            Set<Long> species = (Set)this.tameablesBySpecies.get(speciesId);
            if (species != null) {
               species.remove(entityId);
               if (species.isEmpty()) {
                  this.tameablesBySpecies.remove(speciesId);
               }
            }
         }
      }

      return removed;
   }

   public void clearTameableAnimals() {
      this.tameablesBySpecies.clear();
      this.entitySpecies.clear();
   }

   public List<SystemContext.TameableAnimalInfo> getTameableAnimals() {
      List<SystemContext.TameableAnimalInfo> result = new ArrayList();

      for(Map.Entry<Long, String> entry : this.entitySpecies.entrySet()) {
         long entityId = (Long)entry.getKey();
         String speciesId = (String)entry.getValue();
         this.entityAdapter.getEntity(entityId).ifPresent((entity) -> result.add(new SystemContext.TameableAnimalInfo(entityId, entity.getUuid(), speciesId)));
      }

      return result;
   }

   public Optional<Long> getEntityIdForAnimal(UUID animalId) {
      return this.entityAdapter.getEntityIdByUuid(animalId);
   }

   public void queueInteraction(long playerEntityId, long targetEntityId, String type) {
      this.pendingInteractions.add(new SystemContext.InteractionEvent(playerEntityId, targetEntityId, type));
   }

   public List<SystemContext.InteractionEvent> getPendingInteractions() {
      return List.copyOf(this.pendingInteractions);
   }

   public void teleport(long entityId, double x, double y, double z) {
      this.entityAdapter.getEntity(entityId).ifPresent((entity) -> {
         TransformComponent tc = getTransform(entity);
         if (tc != null) {
            tc.setPosition(new Vector3d(x, y, z));
         }

      });
   }

   public void moveEntityToward(long entityId, double targetX, double targetY, double targetZ, double speed) {
      this.entityAdapter.getEntity(entityId).ifPresent((entity) -> {
         TransformComponent tc = getTransform(entity);
         if (tc != null) {
            Vector3d pos = tc.getPosition();
            if (pos != null) {
               double dx = targetX - pos.x();
               double dy = targetY - pos.y();
               double dz = targetZ - pos.z();
               double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
               if (!(distance < 0.1)) {
                  double moveDistance = speed / (double)this.getTickRate();
                  if (moveDistance > distance) {
                     moveDistance = distance;
                  }

                  double factor = moveDistance / distance;
                  double newX = pos.x() + dx * factor;
                  double newY = pos.y() + dy * factor;
                  double newZ = pos.z() + dz * factor;
                  tc.setPosition(new Vector3d(newX, newY, newZ));
               }
            }
         }
      });
   }

   public void spawnParticle(double x, double y, double z, String particleType) {
      System.out.println("[HyTame] Particle '" + particleType + "' at (" + String.format("%.1f, %.1f, %.1f", x, y, z) + ")");
   }

   public void playSound(double x, double y, double z, String soundType) {
      System.out.println("[HyTame] Sound '" + soundType + "' at (" + String.format("%.1f, %.1f, %.1f", x, y, z) + ")");
   }

   public void sendMessage(long playerId, String message) {
      this.entityAdapter.getPlayer(playerId).ifPresent((player) -> player.getPlayerRef().sendMessage(Message.raw(message)));
   }
}
