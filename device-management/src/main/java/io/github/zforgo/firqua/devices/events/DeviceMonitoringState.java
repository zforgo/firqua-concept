package io.github.zforgo.firqua.devices.events;

import java.util.Map;

public record DeviceMonitoringState(Long deviceId, boolean enabled, Long organisationId,
        String ipAddress, String stationId,
        Map<String, String> labels) {}
