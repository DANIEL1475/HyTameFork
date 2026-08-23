package com.animaltaming.core.service;

import com.animaltaming.system.SystemContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CachedPlayerLookupService implements PlayerLookupService {
   private Map<UUID, Long> uuidToEntityId = new HashMap();
   private Map<Long, UUID> entityIdToUuid = new HashMap();

   public void refreshCache(SystemContext context) {
      this.uuidToEntityId = new HashMap();
      this.entityIdToUuid = new HashMap();

      for(SystemContext.PlayerInfo player : context.getAllPlayers()) {
         this.uuidToEntityId.put(player.uuid(), player.entityId());
         this.entityIdToUuid.put(player.entityId(), player.uuid());
      }

   }

   public Optional<Long> getEntityId(UUID playerUUID) {
      return playerUUID == null ? Optional.empty() : Optional.ofNullable((Long)this.uuidToEntityId.get(playerUUID));
   }

   public Optional<UUID> getPlayerUUID(long entityId) {
      return Optional.ofNullable((UUID)this.entityIdToUuid.get(entityId));
   }

   public boolean isOnline(UUID playerUUID) {
      return playerUUID != null && this.uuidToEntityId.containsKey(playerUUID);
   }

   public boolean isPlayer(long entityId) {
      return this.entityIdToUuid.containsKey(entityId);
   }

   public int getOnlineCount() {
      return this.uuidToEntityId.size();
   }
}
