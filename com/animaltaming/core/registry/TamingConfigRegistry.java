package com.animaltaming.core.registry;

import com.animaltaming.api.model.TamingConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TamingConfigRegistry {
   private final Map<String, TamingConfig> configs = new HashMap();

   public void register(TamingConfig config) {
      Objects.requireNonNull(config, "config is required");
      if (this.configs.containsKey(config.speciesId())) {
         throw new IllegalStateException("Species already registered: " + config.speciesId());
      } else {
         this.configs.put(config.speciesId(), config);
      }
   }

   public void registerAll(Collection<TamingConfig> configList) {
      for(TamingConfig config : configList) {
         this.register(config);
      }

   }

   public Optional<TamingConfig> get(String speciesId) {
      return Optional.ofNullable((TamingConfig)this.configs.get(speciesId));
   }

   public boolean contains(String speciesId) {
      return this.configs.containsKey(speciesId);
   }

   public Set<String> getAllSpeciesIds() {
      return Collections.unmodifiableSet(this.configs.keySet());
   }

   public Collection<TamingConfig> getAllConfigs() {
      return Collections.unmodifiableCollection(this.configs.values());
   }

   public int size() {
      return this.configs.size();
   }

   public void clear() {
      this.configs.clear();
   }
}
