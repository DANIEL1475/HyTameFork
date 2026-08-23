package com.animaltaming.util;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
   private final Map<Class<?>, List<HandlerEntry<?>>> handlers = new ConcurrentHashMap();

   public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
      this.subscribe(eventType, handler, 0);
   }

   public <T> void subscribe(Class<T> eventType, Consumer<T> handler, int priority) {
      Objects.requireNonNull(eventType, "eventType is required");
      Objects.requireNonNull(handler, "handler is required");
      List<HandlerEntry<?>> list = (List)this.handlers.computeIfAbsent(eventType, (k) -> new CopyOnWriteArrayList());
      list.add(new HandlerEntry(handler, priority));
      list.sort(Comparator.comparingInt((e) -> e.priority));
   }

   public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
      List<HandlerEntry<?>> list = (List)this.handlers.get(eventType);
      if (list != null) {
         list.removeIf((entry) -> entry.handler.equals(handler));
      }

   }

   public <T> void publish(T event) {
      Objects.requireNonNull(event, "event is required");
      List<HandlerEntry<?>> list = (List)this.handlers.get(event.getClass());
      if (list != null && !list.isEmpty()) {
         for(HandlerEntry<?> entry : list) {
            try {
               entry.handler.accept(event);
            } catch (RuntimeException e) {
               PrintStream var10000 = System.err;
               String var10001 = event.getClass().getSimpleName();
               var10000.println("[EventBus] Handler exception for " + var10001 + ": " + e.getMessage());
               e.printStackTrace();
            }
         }

      }
   }

   public boolean hasHandlers(Class<?> eventType) {
      List<HandlerEntry<?>> list = (List)this.handlers.get(eventType);
      return list != null && !list.isEmpty();
   }

   public int getHandlerCount(Class<?> eventType) {
      List<HandlerEntry<?>> list = (List)this.handlers.get(eventType);
      return list == null ? 0 : list.size();
   }

   public void clear() {
      this.handlers.clear();
   }

   public void clear(Class<?> eventType) {
      this.handlers.remove(eventType);
   }

   private static record HandlerEntry<T>(Consumer<T> handler, int priority) {
   }
}
