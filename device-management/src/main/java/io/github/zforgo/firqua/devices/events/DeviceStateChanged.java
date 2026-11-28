package io.github.zforgo.firqua.devices.events;

public record DeviceStateChanged(Long deviceId, Type eventType) {

    public enum Type {
        CREATED,
        UPDATED,
        REMOVED
    }

    public static DeviceStateChanged created(Long deviceId) {
        return new DeviceStateChanged(deviceId, Type.CREATED);
    }

    public static DeviceStateChanged updated(Long deviceId) {
        return new DeviceStateChanged(deviceId, Type.UPDATED);
    }

    public static DeviceStateChanged removed(Long deviceId) {
        return new DeviceStateChanged(deviceId, Type.REMOVED);
    }
}
