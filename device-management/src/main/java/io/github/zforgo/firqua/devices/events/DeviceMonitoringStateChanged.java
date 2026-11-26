package io.github.zforgo.firqua.devices.events;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.outbox.quarkus.ExportedEvent;

public class DeviceMonitoringStateChanged implements ExportedEvent<Long, JsonNode> {

    public static final String AGGREGATE_TYPE = "monitoring-state";

    private final Long deviceId;
    private final String type;
    private final JsonNode payload;
    private final Instant timestamp = Instant.now();

    private DeviceMonitoringStateChanged(Long deviceId, String type, JsonNode payload) {
        this.deviceId = deviceId;
        this.type = type;
        this.payload = payload;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public Long getAggregateId() {
        return deviceId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public JsonNode getPayload() {
        return payload;
    }

    public static DeviceMonitoringStateChanged changed(DeviceMonitoringState state, ObjectMapper mapper) {
        return new DeviceMonitoringStateChanged(
                state.deviceId(), "DeviceMonitoringStateChanged", mapper.valueToTree(state)
        );
    }

    public static DeviceMonitoringStateChanged deleted(Long deviceId) {
        return new DeviceMonitoringStateChanged(deviceId, "DeviceMonitoringStateDeleted", null);
    }
}
