package com.hytame.util;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderInfo;
import com.hypixel.hytale.server.npc.asset.builder.BuilderManager;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

public final class EcsReflectionUtil {
   public static final ComponentType<EntityStore, NPCEntity> NPC_TYPE = NPCEntity.getComponentType();
   public static final ComponentType<EntityStore, DespawnComponent> DESPAWN_TYPE = DespawnComponent.getComponentType();
   public static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE = TransformComponent.getComponentType();
   public static final ComponentType<EntityStore, ModelComponent> MODEL_TYPE = ModelComponent.getComponentType();
   public static final ComponentType<EntityStore, UUIDComponent> UUID_TYPE = UUIDComponent.getComponentType();
   public static final ComponentType<EntityStore, Player> PLAYER_TYPE = Player.getComponentType();
   public static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE = PlayerRef.getComponentType();
   public static final ComponentType<EntityStore, Interactions> INTERACTIONS_TYPE = Interactions.getComponentType();
   public static final ComponentType<EntityStore, Interactable> INTERACTABLE_TYPE = Interactable.getComponentType();
   public static final ComponentType<EntityStore, Nameplate> NAMEPLATE_TYPE = Nameplate.getComponentType();
   public static final ComponentType<EntityStore, DeathComponent> DEATH_TYPE = DeathComponent.getComponentType();
   public static final ComponentType<EntityStore, EntityStatMap> ENTITY_STAT_MAP_TYPE = EntityStatMap.getComponentType();
   public static final ComponentType<EntityStore, CoopResidentComponent> COOP_RESIDENT_TYPE = CoopResidentComponent.getComponentType();
   public static final ComponentType<EntityStore, NetworkId> NETWORK_ID_TYPE = NetworkId.getComponentType();
   private static Map<String, String> roleModCache = null;
   private static Map<String, Path> rolePathCache = null;
   private static List<String> allItemIdsCache = null;

   private EcsReflectionUtil() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
   }

   public static String getStableEntityKey(Ref<EntityStore> entityRef) {
      if (!(entityRef instanceof Ref)) {
         return null;
      } else {
         try {
            Store<EntityStore> store = entityRef.getStore();
            if (store != null) {
               UUIDComponent uuidComp = (UUIDComponent)store.getComponent(entityRef, UUID_TYPE);
               if (uuidComp != null && uuidComp.getUuid() != null) {
                  return uuidComp.getUuid().toString();
               }
            }
         } catch (Exception var4) {
         }

         try {
            Integer index = entityRef.getIndex();
            if (index != null) {
               return "idx:" + index;
            }
         } catch (Exception var3) {
         }

         return null;
      }
   }

   public static String getEntityModelAssetId(Store<EntityStore> store, Ref<EntityStore> entityRef) {
      try {
         ModelComponent modelComp = (ModelComponent)store.getComponent(entityRef, MODEL_TYPE);
         if (modelComp == null) {
            return null;
         } else {
            Model model = modelComp.getModel();
            return model == null ? null : model.getModelAssetId();
         }
      } catch (Exception var4) {
         return null;
      }
   }

   public static String getEntityModelId(Entity entity) {
      try {
         Object entityRef = getEntityRef(entity);
         if (entityRef != null && entityRef instanceof Ref) {
            World world = entity.getWorld();
            if (world != null) {
               Store<EntityStore> store = world.getEntityStore().getStore();
               ModelComponent modelComp = (ModelComponent)store.getComponent((Ref)entityRef, MODEL_TYPE);
               if (modelComp != null) {
                  Model model = modelComp.getModel();
                  if (model != null) {
                     return model.getModelAssetId();
                  }
               }
            }
         }
      } catch (Exception var6) {
      }

      return entity.toString();
   }

   public static Ref<EntityStore> getEntityRef(Entity entity) {
      Ref<EntityStore> ref = entity.getReference();
      if (ref != null) {
         return ref;
      } else {
         try {
            Method getRef = entity.getClass().getMethod("getRef");
            return (Ref)getRef.invoke(entity);
         } catch (NoSuchMethodException var5) {
            try {
               Method getEntityRefMethod = entity.getClass().getMethod("getEntityRef");
               return (Ref)getEntityRefMethod.invoke(entity);
            } catch (Exception var4) {
            }
         } catch (Exception var6) {
         }

         return null;
      }
   }

   public static UUID getEntityUUID(Entity entity) {
      try {
         Object entityRef = getEntityRef(entity);
         if (entityRef != null && entityRef instanceof Ref) {
            World world = entity.getWorld();
            if (world != null) {
               Store<EntityStore> store = world.getEntityStore().getStore();
               UUIDComponent uuidComp = (UUIDComponent)store.getComponent((Ref)entityRef, UUID_TYPE);
               if (uuidComp != null) {
                  UUID uuid = uuidComp.getUuid();
                  if (uuid != null) {
                     return uuid;
                  }
               }
            }
         }
      } catch (Exception var6) {
      }

      Object entityRef = getEntityRef(entity);
      return entityRef != null ? UUID.nameUUIDFromBytes(("entity_" + entityRef.toString()).getBytes()) : UUID.nameUUIDFromBytes(entity.toString().getBytes());
   }

   public static Set<String> enumerateNpcRoleNames() {
      Set<String> names = new TreeSet();

      try {
         NPCPlugin npcPlugin = NPCPlugin.get();

         for(int i = 0; i < 5000; ++i) {
            try {
               String name = npcPlugin.getName(i);
               if (name != null && !name.isEmpty()) {
                  names.add(name);
               }
            } catch (Exception var4) {
               break;
            }
         }
      } catch (Exception var5) {
      }

      return names;
   }

   public static String resolveNpcRolePath(String roleName) {
      try {
         NPCPlugin npcPlugin = NPCPlugin.get();
         int index = npcPlugin.getIndex(roleName);
         if (index >= 0) {
            BuilderManager builderManager = npcPlugin.getBuilderManager();
            BuilderInfo builderInfo = builderManager.tryGetBuilderInfo(index);
            if (builderInfo != null && builderInfo.getPath() != null) {
               String result = convertPathToAssetPath(builderInfo.getPath());
               if (result != null) {
                  return result;
               }
            }
         }
      } catch (Exception var6) {
      }

      if (rolePathCache == null) {
         buildRoleModCache();
      }

      if (rolePathCache == null) {
         return null;
      } else {
         Path cachedPath = (Path)rolePathCache.get(roleName);
         return cachedPath != null ? convertPathToAssetPath(cachedPath) : null;
      }
   }

   private static String convertPathToAssetPath(Path path) {
      String pathStr = path.toString().replace('\\', '/');
      int npcIdx = pathStr.indexOf("NPC/Roles/");
      if (npcIdx < 0) {
         return null;
      } else {
         String assetPath = pathStr.substring(npcIdx);
         if (assetPath.endsWith(".json")) {
            assetPath = assetPath.substring(0, assetPath.length() - 5);
         }

         return assetPath;
      }
   }

   public static UUID getUuidFromRef(Ref<EntityStore> ref) {
      try {
         Store<EntityStore> store = ref.getStore();
         if (store != null) {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUID_TYPE);
            if (uuidComp != null) {
               UUID uuid = uuidComp.getUuid();
               if (uuid != null) {
                  return uuid;
               }
            }
         }
      } catch (Exception var5) {
      }

      try {
         Integer index = ref.getIndex();
         if (index != null) {
            return UUID.nameUUIDFromBytes(("entity_ref_" + index).getBytes());
         }
      } catch (Exception var4) {
      }

      return UUID.nameUUIDFromBytes(ref.toString().getBytes());
   }

   public static String debugModPaths(String roleName) {
      StringBuilder sb = new StringBuilder();

      try {
         NPCPlugin npcPlugin = NPCPlugin.get();
         int index = npcPlugin.getIndex(roleName);
         sb.append("Role: ").append(roleName).append(" idx=").append(index).append("\n");
         if (index < 0) {
            return sb.toString();
         }

         BuilderManager builderManager = npcPlugin.getBuilderManager();
         BuilderInfo builderInfo = builderManager.tryGetBuilderInfo(index);
         if (builderInfo == null) {
            sb.append("BuilderInfo: null\n");
            return sb.toString();
         }

         sb.append("-- BuilderInfo methods --\n");

         for(Method m : builderInfo.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && !m.getDeclaringClass().equals(Object.class)) {
               try {
                  Object val = m.invoke(builderInfo);
                  String valStr = val != null ? val.toString() : "null";
                  if (valStr.length() > 120) {
                     valStr = valStr.substring(0, 120) + "...";
                  }

                  sb.append("  ").append(m.getName()).append("() = ").append(valStr).append("\n");
               } catch (Exception e) {
                  sb.append("  ").append(m.getName()).append("() ERR: ").append(e.getMessage()).append("\n");
               }
            }
         }

         AssetModule assetModule = AssetModule.get();
         if (assetModule != null) {
            AssetPack basePack = assetModule.getBaseAssetPack();

            for(AssetPack pack : assetModule.getAssetPacks()) {
               if (pack != basePack) {
                  sb.append("-- AssetPack methods (").append(pack.getName()).append(") --\n");

                  for(Method m : pack.getClass().getMethods()) {
                     if (m.getParameterCount() == 0 && !m.getDeclaringClass().equals(Object.class)) {
                        try {
                           Object val = m.invoke(pack);
                           String valStr = val != null ? val.toString() : "null";
                           if (valStr.length() > 120) {
                              valStr = valStr.substring(0, 120) + "...";
                           }

                           sb.append("  ").append(m.getName()).append("() = ").append(valStr).append("\n");
                        } catch (Exception e) {
                           sb.append("  ").append(m.getName()).append("() ERR: ").append(e.getMessage()).append("\n");
                        }
                     }
                  }
                  break;
               }
            }

            Path rolePath = builderInfo.getPath();
            sb.append("-- findAssetPackForPath --\n");

            try {
               Method findMethod = assetModule.getClass().getMethod("findAssetPackForPath", Path.class);
               Object result = findMethod.invoke(assetModule, rolePath);
               sb.append("  result=").append(result != null ? result.toString() : "null").append("\n");
               if (result instanceof AssetPack) {
                  sb.append("  pack.getName()=").append(((AssetPack)result).getName()).append("\n");
               }
            } catch (NoSuchMethodException var17) {
               sb.append("  method not found\n");
            } catch (Exception e) {
               sb.append("  error: ").append(e.getMessage()).append("\n");
            }

            sb.append("-- AssetModule methods --\n");

            for(Method m : assetModule.getClass().getMethods()) {
               if (m.getParameterCount() <= 1 && !m.getDeclaringClass().equals(Object.class) && !m.getName().startsWith("register") && !m.getName().startsWith("init") && !m.getName().startsWith("wait") && !m.getName().startsWith("notify")) {
                  sb.append("  ").append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(");

                  for(Class<?> p : m.getParameterTypes()) {
                     sb.append(p.getSimpleName());
                  }

                  sb.append(")\n");
               }
            }
         }
      } catch (Exception e) {
         sb.append("Error: ").append(e.getMessage()).append("\n");
      }

      return sb.toString();
   }

   private static void buildRoleModCache() {
      roleModCache = new HashMap();
      rolePathCache = new HashMap();

      try {
         NPCPlugin npcPlugin = NPCPlugin.get();
         BuilderManager builderManager = npcPlugin.getBuilderManager();
         AssetModule assetModule = AssetModule.get();
         AssetPack basePack = assetModule != null ? assetModule.getBaseAssetPack() : null;
         ObjectIterator var4 = builderManager.getAllBuilders().values().iterator();

         while(var4.hasNext()) {
            BuilderInfo info = (BuilderInfo)var4.next();

            try {
               String name = info.getKeyName();
               if (name != null && !name.isEmpty()) {
                  if (info.getPath() != null) {
                     rolePathCache.put(name, info.getPath());
                  }

                  String modName = resolveModForBuilder(info, assetModule, basePack);
                  roleModCache.put(name, modName);
               }
            } catch (Exception var8) {
            }
         }

         if (assetModule != null) {
            scanAssetPacksForRoles(assetModule, basePack);
         }

         if (roleModCache.isEmpty()) {
            roleModCache = null;
            rolePathCache = null;
         }
      } catch (Exception var9) {
         if (roleModCache != null && roleModCache.isEmpty()) {
            roleModCache = null;
            rolePathCache = null;
         }
      }

   }

   private static void scanAssetPacksForRoles(AssetModule assetModule, AssetPack basePack) {
      try {
         for(AssetPack pack : assetModule.getAssetPacks()) {
            Path root = pack.getRoot();
            Path rolesDir = root.resolve("NPC").resolve("Roles");
            if (!Files.isDirectory(rolesDir, new LinkOption[0])) {
               rolesDir = root.resolve("Server").resolve("NPC").resolve("Roles");
            }

            if (Files.isDirectory(rolesDir, new LinkOption[0])) {
               String modName = pack == basePack ? "Hytale" : extractPackDisplayName(pack);
               Stream<Path> stream = Files.walk(rolesDir);

               try {
                  stream.filter((p) -> p.toString().endsWith(".json")).forEach((jsonPath) -> {
                     try {
                        String fileName = jsonPath.getFileName().toString();
                        String roleName = fileName.substring(0, fileName.length() - 5);
                        if (roleName.isEmpty()) {
                           return;
                        }

                        if (!rolePathCache.containsKey(roleName)) {
                           rolePathCache.put(roleName, jsonPath);
                        }

                        if (!roleModCache.containsKey(roleName) || roleModCache.get(roleName) == null) {
                           roleModCache.put(roleName, modName);
                        }
                     } catch (Exception var4) {
                     }

                  });
               } catch (Throwable var11) {
                  if (stream != null) {
                     try {
                        stream.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (stream != null) {
                  stream.close();
               }
            }
         }
      } catch (Exception var12) {
      }

   }

   private static String resolveModForBuilder(BuilderInfo info, AssetModule assetModule, AssetPack basePack) {
      try {
         if (info != null && info.getPath() != null && assetModule != null) {
            AssetPack pack = assetModule.findAssetPackForPath(info.getPath());
            if (pack == null) {
               return null;
            } else if (pack == basePack) {
               return "Hytale";
            } else {
               try {
                  String shortName = pack.getManifest().getName();
                  if (shortName != null && !shortName.isEmpty()) {
                     return shortName;
                  }
               } catch (Exception var6) {
               }

               String packName = pack.getName();
               int colonIdx = packName.indexOf(58);
               return colonIdx >= 0 ? packName.substring(colonIdx + 1) : packName;
            }
         } else {
            return null;
         }
      } catch (Exception var7) {
         return null;
      }
   }

   private static String extractPackDisplayName(AssetPack pack) {
      try {
         String shortName = pack.getManifest().getName();
         if (shortName != null && !shortName.isEmpty()) {
            return shortName;
         }
      } catch (Exception var3) {
      }

      String packName = pack.getName();
      int colonIdx = packName.indexOf(58);
      return colonIdx >= 0 ? packName.substring(colonIdx + 1) : packName;
   }

   public static String getModNameForNpcRole(String roleName) {
      if (roleModCache == null) {
         buildRoleModCache();
      }

      return (String)roleModCache.get(roleName);
   }

   public static void clearRoleModCache() {
      roleModCache = null;
      rolePathCache = null;
   }

   public static List<String> readRoleLovedItems(String roleName) {
      if (rolePathCache == null) {
         buildRoleModCache();
      }

      Path rolePath = (Path)rolePathCache.get(roleName);
      if (rolePath == null) {
         return Collections.emptyList();
      } else {
         try {
            String content = Files.readString(rolePath);
            String[] keys = new String[]{"\"LovedItems\"", "\"$.LovedItems\"", "\"AttractiveItemSet\"", "\"$.AttractiveItemSet\""};

            for(String key : keys) {
               int keyIdx = content.indexOf(key);
               if (keyIdx != -1) {
                  int arrayStart = content.indexOf("[", keyIdx);
                  if (arrayStart != -1 && arrayStart <= keyIdx + key.length() + 10) {
                     int arrayEnd = content.indexOf("]", arrayStart);
                     if (arrayEnd != -1) {
                        String arrayContent = content.substring(arrayStart + 1, arrayEnd);
                        List<String> foods = new ArrayList();

                        for(String item : arrayContent.split(",")) {
                           String trimmed = item.trim().replace("\"", "");
                           if (!trimmed.isEmpty()) {
                              foods.add(trimmed);
                           }
                        }

                        if (!foods.isEmpty()) {
                           return foods;
                        }
                     }
                  }
               }
            }
         } catch (Exception var18) {
         }

         return Collections.emptyList();
      }
   }

   public static List<String> enumerateAllItemIds() {
      if (allItemIdsCache != null) {
         return allItemIdsCache;
      } else {
         List<String> items = new ArrayList();

         try {
            Map<String, ?> itemMap = Item.getAssetMap().getAssetMap();
            items.addAll(itemMap.keySet());
         } catch (Exception var2) {
         }

         Collections.sort(items);
         allItemIdsCache = Collections.unmodifiableList(items);
         return allItemIdsCache;
      }
   }

   public static void clearItemIdCache() {
      allItemIdsCache = null;
   }

   public static Map<String, List<String>> enumerateNpcRolesByMod() {
      if (roleModCache == null) {
         buildRoleModCache();
      }

      Map<String, List<String>> byMod = new TreeMap();

      for(Map.Entry<String, String> entry : roleModCache.entrySet()) {
         String mod = entry.getValue() != null ? (String)entry.getValue() : "Unknown";
         ((List)byMod.computeIfAbsent(mod, (k) -> new ArrayList())).add((String)entry.getKey());
      }

      for(List<String> roles : byMod.values()) {
         Collections.sort(roles);
      }

      LinkedHashMap<String, List<String>> result = new LinkedHashMap();
      List<String> hytaleRoles = (List)byMod.remove("Hytale");
      if (hytaleRoles != null) {
         result.put("Hytale", hytaleRoles);
      }

      result.putAll(byMod);
      return result;
   }
}
