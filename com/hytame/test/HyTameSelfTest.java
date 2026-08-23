package com.hytame.test;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSpawner;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hytame.HyTamePlugin;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.util.ConfigManager;
import com.hytame.util.EntityUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.joml.Vector3d;

public final class HyTameSelfTest {
   private static final UUID DUMMY_OWNER = new UUID(0L, 1L);
   private static final AnimalType[] ROLE_MATRIX;
   private static final EnumSet<AnimalType> CORE_TAME;
   private static final String FAULT;

   private static boolean fault(String name) {
      return Arrays.asList(FAULT.split(",")).contains(name);
   }

   private HyTameSelfTest() {
   }

   public static boolean enabled() {
      return "true".equalsIgnoreCase(System.getProperty("hytame.selftest"));
   }

   public static void scheduleIfEnabled(HyTamePlugin plugin) {
      if (enabled()) {
         ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("SELFTEST: enabled, scheduling in 12s...");
         plugin.getTickScheduler().schedule(() -> {
            try {
               run(plugin);
            } catch (Throwable t) {
               out("SELFTEST: harness=FAIL " + String.valueOf(t));
            }

         }, 12L, TimeUnit.SECONDS);
      }
   }

   private static void out(String s) {
      System.out.println(s);
   }

   private static void run(HyTamePlugin plugin) {
      int[] cfgCov = runConfigTests(plugin);
      probeParticles();
      World world = Universe.get() != null ? Universe.get().getDefaultWorld() : null;
      if (world == null) {
         out("SELFTEST: world=FAIL (no default world)");
      } else {
         String worldName = world.getName();
         TamingManager taming = plugin.getTamingManager();
         BreedingManager breeding = plugin.getBreedingManager();
         world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            StringBuilder roles = new StringBuilder("SELFTEST-ROLES:");
            String tame = "SKIP";
            String breedFeed = "SKIP";
            String growth = "SKIP";
            int compiled = 0;
            int absent = 0;
            int failed = 0;
            double y = (double)100.0F;
            double x = (double)0.5F;

            for(AnimalType type : ROLE_MATRIX) {
               x += (double)2.0F;
               String role = type.getModelAssetId();
               int idx = roleIndex(role);
               boolean injectRoleFail = fault("role") && type == AnimalType.COW;
               if (idx < 0 && !injectRoleFail) {
                  roles.append(" ").append(role).append("=NA");
                  ++absent;
               } else {
                  try {
                     Ref<EntityStore> ref = injectRoleFail ? null : spawnAt(store, idx, x, y);
                     if (ref == null) {
                        roles.append(" ").append(role).append("=FAIL(spawn)");
                        ++failed;
                     } else {
                        ++compiled;
                        String tag = "ok";
                        UUID uuid = EntityUtil.getUuidFromRef(ref);
                        if (uuid != null && taming != null && CORE_TAME.contains(type)) {
                           TamedAnimalData td = fault("tame") ? null : taming.tameAnimal(uuid, DUMMY_OWNER, "SelfTest_" + role, type, ref, x, y, (double)0.5F, GrowthStage.ADULT, worldName);
                           boolean tamed = td != null && taming.isTamed(uuid);
                           tag = tamed ? "tamed" : "tameFAIL";
                           if (!tamed) {
                              ++failed;
                           }

                           if (type == AnimalType.COW && tamed) {
                              tame = "PASS";
                              ConfigManager cfg = plugin.getConfigManager();
                              if (breeding != null) {
                                 if (fault("breed") && cfg != null) {
                                    cfg.setAnyAnimalBreedingEnabled(type.getId(), false);
                                 }

                                 BreedingManager.FeedResult fr = breeding.tryFeed(uuid, type, type.getBreedingFood(), ref, worldName);
                                 breedFeed = fr == BreedingManager.FeedResult.SUCCESS ? "PASS" : "FAIL(" + String.valueOf(fr) + ")";
                                 if (fault("breed") && cfg != null) {
                                    cfg.setAnyAnimalBreedingEnabled(type.getId(), true);
                                 }
                              }

                              try {
                                 GrowthStage next = GrowthStage.BABY.getNextStage();
                                 growth = next != null && next != GrowthStage.BABY ? "PASS" : "FAIL";
                              } catch (Throwable t) {
                                 growth = "FAIL(" + t.getClass().getSimpleName() + ")";
                              }
                           }
                        }

                        roles.append(" ").append(role).append("=").append(tag);
                     }
                  } catch (Throwable t) {
                     roles.append(" ").append(role).append("=FAIL(").append(t.getClass().getSimpleName()).append(")");
                     ++failed;
                  }
               }
            }

            out(roles.toString());
            out(String.format("SELFTEST: roleCompile=%s tame=%s breedFeed=%s growth=%s", failed == 0 ? "PASS" : "FAIL", tame, breedFeed, growth));
            EnumSet<AnimalType.Category> covered = EnumSet.noneOf(AnimalType.Category.class);

            for(AnimalType t : ROLE_MATRIX) {
               covered.add(t.getCategory());
            }

            int totalCats = AnimalType.Category.values().length;
            int totalAnimals = AnimalType.values().length;
            out(String.format("SELFTEST-COVERAGE: categories=%d/%d roles=%d/%d (compiled=%d absent=%d failed=%d) configChecks=%d/%d", covered.size(), totalCats, ROLE_MATRIX.length, totalAnimals, compiled, absent, failed, cfgCov[0], cfgCov[1]));
         });
      }
   }

   private static int roleIndex(String role) {
      try {
         return NPCPlugin.get().getIndex(role);
      } catch (Exception var2) {
         return -1;
      }
   }

   private static Ref<EntityStore> spawnAt(Store<EntityStore> store, int idx, double x, double y) {
      try {
         NPCPlugin.get().prepareRoleBuilderInfo(idx);
      } catch (Exception var7) {
      }

      return (Ref)NPCPlugin.get().spawnEntity(store, idx, new Vector3d(x, y, (double)0.5F), new Rotation3f(0.0F, 0.0F, 0.0F), (Model)null, (TriConsumer)null, (TriConsumer)null).first();
   }

   private static int[] runConfigTests(HyTamePlugin plugin) {
      ConfigManager cfg = plugin.getConfigManager();
      if (cfg == null) {
         out("SELFTEST-CONFIG: FAIL (no ConfigManager)");
         return new int[]{0, 9};
      } else {
         String id = AnimalType.COW.getId();
         String tameId = AnimalType.WOLF.getId();
         ConfigManager.AnimalConfig ac0 = cfg.getAnimalConfig(AnimalType.COW);
         boolean origBreed = ac0.breedingEnabled;
         double origGrowth = ac0.growthTimeMinutes;
         double origCooldown = ac0.breedCooldownMinutes;
         boolean origWolfTame = cfg.getAnimalConfig(AnimalType.WOLF).tamingEnabled;
         List<String> origFoods = new ArrayList(cfg.getBreedingFoods(AnimalType.COW));
         boolean origGrowthEnabled = cfg.isGrowthEnabled();
         int origPlayerLimit = cfg.getPerPlayerTameLimit();
         int origClaimLimit = cfg.getPerClaimTameLimit();
         String breedToggle = "FAIL";
         String tameToggle = "FAIL";
         String growthRt = "FAIL";
         String cooldownRt = "FAIL";
         String foodList = "FAIL";
         String growthToggle = "FAIL";
         String playerLimit = "FAIL";
         String claimLimit = "FAIL";
         String presets = "FAIL";
         boolean var40 = false;

         label433: {
            label432: {
               try {
                  var40 = true;
                  cfg.setAnyAnimalBreedingEnabled(id, false);
                  boolean bOff = !cfg.isBreedingEnabled(AnimalType.COW);
                  cfg.setAnyAnimalBreedingEnabled(id, true);
                  breedToggle = bOff && cfg.isBreedingEnabled(AnimalType.COW) ? "PASS" : "FAIL";
                  cfg.setAnyAnimalTamingEnabled(tameId, false);
                  boolean tOff = !cfg.isTamingEnabled(AnimalType.WOLF);
                  cfg.setAnyAnimalTamingEnabled(tameId, true);
                  tameToggle = tOff && cfg.isTamingEnabled(AnimalType.WOLF) ? "PASS" : "FAIL";
                  if (!fault("config")) {
                     cfg.setAnyAnimalGrowthTime(id, (double)7.0F);
                  }

                  growthRt = near(cfg.getAnimalConfig(AnimalType.COW).growthTimeMinutes, (double)7.0F) ? "PASS" : "FAIL";
                  cfg.setAnyAnimalCooldown(id, (double)9.0F);
                  cooldownRt = near(cfg.getAnimalConfig(AnimalType.COW).breedCooldownMinutes, (double)9.0F) ? "PASS" : "FAIL";
                  String marker = "Food_SelfTest_Marker";
                  boolean before = cfg.isBreedingFood(AnimalType.COW, marker);
                  if (!fault("food")) {
                     cfg.addAnyAnimalFood(id, marker);
                  }

                  boolean added = cfg.isBreedingFood(AnimalType.COW, marker);
                  cfg.removeAnyAnimalFood(id, marker);
                  boolean removed = !cfg.isBreedingFood(AnimalType.COW, marker);
                  cfg.setAnyAnimalFood(id, marker);
                  boolean setOk = cfg.isBreedingFood(AnimalType.COW, marker) && marker.equalsIgnoreCase(cfg.getBreedingFood(AnimalType.COW));
                  foodList = !before && added && removed && setOk ? "PASS" : "FAIL";
                  cfg.setGrowthEnabled(!origGrowthEnabled);
                  boolean gFlipped = cfg.isGrowthEnabled() == !origGrowthEnabled;
                  cfg.setGrowthEnabled(origGrowthEnabled);
                  growthToggle = gFlipped && cfg.isGrowthEnabled() == origGrowthEnabled ? "PASS" : "FAIL";
                  cfg.setPerPlayerTameLimit(origPlayerLimit + 7);
                  playerLimit = cfg.getPerPlayerTameLimit() == origPlayerLimit + 7 ? "PASS" : "FAIL";
                  cfg.setPerClaimTameLimit(origClaimLimit + 5);
                  claimLimit = cfg.getPerClaimTameLimit() == origClaimLimit + 5 ? "PASS" : "FAIL";
                  List<String> avail = cfg.getAvailablePresets();
                  presets = cfg.getActivePreset() != null && avail != null && avail.size() >= 5 && cfg.isBuiltinPreset("default") ? "PASS" : "FAIL";
                  var40 = false;
                  break label432;
               } catch (Throwable t) {
                  out("SELFTEST-CONFIG: phase crash " + String.valueOf(t));
                  var40 = false;
               } finally {
                  if (var40) {
                     try {
                        cfg.setAnyAnimalBreedingEnabled(id, origBreed);
                        cfg.setAnyAnimalTamingEnabled(tameId, origWolfTame);
                        cfg.setAnyAnimalGrowthTime(id, origGrowth);
                        cfg.setAnyAnimalCooldown(id, origCooldown);
                        cfg.setAnyAnimalFood(id, origFoods.isEmpty() ? AnimalType.COW.getDefaultBreedingFood() : (String)origFoods.get(0));

                        for(int i = 1; i < origFoods.size(); ++i) {
                           cfg.addAnyAnimalFood(id, (String)origFoods.get(i));
                        }

                        cfg.setGrowthEnabled(origGrowthEnabled);
                        cfg.setPerPlayerTameLimit(origPlayerLimit);
                        cfg.setPerClaimTameLimit(origClaimLimit);
                     } catch (Throwable var41) {
                     }

                  }
               }

               try {
                  cfg.setAnyAnimalBreedingEnabled(id, origBreed);
                  cfg.setAnyAnimalTamingEnabled(tameId, origWolfTame);
                  cfg.setAnyAnimalGrowthTime(id, origGrowth);
                  cfg.setAnyAnimalCooldown(id, origCooldown);
                  cfg.setAnyAnimalFood(id, origFoods.isEmpty() ? AnimalType.COW.getDefaultBreedingFood() : (String)origFoods.get(0));

                  for(int i = 1; i < origFoods.size(); ++i) {
                     cfg.addAnyAnimalFood(id, (String)origFoods.get(i));
                  }

                  cfg.setGrowthEnabled(origGrowthEnabled);
                  cfg.setPerPlayerTameLimit(origPlayerLimit);
                  cfg.setPerClaimTameLimit(origClaimLimit);
               } catch (Throwable var42) {
               }
               break label433;
            }

            try {
               cfg.setAnyAnimalBreedingEnabled(id, origBreed);
               cfg.setAnyAnimalTamingEnabled(tameId, origWolfTame);
               cfg.setAnyAnimalGrowthTime(id, origGrowth);
               cfg.setAnyAnimalCooldown(id, origCooldown);
               cfg.setAnyAnimalFood(id, origFoods.isEmpty() ? AnimalType.COW.getDefaultBreedingFood() : (String)origFoods.get(0));

               for(int i = 1; i < origFoods.size(); ++i) {
                  cfg.addAnyAnimalFood(id, (String)origFoods.get(i));
               }

               cfg.setGrowthEnabled(origGrowthEnabled);
               cfg.setPerPlayerTameLimit(origPlayerLimit);
               cfg.setPerClaimTameLimit(origClaimLimit);
            } catch (Throwable var43) {
            }
         }

         out(String.format("SELFTEST-CONFIG: breedToggle=%s tameToggle=%s growthRoundtrip=%s cooldownRoundtrip=%s", breedToggle, tameToggle, growthRt, cooldownRt));
         out(String.format("SELFTEST-FOOD: favoriteFoodList=%s", foodList));
         out(String.format("SELFTEST-GLOBAL: growthToggle=%s perPlayerLimit=%s perClaimLimit=%s presets=%s", growthToggle, playerLimit, claimLimit, presets));
         String[] checks = new String[]{breedToggle, tameToggle, growthRt, cooldownRt, foodList, growthToggle, playerLimit, claimLimit, presets};
         int passed = 0;

         for(String c : checks) {
            if ("PASS".equals(c)) {
               ++passed;
            }
         }

         return new int[]{passed, checks.length};
      }
   }

   private static boolean near(double a, double b) {
      return Math.abs(a - b) < 0.001;
   }

   private static void probeParticles() {
      try {
         DefaultAssetMap<String, ParticleSystem> sysMap = ParticleSystem.getAssetMap();
         DefaultAssetMap<String, ParticleSpawner> spnMap = ParticleSpawner.getAssetMap();
         StringBuilder sb = new StringBuilder("SELFTEST-PARTICLE:");

         for(String n : new String[]{"Hearts", "Hearts_Subtle", "TameHearts", "BreedingHearts"}) {
            boolean sys;
            try {
               sys = sysMap.getAsset(n) != null;
            } catch (Throwable var11) {
               sys = false;
            }

            boolean spn;
            try {
               spn = spnMap.getAsset(n) != null;
            } catch (Throwable var10) {
               spn = false;
            }

            sb.append(" ").append(n).append("=sys:").append(sys ? "OK" : "MISSING").append(",spawner:").append(spn ? "OK" : "MISSING");
         }

         out(sb.toString());
      } catch (Throwable t) {
         String var10000 = t.getClass().getSimpleName();
         out("SELFTEST-PARTICLE: probe-FAIL " + var10000 + " " + t.getMessage());
      }

   }

   static {
      ROLE_MATRIX = new AnimalType[]{AnimalType.COW, AnimalType.WOLF, AnimalType.FROG, AnimalType.DUCK, AnimalType.TORTOISE, AnimalType.RAT, AnimalType.EEL_MORAY, AnimalType.EMBERWULF, AnimalType.RAPTOR_CAVE, AnimalType.DRAGON_FIRE, AnimalType.SKELETON, AnimalType.GOLEM_CRYSTAL_EARTH, AnimalType.SPIRIT_EMBER, AnimalType.GOBLIN_SCRAPPER, AnimalType.TRORK_BRAWLER, AnimalType.SCARAK_LOUSE, AnimalType.KWEEBEC_SEEDLING, AnimalType.OUTLANDER_PEON, AnimalType.CRAWLER_VOID, AnimalType.HEDERA};
      CORE_TAME = EnumSet.of(AnimalType.COW, AnimalType.WOLF, AnimalType.FROG, AnimalType.DUCK);
      FAULT = System.getProperty("hytame.selftest.fault", "");
   }
}
