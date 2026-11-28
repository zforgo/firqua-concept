package io.github.zforgo.firqua.devices.events;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.assets.IpAddressAware;
import io.github.zforgo.firqua.assets.StationAware;
import io.github.zforgo.firqua.devices.Device;

@ApplicationScoped
class MonitoringStateProjector {

    @Inject
    ObjectMapper mapper;

    @Inject
    Event<DeviceMonitoringStateChanged> exported;

    @Transactional(TxType.MANDATORY)
    void handleInternalChange(@Observes DeviceStateChanged event) {
        switch (event.eventType()) {
            case CREATED,
                 UPDATED ->
                exportEvent(event.deviceId());
            case REMOVED -> exported.fire(DeviceMonitoringStateChanged.deleted(event.deviceId()));
        }
    }

    private void exportEvent(Long deviceId) {
        var device = Device.<Device<? extends Asset>> findByIdOptional(deviceId, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new EntityNotFoundException("No device found by id: %d".formatted(deviceId)));
        String ipAddress = null;
        if (device.asset instanceof IpAddressAware ip) {
            ipAddress = ip.getIpAddress().getAddress();
        }
        var state = getState(device, ipAddress);
        exported.fire(DeviceMonitoringStateChanged.changed(state, mapper)); // same tx → outbox insert

    }

    private static @NonNull DeviceMonitoringState getState(Device<?> device, String ipAddress) {
        String stationId = null;
        if (device.asset instanceof StationAware station) {
            stationId = station.getStationId();
        }
        var labels = Map.of(
                "organisation_unit", device.organisationUnit.slug,
                "asset_name", device.asset.name,
                "device_name", device.name
        );
        return new DeviceMonitoringState(
                device.id,
                device.isEffectiveEnabled(),
                device.organisationUnit.id,
                ipAddress,
                stationId,
                labels
        );
    }
}
