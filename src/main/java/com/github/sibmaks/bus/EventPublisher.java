package com.github.sibmaks.bus;

public interface EventPublisher {

    void publish(String topic, Object event);

}
