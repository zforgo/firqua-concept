package io.github.zforgo.firqua.devices.events;

import java.sql.NClob;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.assets.AssetAdminService;
import io.github.zforgo.firqua.assets.MeteoSensorAssetCreateDto;
import io.github.zforgo.firqua.assets.MeteoSensorAssetDto;
import io.github.zforgo.firqua.assets.SosAssetCreateDto;
import io.github.zforgo.firqua.assets.SosAssetDto;
import io.github.zforgo.firqua.devices.DeviceService;
import io.github.zforgo.firqua.devices.MeteoSensorDevice;
import io.github.zforgo.firqua.devices.MeteoSensorDeviceCreateDto;
import io.github.zforgo.firqua.devices.MeteoSensorDeviceDto;
import io.github.zforgo.firqua.devices.SosDeviceCreateDto;
import io.github.zforgo.firqua.organisations.OrganisationUnit;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true, additionalContexts = "sample-data")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestTransaction
class MonitoringStateChangedDatabaseTest {

    private static final String outboxTable = "DEVICE_MONITORING_OUTBOX";

    //language=sql
    private static final String selectStatement = "SELECT ID,AGGREGATE_TYPE,AGGREGATE_ID,TYPE,TIMESTAMP,PAYLOAD FROM %s"
            .formatted(outboxTable);

    @Inject
    EntityManager em;

    @Inject
    AssetAdminService assetService;

    @Inject
    DeviceService deviceService;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void initTable() {
        em.createNativeQuery("DELETE FROM " + outboxTable).executeUpdate();
    }

    @Test
    @DisplayName("Outbox record stored when device created")
    void deviceCreated() {
        var ou = OrganisationUnit.<OrganisationUnit> find("slug", "OU_BAR").firstResultOptional()
                .orElseThrow(() -> new AssertionError("No OrganisationUnit found"));
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.50.51");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "FOO-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var createdAsset = assertInstanceOf(SosAssetDto.class, assetService.createAsset(sosWithIp));

        var input = new SosDeviceCreateDto();
        input.organisationId = ou.id;
        input.assetId = createdAsset.id;
        input.enabled = true;
        input.name = "SOS-CREATE-001";

        var createdDevice = deviceService.createDevice(input);
        assertEquals(1, countRows(em), "Unexpected row count in table " + outboxTable);
        var actual = restoreState(em, objectMapper);
        var expectedLabels = Map.of(
                "organisation_unit", ou.slug,
                "asset_name", createdAsset.name,
                "device_name", createdDevice.name
        );
        assertAll(
                () -> assertEquals(createdDevice.id, actual.deviceId(), "Unexpected device id"),
                () -> assertEquals("DeviceMonitoringStateChanged", actual.type()),
                () -> assertAll(
                        "state checks",
                        () -> assertEquals(createdDevice.id, actual.state().deviceId()),
                        () -> assertEquals(createdDevice.effectiveEnabled, actual.state().enabled()),
                        () -> assertEquals(ou.id, actual.state().organisationId()),
                        () -> assertEquals(createdAsset.ipAddress, actual.state().ipAddress()),
                        () -> assertEquals(createdAsset.stationId, actual.state().stationId()),
                        () -> assertThat(actual.state().labels(), containsOf(expectedLabels))
                )
        );
    }

    @Test
    @DisplayName("Outbox record stored when modified own properties of the device")
    void deviceUpdated() {
        var device = MeteoSensorDevice.<MeteoSensorDevice> find("name", "DEV-METEO-001").firstResultOptional()
                .orElseThrow(() -> new AssertionError("No Device found"));
        var changes = new MeteoSensorDeviceCreateDto();
        changes.assetId = device.asset.id;
        changes.organisationId = device.organisationUnit.id;
        changes.enabled = true;
        changes.name = "MET-CHANGED-001";

        var changedDevice = assertInstanceOf(MeteoSensorDeviceDto.class, deviceService.updateDevice(device.id, changes));
        assertEquals(1, countRows(em), "Unexpected row count in table " + outboxTable);
        var actual = restoreState(em, objectMapper);
        var expectedLabels = Map.of(
                "organisation_unit", device.organisationUnit.slug,
                "asset_name", device.asset.name,
                "device_name", changedDevice.name
        );

        assertAll(
                () -> assertEquals("DeviceMonitoringStateChanged", actual.type()),
                () -> assertEquals(changedDevice.id, actual.deviceId()),
                () -> assertAll(
                        "state checks",
                        () -> assertEquals(changedDevice.id, actual.state().deviceId()),
                        () -> assertEquals(changedDevice.effectiveEnabled, actual.state().enabled()),
                        () -> assertEquals(changedDevice.organisationUnit.id, actual.state().organisationId()),
                        () -> assertEquals(changedDevice.asset.ipAddress, actual.state().ipAddress()),
                        () -> assertEquals(changedDevice.asset.stationId, actual.state().stationId()),
                        () -> assertThat(actual.state().labels(), containsOf(expectedLabels))
                )
        );
    }

    @Test
    @DisplayName("Outbox record stored the when associated asset changed of the device")
    void relatedAssetUpdated() {
        var device = MeteoSensorDevice.<MeteoSensorDevice> find("name", "DEV-METEO-001").firstResultOptional()
                .orElseThrow(() -> new AssertionError("No Device found"));
        var assetChanges = new MeteoSensorAssetCreateDto();
        assetChanges.model = device.asset.model;
        assetChanges.vendor = device.asset.vendor;
        assetChanges.name = device.asset.name;
        assetChanges.enabled = device.asset.enabled;
        assetChanges.setIpAddress(device.asset.getIpAddress().getAddress());
        assetChanges.stationId = "CHANGED-STATION-ID";

        var changedAsset = assertInstanceOf(MeteoSensorAssetDto.class, assetService.updateAsset(device.asset.id, assetChanges));
        assertEquals(1, countRows(em), "Unexpected row count in table " + outboxTable);
        var actual = restoreState(em, objectMapper);
        var expectedLabels = Map.of(
                "organisation_unit", device.organisationUnit.slug,
                "asset_name", changedAsset.name,
                "device_name", device.name
        );

        assertAll(
                () -> assertEquals("DeviceMonitoringStateChanged", actual.type()),
                () -> assertEquals(device.id, actual.deviceId()),
                () -> assertAll(
                        "state checks",
                        () -> assertEquals(device.id, actual.state().deviceId()),
                        () -> assertEquals(device.isEffectiveEnabled(), actual.state().enabled()),
                        () -> assertEquals(device.organisationUnit.id, actual.state().organisationId()),
                        () -> assertEquals(changedAsset.ipAddress, actual.state().ipAddress()),
                        () -> assertEquals(changedAsset.stationId, actual.state().stationId()),
                        () -> assertThat(actual.state().labels(), containsOf(expectedLabels))
                )
        );
    }

    @Test
    @DisplayName("Outbox record stored the when organisation unit changed of the device")
    void organisationChanged() {
        var device = MeteoSensorDevice.<MeteoSensorDevice> find("name", "DEV-METEO-001").firstResultOptional()
                .orElseThrow(() -> new AssertionError("No Device found"));
        var ou = OrganisationUnit.<OrganisationUnit> find("slug", "OU_BAZ").firstResultOptional()
                .orElseThrow(() -> new AssertionError("No OrganisationUnit found"));

        assertNotEquals(ou.id, device.organisationUnit.id, "OrganisationUnit must changed");

        var changes = new MeteoSensorDeviceCreateDto();
        changes.assetId = device.asset.id;
        changes.organisationId = ou.id;
        changes.enabled = device.enabled;
        changes.name = device.name;

        var changedDevice = assertInstanceOf(MeteoSensorDeviceDto.class, deviceService.updateDevice(device.id, changes));
        assertEquals(1, countRows(em), "Unexpected row count in table " + outboxTable);
        var actual = restoreState(em, objectMapper);
        var expectedLabels = Map.of(
                "organisation_unit", changedDevice.organisationUnit.slug,
                "asset_name", changedDevice.asset.name,
                "device_name", changedDevice.name
        );

        assertAll(
                () -> assertEquals("DeviceMonitoringStateChanged", actual.type()),
                () -> assertEquals(changedDevice.id, actual.deviceId()),
                () -> assertAll(
                        "state checks",
                        () -> assertEquals(changedDevice.id, actual.state().deviceId()),
                        () -> assertEquals(changedDevice.effectiveEnabled, actual.state().enabled()),
                        () -> assertEquals(changedDevice.organisationUnit.id, actual.state().organisationId()),
                        () -> assertEquals(changedDevice.asset.ipAddress, actual.state().ipAddress()),
                        () -> assertEquals(changedDevice.asset.stationId, actual.state().stationId()),
                        () -> assertThat(actual.state().labels(), containsOf(expectedLabels))
                )
        );
    }

    @Test
    @DisplayName("Outbox record stored the when effectiveEnabled flag changed changed of the device")
    void activationChanged() {
        var device = MeteoSensorDevice.<MeteoSensorDevice> find("name", "DEV-METEO-001").firstResultOptional()
                .orElseThrow(() -> new AssertionError("No Device found"));
        assertAll(
                "checks before modification",
                () -> assertTrue(device.enabled),
                () -> assertTrue(device.isEffectiveEnabled()),
                () -> assertTrue(device.asset.enabled)
        );
        var assetChanges = new MeteoSensorAssetCreateDto();
        assetChanges.enabled = false;
        assetChanges.name = device.asset.name;
        assetChanges.stationId = device.asset.stationId;
        assetChanges.setIpAddress(device.asset.getIpAddress().getAddress());
        assetChanges.vendor = device.asset.vendor;
        assetChanges.model = device.asset.model;

        var changedAsset = assertInstanceOf(MeteoSensorAssetDto.class, assetService.updateAsset(device.asset.id, assetChanges));
        assertEquals(1, countRows(em), "Unexpected row count in table " + outboxTable);
        var actual = restoreState(em, objectMapper);
        var expectedLabels = Map.of(
                "organisation_unit", device.organisationUnit.slug,
                "asset_name", changedAsset.name,
                "device_name", device.name
        );

        assertAll(
                () -> assertEquals("DeviceMonitoringStateChanged", actual.type()),
                () -> assertEquals(device.id, actual.deviceId()),
                () -> assertAll(
                        "state checks",
                        () -> assertEquals(device.id, actual.state().deviceId()),
                        () -> assertFalse(actual.state().enabled()),
                        () -> assertEquals(device.organisationUnit.id, actual.state().organisationId()),
                        () -> assertEquals(changedAsset.ipAddress, actual.state().ipAddress()),
                        () -> assertEquals(changedAsset.stationId, actual.state().stationId()),
                        () -> assertThat(actual.state().labels(), containsOf(expectedLabels))
                )
        );
    }

    private record RestoredRecord(Long deviceId, String aggregateType, String type, DeviceMonitoringState state) {}

    private static Long countRows(EntityManager em) {
        return (Long) em.createNativeQuery("SELECT count(1) FROM " + outboxTable).getSingleResult();
    }

    private static RestoredRecord restoreState(EntityManager em, ObjectMapper objectMapper) {
        var res = (Object[]) em.createNativeQuery(selectStatement).getSingleResult();
        var state = assertDoesNotThrow(
                () -> objectMapper.readValue(((NClob) res[5]).getCharacterStream(), DeviceMonitoringState.class)
        );
        return new RestoredRecord(
                (Long) res[2],
                (String) res[1],
                (String) res[3],
                state
        );
    }

    private static <K, V> Matcher<Map<K, V>> containsOf(Map<K, V> expected) {
        Matcher<Iterable<? extends Map.Entry<K, V>>> entriesMatcher = both(everyItem(is(in(expected.entrySet()))))
                .and(containsInAnyOrder(expected.entrySet().toArray()));
        return new FeatureMatcher<Map<K, V>, Set<Map.Entry<K, V>>>(entriesMatcher, "a map with entries", "entries") {

            @Override
            protected Set<Map.Entry<K, V>> featureValueOf(Map<K, V> actual) {
                return actual.entrySet();
            }
        };
    }
}
