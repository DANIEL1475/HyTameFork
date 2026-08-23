package com.animaltaming.api;

import com.animaltaming.api.model.TamedAnimal;
import com.animaltaming.api.model.TamingProgress;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TamingService {
   Optional<TamingProgress> startCalming(long var1, UUID var3, String var4, UUID var5);

   Optional<TamingProgress> feed(long var1, UUID var3, String var4);

   Optional<TamedAnimal> completeTaming(long var1, String var3, double var4, double var6, double var8);

   boolean toggleBehaviorMode(UUID var1, UUID var2, double var3, double var5, double var7);

   Optional<TamedAnimal> getTamedAnimal(UUID var1);

   Set<TamedAnimal> getAnimalsOwnedBy(UUID var1);

   Optional<TamingProgress> getTamingProgress(long var1);

   void removeTamingProgress(long var1);

   boolean releaseTamedAnimal(UUID var1, UUID var2);

   void updateTamedAnimal(TamedAnimal var1);
}
