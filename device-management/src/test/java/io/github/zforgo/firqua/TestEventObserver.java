package io.github.zforgo.firqua;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Observes;

public abstract class TestEventObserver<T> {

    private final List<T> events = new ArrayList<>();

    void observe(@Observes T event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }

    public List<T> getEvents() {
        return events;
    }

    public boolean hasEvent() {
        return !events.isEmpty();
    }
}
