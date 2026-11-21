package io.github.zforgo.firqua.devices;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.github.zforgo.firqua.devices.events.DeviceStateChanged;

@ApplicationScoped
public class TestChangeEventObserver {

    private final List<DeviceStateChanged> events = new ArrayList<>();

    void observer(@Observes DeviceStateChanged event) {
        events.add(event);
    }

    void clear() {
        events.clear();
    }

    public List<DeviceStateChanged> getEvents() {
        return events;
    }

    boolean hasEvent() {
        return !events.isEmpty();
    }
}
