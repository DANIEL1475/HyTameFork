package com.hytame.coop;

import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderField;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import com.hytame.HyTamePlugin;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CoopCodecExtender {
   private static final Map<CapturedNPCMetadata, HyTameCoopData> companionMap = Collections.synchronizedMap(new WeakHashMap());
   private static volatile boolean extended = false;
   private static Field residentsField;

   public static void extend() {
      if (!extended) {
         try {
            BuilderCodec<CapturedNPCMetadata> codec = CapturedNPCMetadata.CODEC;
            BuilderCodec.Builder<CapturedNPCMetadata> tempBuilder = BuilderCodec.builder(CapturedNPCMetadata.class, codec.getSupplier());
            appendField(tempBuilder, "HyTame.Id", Codec.STRING, (meta, v) -> ((HyTameCoopData)companionMap.computeIfAbsent(meta, (k) -> new HyTameCoopData())).setHytameIdStr(v), (meta) -> (String)Optional.ofNullable((HyTameCoopData)companionMap.get(meta)).map(HyTameCoopData::getHytameIdStr).orElse((Object)null));
            appendField(tempBuilder, "HyTame.OwnerUuid", Codec.STRING, (meta, v) -> ((HyTameCoopData)companionMap.computeIfAbsent(meta, (k) -> new HyTameCoopData())).setOwnerUuidStr(v), (meta) -> (String)Optional.ofNullable((HyTameCoopData)companionMap.get(meta)).map(HyTameCoopData::getOwnerUuidStr).orElse((Object)null));
            appendField(tempBuilder, "HyTame.OwnerName", Codec.STRING, (meta, v) -> ((HyTameCoopData)companionMap.computeIfAbsent(meta, (k) -> new HyTameCoopData())).setOwnerName(v), (meta) -> (String)Optional.ofNullable((HyTameCoopData)companionMap.get(meta)).map(HyTameCoopData::getOwnerName).orElse((Object)null));
            appendField(tempBuilder, "HyTame.CustomName", Codec.STRING, (meta, v) -> ((HyTameCoopData)companionMap.computeIfAbsent(meta, (k) -> new HyTameCoopData())).setCustomName(v), (meta) -> (String)Optional.ofNullable((HyTameCoopData)companionMap.get(meta)).map(HyTameCoopData::getCustomName).orElse((Object)null));
            appendField(tempBuilder, "HyTame.AnimalType", Codec.STRING, (meta, v) -> ((HyTameCoopData)companionMap.computeIfAbsent(meta, (k) -> new HyTameCoopData())).setAnimalType(v), (meta) -> (String)Optional.ofNullable((HyTameCoopData)companionMap.get(meta)).map(HyTameCoopData::getAnimalType).orElse((Object)null));
            appendField(tempBuilder, "HyTame.Tamed", Codec.BOOLEAN, (meta, v) -> ((HyTameCoopData)companionMap.computeIfAbsent(meta, (k) -> new HyTameCoopData())).setTamed(Boolean.TRUE.equals(v)), (meta) -> (Boolean)Optional.ofNullable((HyTameCoopData)companionMap.get(meta)).filter(HyTameCoopData::isTamed).map((d) -> true).orElse((Object)null));
            BuilderCodec<CapturedNPCMetadata> tempCodec = tempBuilder.build();
            Field entriesField = BuilderCodec.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            Map<String, List<BuilderField<CapturedNPCMetadata, ?>>> realEntries = (Map)entriesField.get(codec);

            for(Map.Entry<String, List<BuilderField<CapturedNPCMetadata, ?>>> entry : tempCodec.getEntries().entrySet()) {
               realEntries.put((String)entry.getKey(), new ArrayList((Collection)entry.getValue()));
            }

            extended = true;
            System.out.println("[COOP-CODEC] CapturedNPCMetadata.CODEC extended with 6 HyTame fields (entries now has " + realEntries.size() + " keys)");
            log("CapturedNPCMetadata.CODEC extended with 6 HyTame fields");
         } catch (Exception var7) {
            System.out.println("[COOP-CODEC] FAILED to extend CODEC: " + var7.getMessage());
            HyTamePlugin plugin = HyTamePlugin.getInstance();
            if (plugin != null) {
               ((HytaleLogger.Api)plugin.getLogger().atWarning()).log("[CoopCodecExtender] Failed to extend CODEC: " + var7.getMessage());
            }

            var7.printStackTrace();
         }

      }
   }

   private static <FT> void appendField(BuilderCodec.Builder<CapturedNPCMetadata> builder, String key, Codec<FT> codecType, BiConsumer<CapturedNPCMetadata, FT> setter, Function<CapturedNPCMetadata, FT> getter) {
      builder.append(new KeyedCodec(key, codecType), setter, getter).add();
   }

   public static void setHyTameData(CapturedNPCMetadata metadata, HyTameCoopData data) {
      if (metadata != null && data != null) {
         companionMap.put(metadata, data);
         log("setHyTameData: " + String.valueOf(data));
      }
   }

   public static HyTameCoopData getHyTameData(CapturedNPCMetadata metadata) {
      return metadata == null ? null : (HyTameCoopData)companionMap.get(metadata);
   }

   public static boolean hasHyTameData(CapturedNPCMetadata metadata) {
      return metadata != null && companionMap.containsKey(metadata);
   }

   public static List<CoopBlock.CoopResident> getResidents(CoopBlock coopBlock) {
      if (coopBlock == null) {
         return null;
      } else {
         try {
            if (residentsField == null) {
               residentsField = CoopBlock.class.getDeclaredField("residents");
               residentsField.setAccessible(true);
            }

            return (List)residentsField.get(coopBlock);
         } catch (Exception e) {
            log("Failed to access CoopBlock.residents: " + e.getMessage());
            return null;
         }
      }
   }

   private static void log(String message) {
      if (HyTamePlugin.isVerboseLogging()) {
         HyTamePlugin plugin = HyTamePlugin.getInstance();
         if (plugin != null) {
            ((HytaleLogger.Api)plugin.getLogger().atInfo()).log("[CoopCodecExtender] " + message);
         }

      }
   }
}
