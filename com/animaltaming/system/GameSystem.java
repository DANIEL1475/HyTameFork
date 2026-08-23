package com.animaltaming.system;

public interface GameSystem {
   void update(SystemContext var1, float var2);

   default int priority() {
      return 0;
   }

   default String getName() {
      return this.getClass().getSimpleName();
   }

   default boolean isEnabled() {
      return true;
   }

   default void onRegister() {
   }

   default void onUnregister() {
   }
}
