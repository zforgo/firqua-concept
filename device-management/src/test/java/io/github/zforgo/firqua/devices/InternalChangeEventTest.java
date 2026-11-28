package io.github.zforgo.firqua.devices;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.assets.AssetAdminService;
import io.github.zforgo.firqua.assets.MeteoAsset;
import io.github.zforgo.firqua.assets.MeteoSensorAssetCreateDto;
import io.github.zforgo.firqua.organisations.OrganisationUnit;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.devices.events.DeviceStateChanged.Type.CREATED;
import static io.github.zforgo.firqua.devices.events.DeviceStateChanged.Type.UPDATED;
import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true, additionalContexts = "dirty_props")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestTransaction
public class InternalChangeEventTest {

    @Inject
    AssetAdminService assetService;

    @Inject
    DeviceService deviceService;

    @Inject
    TestChangeEventObserver observer;

    @BeforeEach
    void clear() {
        observer.clear();
    }

    @Test
    @DisplayName("CREATED event fired after device creation")
    void deviceCreatedTest() {
        var asset = MeteoAsset.<MeteoAsset> find("stationId", "MET-FOO-002").firstResult();
        var input = new MeteoSensorDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = asset.id;
        input.name = "MET-CREATE-001";
        deviceService.createDevice(input);
        assertAll(
                () -> assertEquals(1, observer.getEvents().size()),
                () -> assertEquals(CREATED, observer.getEvents().getFirst().eventType())
        );
    }

    @Test
    @DisplayName("UPDATED event fired when tracked property modified of the associated asset")
    void assetChanged() {
        var device = Device.<MeteoSensorDevice> findById(1L);
        var changedAsset = new MeteoSensorAssetCreateDto();
        changedAsset.stationId = "CHANGED_STATION-001";
        changedAsset.model = device.asset.model;
        changedAsset.setIpAddress(device.asset.ipAddress.getAddress());
        changedAsset.name = device.asset.name;
        changedAsset.vendor = device.asset.vendor;
        changedAsset.enabled = device.asset.enabled;
        assetService.updateAsset(device.asset.id, changedAsset);
        assertAll(
                () -> assertEquals(1, observer.getEvents().size()),
                () -> assertEquals(UPDATED, observer.getEvents().getFirst().eventType()),
                () -> assertEquals(device.id, observer.getEvents().getFirst().deviceId())
        );
    }

    @Test
    @DisplayName("Non-tracked property change of the associated asset doesn't fire change event")
    void nonTrackedAssetChanged() {
        var device = Device.<MeteoSensorDevice> findById(1L);
        var changedAsset = new MeteoSensorAssetCreateDto();
        changedAsset.stationId = device.asset.stationId;
        changedAsset.model = device.asset.model;
        changedAsset.setIpAddress(device.asset.ipAddress.getAddress());
        changedAsset.name = "CHANGED_NAME";
        changedAsset.vendor = device.asset.vendor;
        changedAsset.enabled = device.asset.enabled;
        assetService.updateAsset(device.asset.id, changedAsset);
        assertFalse(observer.hasEvent());
    }

    @Test
    @DisplayName("UPDATED event fired when an own property changed of the device")
    void deviceChanged() {
        var device = Device.<MeteoSensorDevice> findById(1L);
        var changedDevice = new MeteoSensorDeviceCreateDto();
        changedDevice.organisationId = device.organisationUnit.id;
        changedDevice.assetId = device.asset.id;
        changedDevice.name = device.name;
        changedDevice.enabled = !device.enabled;
        deviceService.updateDevice(device.id, changedDevice);
        assertAll(
                () -> assertEquals(1, observer.getEvents().size()),
                () -> assertEquals(UPDATED, observer.getEvents().getFirst().eventType()),
                () -> assertEquals(device.id, observer.getEvents().getFirst().deviceId())
        );
    }

    @Test
    @DisplayName("UPDATED event fired the when associated asset changed of the device")
    void deviceAssociatedAssetChanged() {
        var orphanAsset = MeteoAsset.<MeteoAsset> find("stationId", "MET-FOO-002").firstResult();

        var device = Device.<MeteoSensorDevice> findById(1L);
        var changedDevice = new MeteoSensorDeviceCreateDto();
        changedDevice.organisationId = device.organisationUnit.id;
        changedDevice.assetId = orphanAsset.id;
        changedDevice.name = device.name;
        changedDevice.enabled = device.enabled;
        deviceService.updateDevice(device.id, changedDevice);
        assertAll(
                () -> assertEquals(1, observer.getEvents().size()),
                () -> assertEquals(UPDATED, observer.getEvents().getFirst().eventType()),
                () -> assertEquals(device.id, observer.getEvents().getFirst().deviceId())
        );
    }

    @Test
    @DisplayName("UPDATED event fired the when associated organization unit changed of the device")
    void deviceAssociatedOUChanged() {
        var changedOU = OrganisationUnit.<OrganisationUnit> find("slug", "OU_BAR").firstResult();

        var device = Device.<MeteoSensorDevice> findById(1L);
        var changedDevice = new MeteoSensorDeviceCreateDto();
        changedDevice.organisationId = changedOU.id;
        changedDevice.assetId = device.asset.id;
        changedDevice.name = device.name;
        changedDevice.enabled = device.enabled;
        deviceService.updateDevice(device.id, changedDevice);
        assertAll(
                () -> assertEquals(1, observer.getEvents().size()),
                () -> assertEquals(UPDATED, observer.getEvents().getFirst().eventType()),
                () -> assertEquals(device.id, observer.getEvents().getFirst().deviceId())
        );
    }
}
