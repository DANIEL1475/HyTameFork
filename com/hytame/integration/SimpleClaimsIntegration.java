package com.hytame.integration;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.chunk.ChunkInfo;
import com.hytame.managers.TamingManager;
import com.hytame.models.TamedAnimalData;
import java.util.UUID;

public class SimpleClaimsIntegration {
   public UUID getPartyIdAtPosition(String dimension, int blockX, int blockZ) {
      try {
         ClaimManager cm = ClaimManager.getInstance();
         if (cm == null) {
            return null;
         } else {
            ChunkInfo chunk = cm.getChunkRawCoords(dimension, blockX, blockZ);
            return chunk != null ? chunk.getPartyOwner() : null;
         }
      } catch (Exception var6) {
         return null;
      }
   }

   public int countTamedAnimalsInParty(UUID partyId, String dimension, TamingManager tamingManager) {
      try {
         ClaimManager cm = ClaimManager.getInstance();
         if (cm == null) {
            return 0;
         } else {
            int count = 0;

            for(TamedAnimalData animal : tamingManager.getAllTamedAnimals()) {
               if (!animal.isDead() && !animal.isCaptured()) {
                  String animalWorld = animal.getWorldId();
                  if (animalWorld != null && animalWorld.equals(dimension)) {
                     ChunkInfo chunk = cm.getChunkRawCoords(dimension, (int)animal.getLastX(), (int)animal.getLastZ());
                     if (chunk != null && partyId.equals(chunk.getPartyOwner())) {
                        ++count;
                     }
                  }
               }
            }

            return count;
         }
      } catch (Exception var10) {
         return 0;
      }
   }
}
