package com.hytame.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytame.HyTamePlugin;
import com.hytame.managers.BreedingManager;
import com.hytame.managers.TamingManager;
import com.hytame.models.AnimalType;
import com.hytame.models.BreedingData;
import com.hytame.models.GrowthStage;
import com.hytame.models.TamedAnimalData;
import com.hytame.util.AnimalNameGenerator;
import com.hytame.util.EcsReflectionUtil;
import com.hytame.util.EntityUtil;
import com.hytame.util.NameplateUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector3d;

public class NametagUIPage extends InteractiveCustomUIPage<NametagEventData> {
   private final Ref<EntityStore> targetAnimalRef;
   private final UUID playerUuid;
   private final String animalType;
   private final String existingName;

   public NametagUIPage(PlayerRef playerRef, Ref<EntityStore> targetAnimalRef, UUID playerUuid, String animalType) {
      this(playerRef, targetAnimalRef, playerUuid, animalType, (String)null);
   }

   public NametagUIPage(PlayerRef playerRef, Ref<EntityStore> targetAnimalRef, UUID playerUuid, String animalType, String existingName) {
      super(playerRef, CustomPageLifetime.CanDismiss, NametagUIPage.NametagEventData.CODEC);
      this.targetAnimalRef = targetAnimalRef;
      this.playerUuid = playerUuid;
      this.animalType = animalType;
      this.existingName = existingName;
   }

   public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
      cmd.append("Pages/NametagPage.ui");
      String var10000 = this.animalType != null ? this.formatAnimalType(this.animalType) : "Animal";
      String title = "Name Your " + var10000;
      cmd.set("#animalTypeLabel.Text", title);
      if (this.existingName != null && !this.existingName.isEmpty() && !this.existingName.equalsIgnoreCase("_UNDEFINED")) {
         cmd.set("#subtitleLabel.Text", "Current name: " + this.existingName);
         cmd.set("#nameInput.Value", this.existingName);
      } else {
         AnimalType type = AnimalType.fromModelAssetId(this.animalType);
         List<String> suggestedNames = AnimalNameGenerator.getSuggestedNames(type);
         if (!suggestedNames.isEmpty()) {
            cmd.set("#nameInput.Value", (String)suggestedNames.get(0));
         }
      }

      events.addEventBinding(CustomUIEventBindingType.Activating, "#confirmButton", (new EventData()).append("@animalName", "#nameInput.Value"));
      events.addEventBinding(CustomUIEventBindingType.Activating, "#cancelButton", new EventData());
   }

   private String formatAnimalType(String rawType) {
      if (rawType == null) {
         return "Animal";
      } else {
         String formatted = rawType.replace("_Calf", "").replace("_Piglet", "").replace("_Chick", "").replace("_Lamb", "").replace("_Foal", "").replace("_", " ");
         if (!formatted.isEmpty()) {
            char var10000 = Character.toUpperCase(formatted.charAt(0));
            return var10000 + formatted.substring(1).toLowerCase();
         } else {
            return formatted;
         }
      }
   }

   public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, NametagEventData data) {
      try {
         Player player = (Player)store.getComponent(ref, EcsReflectionUtil.PLAYER_TYPE);
         if (data.animalName != null && !data.animalName.trim().isEmpty() && !data.animalName.equalsIgnoreCase("_UNDEFINED")) {
            String name = AnimalNameGenerator.validateName(data.animalName);
            if (name == null) {
               if (player != null) {
                  player.getPlayerRef().sendMessage(Message.raw("Invalid name. Please try again.").color("#FF5555"));
               }

               return;
            }

            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null) {
               TamingManager tamingManager = plugin.getTamingManager();
               if (tamingManager != null) {
                  UUID animalUuid = this.getAnimalUuidFromRef(this.targetAnimalRef);
                  if (animalUuid != null) {
                     AnimalType type = AnimalType.fromModelAssetId(this.animalType);
                     TamedAnimalData existingData = tamingManager.getTamedData(animalUuid);
                     boolean isRename = existingData != null;
                     boolean success;
                     if (isRename) {
                        success = tamingManager.renameAnimal(animalUuid, this.playerUuid, name);
                     } else {
                        double px = (double)0.0F;
                        double py = (double)0.0F;
                        double pz = (double)0.0F;

                        try {
                           Vector3d pos = EntityUtil.getPositionFromRef(this.targetAnimalRef);
                           if (pos != null) {
                              px = pos.x();
                              py = pos.y();
                              pz = pos.z();
                           }
                        } catch (Exception var26) {
                        }

                        GrowthStage existingGrowthStage = null;
                        long existingBirthTime = 0L;
                        BreedingManager breedingManager = plugin.getBreedingManager();
                        if (breedingManager != null) {
                           this.log("Looking up BreedingData for UUID: " + String.valueOf(animalUuid));
                           BreedingData existingBreedingData = breedingManager.getData(animalUuid);
                           if (existingBreedingData != null) {
                              this.log("BreedingData lookup result: found (growthStage=" + String.valueOf(existingBreedingData.getGrowthStage()) + ")");
                           } else {
                              this.log("BreedingData lookup result: NOT FOUND by UUID");
                              this.log(breedingManager.getRegisteredBabiesDebug());
                              existingBreedingData = breedingManager.findBabyByRef(this.targetAnimalRef);
                              if (existingBreedingData != null) {
                                 this.log("Found baby via ref-based lookup (UUID mismatch resolved)");
                                 this.log("Resolved growthStage=" + String.valueOf(existingBreedingData.getGrowthStage()));
                              }
                           }

                           if (existingBreedingData != null && existingBreedingData.getGrowthStage() != GrowthStage.ADULT) {
                              existingGrowthStage = existingBreedingData.getGrowthStage();
                              existingBirthTime = existingBreedingData.getBirthTime();
                              this.log("Preserving existing growth stage: " + String.valueOf(existingGrowthStage));
                           }
                        }

                        if (existingGrowthStage == null && this.animalType != null && (this.animalType.contains("_Calf") || this.animalType.contains("_Piglet") || this.animalType.contains("_Chick") || this.animalType.contains("_Lamb") || this.animalType.contains("_Foal") || this.animalType.contains("_Bunny"))) {
                           existingGrowthStage = GrowthStage.BABY;
                           existingBirthTime = System.currentTimeMillis();
                           this.log("Detected baby from model ID: " + this.animalType);
                        }

                        GrowthStage stageToUse = existingGrowthStage != null ? existingGrowthStage : GrowthStage.ADULT;
                        String worldName = this.getWorldNameFromRef(this.targetAnimalRef);
                        TamedAnimalData tamedData = tamingManager.tameAnimal(animalUuid, this.playerUuid, name, type, this.targetAnimalRef, px, py, pz, stageToUse, worldName);
                        success = tamedData != null;
                        if (success && tamedData != null && existingGrowthStage != null && existingBirthTime > 0L) {
                           tamedData.setBirthTime(existingBirthTime);
                           tamingManager.notifyDataChanged();
                           this.log("Tamed baby with growth stage: " + String.valueOf(stageToUse));
                        }
                     }

                     if (success) {
                        NameplateUtil.setEntityNameplate(this.targetAnimalRef, name);
                        if (player != null) {
                           if (isRename) {
                              player.getPlayerRef().sendMessage(Message.raw("Renamed to " + name + "!").color("#55FF55"));
                           } else {
                              player.getPlayerRef().sendMessage(Message.raw(name + " is now yours!").color("#55FF55"));
                           }
                        }

                        this.log((isRename ? "Renamed" : "Tamed") + " " + this.animalType + " as '" + name + "' for player " + String.valueOf(this.playerUuid));
                     } else if (player != null) {
                        player.getPlayerRef().sendMessage(Message.raw("Failed to tame the animal.").color("#FF5555"));
                     }
                  }
               }
            }
         } else {
            this.log("Nametag UI cancelled");
         }

         if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
         }
      } catch (Exception e) {
         this.log("Error in handleDataEvent: " + e.getMessage());
      }

   }

   private UUID getAnimalUuidFromRef(Ref<EntityStore> animalRef) {
      return animalRef == null ? null : EcsReflectionUtil.getUuidFromRef(animalRef);
   }

   private void log(String message) {
      HyTamePlugin plugin = HyTamePlugin.getInstance();
      if (plugin != null && HyTamePlugin.isVerboseLogging()) {
         ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[NametagUI] " + message);
      }

   }

   public Ref<EntityStore> getTargetAnimalRef() {
      return this.targetAnimalRef;
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public String getAnimalType() {
      return this.animalType;
   }

   public String getExistingName() {
      return this.existingName;
   }

   private String getWorldNameFromRef(Ref<EntityStore> ref) {
      if (ref == null) {
         return null;
      } else {
         try {
            Store<EntityStore> entityStore = ref.getStore();
            if (entityStore == null) {
               return null;
            }

            for(Map.Entry<String, World> entry : Universe.get().getWorlds().entrySet()) {
               World world = (World)entry.getValue();
               if (world != null) {
                  try {
                     Store<EntityStore> worldStore = world.getEntityStore().getStore();
                     if (worldStore == entityStore) {
                        return (String)entry.getKey();
                     }
                  } catch (Exception var7) {
                  }
               }
            }
         } catch (Exception var8) {
         }

         return null;
      }
   }

   public static class NametagEventData {
      public String animalName;
      public static final BuilderCodec<NametagEventData> CODEC = ((BuilderCodec.Builder)BuilderCodec.builder(NametagEventData.class, NametagEventData::new).append(new KeyedCodec("@animalName", new StringCodec()), (obj, val) -> obj.animalName = val, (obj) -> obj.animalName).add()).build();
   }
}
