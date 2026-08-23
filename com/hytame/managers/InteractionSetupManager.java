package com.hytame.managers;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.components.HyTameInteractionComponent;
import com.hytame.interactions.InteractionStateCache;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.CustomAnimalConfig;
import com.hytame.models.OriginalInteractionState;
import com.hytame.util.ConfigManager;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class InteractionSetupManager {
   private final ConfigManager configManager;
   private final BreedingManager breedingManager;
   private Supplier<ComponentType<EntityStore, HyTameInteractionComponent>> hyTameInteractionTypeSupplier;
   private boolean useEntityBasedInteractions = false;
   private boolean useLegacyFeedInteraction = false;
   private boolean showAbility2HintsOnEntities = false;
   private boolean modifyInteractionHints = false;
   private static InteractionType cachedUseType = null;
   private static InteractionType cachedAbility2Type = null;
   private static boolean interactionTypesCached = false;
   private static final String FEED_INTERACTION_ID = "Root_FeedAnimal";

   public InteractionSetupManager(ConfigManager configManager, BreedingManager breedingManager) {
      this.configManager = configManager;
      this.breedingManager = breedingManager;
   }

   public void setHyTameInteractionTypeSupplier(Supplier<ComponentType<EntityStore, HyTameInteractionComponent>> supplier) {
      this.hyTameInteractionTypeSupplier = supplier;
   }

   public void setUseEntityBasedInteractions(boolean use) {
      this.useEntityBasedInteractions = use;
   }

   public void setUseLegacyFeedInteraction(boolean use) {
      this.useLegacyFeedInteraction = use;
   }

   public void setShowAbility2HintsOnEntities(boolean show) {
      this.showAbility2HintsOnEntities = show;
   }

   public void setModifyInteractionHints(boolean modify) {
      this.modifyInteractionHints = modify;
   }

   private void logVerbose(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atInfo()).log(message);
      }

   }

   private void logWarning(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)HyTamePlugin.getInstance().getLogger().atWarning()).log(message);
      }

   }

   private static void ensureInteractionTypesCached() {
      if (!interactionTypesCached) {
         for(InteractionType enumConst : (InteractionType[])InteractionType.class.getEnumConstants()) {
            String name = enumConst.toString();
            if ("Use".equals(name)) {
               cachedUseType = enumConst;
            } else if ("Ability2".equals(name)) {
               cachedAbility2Type = enumConst;
            }
         }

         interactionTypesCached = true;
      }
   }

   private ComponentType<EntityStore, HyTameInteractionComponent> getHyTameInteractionType() {
      return this.hyTameInteractionTypeSupplier != null ? (ComponentType)this.hyTameInteractionTypeSupplier.get() : null;
   }

   public void setupSingleEntity(World world, Ref<EntityStore> entityRef) {
      try {
         Store<EntityStore> store = world.getEntityStore().getStore();
         String modelAssetId = EcsReflectionUtil.getEntityModelAssetId(store, entityRef);
         if (modelAssetId == null) {
            return;
         }

         AnimalType animalType = AnimalType.fromModelAssetId(modelAssetId);
         if (animalType == null) {
            return;
         }

         this.logVerbose("Setting up animal: " + modelAssetId + " (" + String.valueOf(animalType) + ")");
         if (!this.configManager.isBreedingEnabled(animalType) && !this.configManager.isTamingEnabled(animalType)) {
            return;
         }

         boolean isBaby = AnimalType.isBabyVariant(modelAssetId);
         if (isBaby) {
            UUID babyId = UUID.nameUUIDFromBytes(entityRef.toString().getBytes());
            if (this.breedingManager.getData(babyId) == null) {
               this.breedingManager.registerBaby(babyId, animalType, entityRef);
               this.logVerbose("Registered baby for growth tracking: " + modelAssetId);
            }
         }

         if (!isBaby) {
            this.setupEntityInteractions(store, entityRef, animalType);
         }
      } catch (IllegalStateException var8) {
         if (var8.getMessage() != null && var8.getMessage().contains("Invalid entity")) {
            return;
         }

         this.logVerbose("setupSingleEntity error: " + var8.getMessage());
      } catch (Exception e) {
         this.logVerbose("setupSingleEntity error: " + e.getMessage());
      }

   }

   public void onNewAnimalDetected(Store<EntityStore> store, Ref<EntityStore> entityRef, String modelAssetId, AnimalType animalType, World world) {
      try {
         if (entityRef == null || !entityRef.isValid()) {
            return;
         }

         this.logVerbose("NewAnimalSpawnDetector: Immediate detection of " + modelAssetId);
         CustomAnimalConfig customAnimal = null;
         if (animalType == null) {
            customAnimal = this.configManager.getCustomAnimal(modelAssetId);
         }

         if (animalType != null && !this.configManager.isBreedingEnabled(animalType) && !this.configManager.isTamingEnabled(animalType)) {
            this.logVerbose("Skipping disabled animal: " + String.valueOf(animalType));
            return;
         }

         if (customAnimal != null && !customAnimal.isBreedingEnabled() && !customAnimal.isTamingEnabled()) {
            this.logVerbose("Skipping disabled custom animal: " + modelAssetId);
            return;
         }

         boolean isBaby = AnimalType.isBabyVariant(modelAssetId);
         if (isBaby && animalType != null) {
            UUID babyId = UUID.nameUUIDFromBytes(entityRef.toString().getBytes());
            if (this.breedingManager.getData(babyId) == null) {
               this.breedingManager.registerBaby(babyId, animalType, entityRef);
               this.logVerbose("Registered new baby for growth tracking: " + modelAssetId);
            }
         }

         if (!isBaby && world != null && this.useLegacyFeedInteraction) {
            world.execute(() -> {
               try {
                  if (!entityRef.isValid()) {
                     return;
                  }

                  Store<EntityStore> worldStore = world.getEntityStore().getStore();
                  if (this.useEntityBasedInteractions) {
                     if (animalType != null) {
                        this.setupEntityInteractions(worldStore, entityRef, animalType);
                        this.logVerbose("Interactions set up for new animal: " + modelAssetId);
                     } else if (customAnimal != null) {
                        this.setupCustomAnimalInteractions(worldStore, entityRef, customAnimal);
                        this.logVerbose("[CustomAnimal] Interactions set up for: " + modelAssetId);
                     }
                  } else if (this.showAbility2HintsOnEntities) {
                     String hintKey = "server.interactionHints.feed";
                     this.setupAbility2HintOnly(worldStore, entityRef, hintKey);
                     this.logVerbose("Ability2 hint set up for: " + modelAssetId);
                  }
               } catch (Exception e) {
                  this.logVerbose("Deferred interaction setup error: " + e.getMessage());
               }

            });
         }
      } catch (IllegalStateException var12) {
         if (var12.getMessage() != null && var12.getMessage().contains("Invalid entity")) {
            return;
         }

         this.logVerbose("onNewAnimalDetected error: " + var12.getMessage());
      } catch (Exception e) {
         this.logVerbose("onNewAnimalDetected error: " + e.getMessage());
      }

   }

   public void ensureInteractableComponent(Store<EntityStore> store, Ref<EntityStore> entityRef) {
      try {
         if (EntityUtil.isPlayerEntity(entityRef)) {
            return;
         }

         store.ensureAndGetComponent(entityRef, EcsReflectionUtil.INTERACTABLE_TYPE);
         this.logVerbose("[EnsureInteractable] Added Interactable component to entity");
      } catch (Exception var4) {
      }

   }

   public void setupEntityInteractions(Store<EntityStore> store, Ref<EntityStore> entityRef, AnimalType animalType) {
      if (this.useLegacyFeedInteraction) {
         try {
            if (EntityUtil.isPlayerEntity(entityRef)) {
               this.logVerbose("[SetupInteraction] Skipping player entity with animal model");
               return;
            }

            String modelAssetId = EcsReflectionUtil.getEntityModelAssetId(store, entityRef);
            if (modelAssetId != null && AnimalType.isBabyVariant(modelAssetId)) {
               this.logVerbose("[SetupInteraction] Skipping baby animal: " + modelAssetId);
               return;
            }

            Interactions interactions = (Interactions)store.getComponent(entityRef, EcsReflectionUtil.INTERACTIONS_TYPE);
            if (interactions == null) {
               this.logVerbose("[SetupInteraction] Skipping non-NPC entity (no Interactions component)");
               return;
            }

            ensureInteractionTypesCached();
            InteractionType useType = cachedUseType;
            String currentUse = interactions.getInteractionId(useType);
            ComponentType<EntityStore, HyTameInteractionComponent> hyTameType = this.getHyTameInteractionType();
            HyTameInteractionComponent origComp = hyTameType != null ? (HyTameInteractionComponent)store.getComponent(entityRef, hyTameType) : null;
            if (origComp != null && origComp.isCaptured() && "Root_FeedAnimal".equals(currentUse)) {
               return;
            }

            String currentHint = interactions.getInteractionHint();
            if (origComp != null && origComp.isCaptured()) {
               InteractionStateCache.getInstance().storeOriginalState(entityRef, origComp.getOriginalInteractionId(), origComp.getOriginalHint(), animalType);
               String var14 = String.valueOf(animalType);
               this.logVerbose("[BuiltIn] " + var14 + ": restored original from ECS: interaction=" + origComp.getOriginalInteractionId());
            } else if (currentUse == null || !currentUse.equals("Root_FeedAnimal")) {
               if (hyTameType != null) {
                  origComp = (HyTameInteractionComponent)store.ensureAndGetComponent(entityRef, hyTameType);
                  origComp.setOriginalInteractionId(currentUse);
                  origComp.setOriginalHint(currentHint);
                  origComp.setCaptured(true);
                  this.logVerbose("[BuiltIn] " + String.valueOf(animalType) + ": persisted original interaction=" + currentUse + ", hint=" + currentHint);
               }

               InteractionStateCache.getInstance().storeOriginalState(entityRef, currentUse, currentHint, animalType);
            }

            if (currentUse == null || !currentUse.equals("Root_FeedAnimal")) {
               interactions.setInteractionId(useType, "Root_FeedAnimal");
               this.logVerbose("[BuiltIn] " + String.valueOf(animalType) + ": set interaction to Root_FeedAnimal");
            }

            if (this.modifyInteractionHints) {
               String hintKey = animalType.isMountable() ? "server.interactionHints.legacyFeedOrMount" : "server.interactionHints.legacyFeed";
               interactions.setInteractionHint(hintKey);
               String var15 = String.valueOf(animalType);
               this.logVerbose("[SetupInteraction] SUCCESS for " + var15 + ": interactionId=Root_FeedAnimal, hint=" + hintKey);
            } else {
               this.logVerbose("[SetupInteraction] SUCCESS for " + String.valueOf(animalType) + ": interactionId=Root_FeedAnimal");
            }
         } catch (Exception e) {
            String var10001 = String.valueOf(animalType);
            this.logWarning("[SetupInteraction] ERROR for " + var10001 + ": " + e.getMessage());
         }

      }
   }

   public void setupCustomAnimalInteractions(Store<EntityStore> store, Ref<EntityStore> entityRef, CustomAnimalConfig customAnimal) {
      if (this.useLegacyFeedInteraction) {
         String animalName = customAnimal.getModelAssetId();
         this.logVerbose("[CustomAnimal] setupCustomAnimalInteractions CALLED for: " + animalName);

         try {
            if (EntityUtil.isPlayerEntity(entityRef)) {
               this.logVerbose("[CustomAnimal] Skipping player entity with custom animal model: " + animalName);
               return;
            }

            String actualModelId = EcsReflectionUtil.getEntityModelAssetId(store, entityRef);
            if (actualModelId != null && AnimalType.isBabyVariant(actualModelId)) {
               this.logVerbose("[CustomAnimal] Skipping baby animal: " + actualModelId);
               return;
            }

            try {
               store.ensureAndGetComponent(entityRef, EcsReflectionUtil.INTERACTABLE_TYPE);
            } catch (Exception var12) {
            }

            Interactions interactions = (Interactions)store.getComponent(entityRef, EcsReflectionUtil.INTERACTIONS_TYPE);
            if (interactions == null) {
               this.logVerbose("[CustomAnimal] Skipping non-NPC entity (no Interactions component): " + animalName);
               return;
            }

            ensureInteractionTypesCached();
            InteractionType useType = cachedUseType;
            String currentUse = interactions.getInteractionId(useType);
            ComponentType<EntityStore, HyTameInteractionComponent> hyTameType = this.getHyTameInteractionType();
            HyTameInteractionComponent origComp = hyTameType != null ? (HyTameInteractionComponent)store.getComponent(entityRef, hyTameType) : null;
            if (origComp != null && origComp.isCaptured() && "Root_FeedAnimal".equals(currentUse)) {
               return;
            }

            String currentHint = interactions.getInteractionHint();
            this.logVerbose("[CustomAnimal] " + animalName + ": currentUse='" + currentUse + "', currentHint='" + currentHint + "'");
            if (origComp != null && origComp.isCaptured()) {
               InteractionStateCache.getInstance().storeOriginalState(entityRef, origComp.getOriginalInteractionId(), origComp.getOriginalHint(), (AnimalType)null);
               this.logVerbose("[CustomAnimal] " + animalName + ": restored original from ECS");
            } else if (currentUse == null || !currentUse.equals("Root_FeedAnimal")) {
               if (hyTameType != null) {
                  origComp = (HyTameInteractionComponent)store.ensureAndGetComponent(entityRef, hyTameType);
                  origComp.setOriginalInteractionId(currentUse);
                  origComp.setOriginalHint(currentHint);
                  origComp.setCaptured(true);
                  this.logVerbose("[CustomAnimal] " + animalName + ": persisted original interaction=" + currentUse);
               }

               InteractionStateCache.getInstance().storeOriginalState(entityRef, currentUse, currentHint, (AnimalType)null);
            }

            if (currentUse == null || !currentUse.equals("Root_FeedAnimal")) {
               if (currentUse != null && currentUse.startsWith("*")) {
                  interactions.setInteractionId(useType, (String)null);
                  this.logVerbose("[CustomAnimal] " + animalName + ": cleared special interaction '" + currentUse + "' to null");
               }

               interactions.setInteractionId(useType, "Root_FeedAnimal");
               this.logVerbose("[CustomAnimal] " + animalName + ": set interaction to Root_FeedAnimal");
            }

            if (this.modifyInteractionHints) {
               interactions.setInteractionHint("server.interactionHints.legacyFeed");
            }

            this.logVerbose("[CustomAnimal] " + animalName + ": setup complete");
         } catch (Exception e) {
            this.logWarning("[CustomAnimal] " + animalName + ": setup error: " + e.getMessage());
         }

      }
   }

   public void updateAnimalInteractionState(Ref<EntityStore> entityRef, AnimalType animalType, BreedingData data) {
      if (this.useEntityBasedInteractions) {
         if (entityRef != null && entityRef.isValid()) {
            try {
               Store<EntityStore> store = entityRef.getStore();
               if (store == null) {
                  return;
               }

               boolean shouldShowFeed = true;
               if (data != null) {
                  if (data.isInLove()) {
                     shouldShowFeed = false;
                  }

                  long cooldown = this.configManager.getBreedingCooldown(animalType);
                  if (data.getCooldownRemaining(cooldown) > 0L) {
                     shouldShowFeed = false;
                  }
               }

               Interactions interactions = (Interactions)store.getComponent(entityRef, EcsReflectionUtil.INTERACTIONS_TYPE);
               if (interactions == null) {
                  return;
               }

               ensureInteractionTypesCached();
               InteractionType useType = cachedUseType;
               if (shouldShowFeed) {
                  interactions.setInteractionId(useType, "Root_FeedAnimal");
                  if (this.modifyInteractionHints) {
                     String hintKey = animalType.isMountable() ? "server.interactionHints.legacyFeedOrMount" : "server.interactionHints.legacyFeed";
                     interactions.setInteractionHint(hintKey);
                  }
               } else {
                  OriginalInteractionState original = InteractionStateCache.getInstance().getOriginalState(entityRef);
                  if (original != null) {
                     interactions.setInteractionId(useType, original.getInteractionId());
                     if (this.modifyInteractionHints) {
                        if (original.hasHint()) {
                           interactions.setInteractionHint(original.getHint());
                        } else {
                           interactions.setInteractionHint((String)null);
                        }
                     }
                  }
               }
            } catch (Exception var9) {
            }

         }
      }
   }

   public void updateTrackedAnimalStates() {
      if (this.useEntityBasedInteractions) {
         World world = Universe.get() != null ? Universe.get().getDefaultWorld() : null;
         if (world != null) {
            List<BreedingData> dataToProcess = new ArrayList();

            for(BreedingData data : this.breedingManager.getAllBreedingData()) {
               if (data.getEntityRef() != null) {
                  dataToProcess.add(data);
               }
            }

            if (!dataToProcess.isEmpty()) {
               world.execute(() -> {
                  for(BreedingData data : dataToProcess) {
                     Object refObj = data.getEntityRef();
                     AnimalType animalType = data.getAnimalType();
                     if (refObj != null && animalType != null) {
                        try {
                           Ref<EntityStore> entityRef = (Ref)refObj;
                           if (!entityRef.isValid()) {
                              data.setEntityRef((Ref)null);
                           } else {
                              this.updateAnimalInteractionState(entityRef, animalType, data);
                           }
                        } catch (Exception var7) {
                           data.setEntityRef((Ref)null);
                        }
                     }
                  }

               });
            }
         }
      }
   }

   public void setupAbility2HintOnly(Store<EntityStore> store, Ref<EntityStore> entityRef, String hintKey) {
      try {
         try {
            store.ensureAndGetComponent(entityRef, EcsReflectionUtil.INTERACTABLE_TYPE);
         } catch (Exception var6) {
         }

         Interactions interactions = (Interactions)store.getComponent(entityRef, EcsReflectionUtil.INTERACTIONS_TYPE);
         if (interactions == null) {
            this.logVerbose("[SetupInteraction] Skipping non-NPC entity (no Interactions component)");
            return;
         }

         ensureInteractionTypesCached();
         InteractionType ability2Type = cachedAbility2Type;
         if (ability2Type == null) {
            this.logVerbose("Could not find Ability2 InteractionType");
            return;
         }

         interactions.setInteractionId(ability2Type, "Root_FeedAnimal");
         if (this.modifyInteractionHints) {
            interactions.setInteractionHint(hintKey);
            this.logVerbose("Set up Ability2 hint: " + hintKey);
         } else {
            this.logVerbose("Set up Ability2 interaction (no custom hint)");
         }
      } catch (Exception e) {
         this.logVerbose("setupAbility2HintOnly error: " + e.getMessage());
      }

   }

   public void storeOriginalInteractionIdForCustom(Ref<EntityStore> entityRef, String originalId, CustomAnimalConfig customAnimal) {
      if (originalId != null && !originalId.isEmpty()) {
         InteractionStateCache.getInstance().storeOriginalState(entityRef, originalId, (String)null, (AnimalType)null);
      }

   }
}
