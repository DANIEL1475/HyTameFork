package com.animaltaming.core.service;

import com.animaltaming.system.SystemContext;
import java.util.Optional;
import java.util.UUID;

public interface PlayerLookupService {
   void refreshCache(SystemContext var1);

   Optional<Long> getEntityId(UUID var1);

   Optional<UUID> getPlayerUUID(long var1);

   boolean isOnline(UUID var1);

   boolean isPlayer(long var1);

   int getOnlineCount();
}
