package com.github.sibmaks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class InMemoryEventBus {
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    public void publish(String topic, Object event) {
        var consumers = Optional.ofNullable(subscribers.get(topic))
                .orElseGet(Collections::emptyList);
        for (var consumer : consumers) {
            consumer.accept(event);
        }
    }

    public void subscribe(String topic, Consumer<Object> handler) {
        var consumers = subscribers.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>());
        consumers.add(handler);
    }

    public void unsubscribe(String topic, Consumer<Object> handler) {
        var consumers = subscribers.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>());
        consumers.remove(handler);
    }
}
