package io.github.zforgo.firqua.devices;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.assets.AssetAdminService;
import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.assets.MeteoSensorAssetCreateDto;
import io.github.zforgo.firqua.assets.MeteoSensorAssetDto;
import io.github.zforgo.firqua.assets.SosAssetCreateDto;
import io.github.zforgo.firqua.assets.SosAssetDto;
import io.github.zforgo.firqua.common.IncompatibleAssetTypeException;
import io.github.zforgo.firqua.common.NonUniqueException;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeviceServiceTest {

    @Inject
    AssetAdminService assetAdminService;

    @Inject
    DeviceService deviceService;

    private AssetDto sosAsset;
    private AssetDto otherSosAsset;
    private AssetDto sosAssetWoIp;
    private AssetDto metAsset;
    private AssetDto metAssetWoIp;

    @BeforeAll
    void init() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.10.1");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "SOS-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";
        sosAsset = assetAdminService.createAsset(sosWithIp);
        assertNotNull(sosAsset.id);

        var otherSos = new SosAssetCreateDto();
        otherSos.setIpAddress("192.168.10.2");
        otherSos.stationId = "SOS-TEST-003";
        otherSos.name = "SOS-003";
        otherSos.serviceProvider = "AT&T";
        otherSos.vendor = "J&R Technology";
        otherSos.model = "JR321-SC";
        otherSosAsset = assetAdminService.createAsset(otherSos);
        assertNotNull(otherSosAsset.id);

        var sosWoIp = new SosAssetCreateDto();
        sosWoIp.stationId = "SOS-TEST-002";
        sosWoIp.name = "SOS-002";
        sosWoIp.serviceProvider = "AT&T";
        sosWoIp.vendor = "J&R Technology";
        sosWoIp.model = "JR321-SC";
        sosAssetWoIp = assetAdminService.createAsset(sosWoIp);
        assertNotNull(sosAssetWoIp.id);

        var metWithIp = new MeteoSensorAssetCreateDto();
        metWithIp.stationId = "MET-TEST-001";
        metWithIp.name = "MET-001";
        metWithIp.vendor = "Lufft";
        metWithIp.model = "WS500";
        metWithIp.setIpAddress("192.168.20.1");
        metAsset = assetAdminService.createAsset(metWithIp);
        assertNotNull(metAsset.id);

        var metWoIp = new MeteoSensorAssetCreateDto();
        metWoIp.stationId = "MET-TEST-002";
        metWoIp.name = "MET-002";
        metWoIp.vendor = "Lufft";
        metWoIp.model = "WS500";
        metAssetWoIp = assetAdminService.createAsset(metWoIp);
        assertNotNull(metAssetWoIp.id);
    }

    @Transactional
    @BeforeEach
    void beforeEach() {
        Device.deleteAll();
    }

    @Test
    @DisplayName("SOS device created successfully")
    void sosDeviceCreated() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-CREATE-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        var dto = assertInstanceOf(SosDeviceDto.class, result);
        var assetDto = assertInstanceOf(SosAssetDto.class, dto.asset);
        assertEquals(sosAsset.id, assetDto.id);
    }

    @Test
    @DisplayName("MET device created successfully")
    void meteoDeviceCreated() {
        var input = new MeteoSensorDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = metAsset.id;
        input.name = "MET-CREATE-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        var dto = assertInstanceOf(MeteoSensorDeviceDto.class, result);
        var assetDto = assertInstanceOf(MeteoSensorAssetDto.class, dto.asset);
        assertEquals(metAsset.id, assetDto.id);
    }

    @Test
    @DisplayName("Creation rejected when the asset type does not match the device type")
    void incompatibleTypeCreate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = metAsset.id;
        input.name = "SOS-CREATE-INCOMPATIBLE-001";
        assertThrows(IncompatibleAssetTypeException.class, () -> deviceService.createDevice(input));
    }

    @Test
    @DisplayName("Creation rejected when the name is already taken")
    void duplicatedNameCreate() {
        var sos = new SosDeviceCreateDto();
        sos.organisationId = 1L;
        sos.assetId = sosAsset.id;
        sos.name = "SOS-CREATE-MULTI-001";
        deviceService.createDevice(sos);

        var met = new MeteoSensorDeviceCreateDto();
        met.organisationId = 1L;
        met.assetId = metAsset.id;
        met.name = sos.name;
        var ex = assertThrows(NonUniqueException.class, () -> deviceService.createDevice(met));
        assertAll(
                "Exception parameters",
                () -> assertEquals("name", ex.getKey()),
                () -> assertEquals(sos.name, ex.getValue())
        );
    }

    @Test
    @DisplayName("Creation rejected when the asset is already assigned to another device")
    void multipleAssignmentCreate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-CREATE-MULTI-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        var dto = assertInstanceOf(SosDeviceDto.class, result);
        var assetDto = assertInstanceOf(SosAssetDto.class, dto.asset);
        assertEquals(sosAsset.id, assetDto.id);

        input.organisationId = 2L;
        input.name = "SOS-TEST-002";
        var ex = assertThrows(NonUniqueException.class, () -> deviceService.createDevice(input));
        assertAll(
                "Exception parameters",
                () -> assertEquals("assetId", ex.getKey()),
                () -> assertEquals(sosAsset.id, ex.getValue())
        );
    }

    @Test
    @DisplayName("Name and asset updated successfully")
    void validUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-UPDATE-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        assertNotNull(result.id);

        input.name = "SOS-UPDATE-002";
        input.assetId = otherSosAsset.id;
        var modified = deviceService.updateDevice(result.id, input);
        assertNotNull(modified);
        assertEquals(result.id, modified.id);
        assertEquals(otherSosAsset.id, modified.asset.id);
    }

    @Test
    @DisplayName("Update rejected when the modified asset type does not match the device type")
    void incompatibleUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-UPDATE-INCOMPATIBLE-002";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        assertNotNull(result.id);

        input.assetId = metAsset.id;
        assertThrows(IncompatibleAssetTypeException.class, () -> deviceService.updateDevice(result.id, input));
    }

    @Test
    @DisplayName("Update rejected when the name is already taken")
    void duplicatedNameUpdate() {
        var sos = new SosDeviceCreateDto();
        sos.organisationId = 1L;
        sos.assetId = sosAsset.id;
        sos.name = "SOS-UPDATE-MULTIPLE-001";
        deviceService.createDevice(sos);

        var met = new MeteoSensorDeviceCreateDto();
        met.organisationId = 1L;
        met.assetId = metAsset.id;
        met.name = "MET-UPDATE-001";
        var dto = deviceService.createDevice(met);
        assertNotNull(dto);
        met.name = sos.name;
        var ex = assertThrows(NonUniqueException.class, () -> deviceService.updateDevice(dto.id, met));
        assertAll(
                "Exception parameters",
                () -> assertEquals("name", ex.getKey()),
                () -> assertEquals(met.name, ex.getValue())

        );
    }

    @Test
    @DisplayName("Update rejected when the asset is already assigned to another device")
    void multipleAssignmentUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-UPDATE-MULTIPLE-001";
        var first = deviceService.createDevice(input);

        var input02 = new SosDeviceCreateDto();
        input02.organisationId = 1L;
        input02.assetId = otherSosAsset.id;
        input02.name = "SOS-UPDATE-MULTIPLE-002";
        var second = deviceService.createDevice(input02);
        assertNotNull(second);
        assertNotNull(second.id);

        input02.assetId = first.asset.id;
        var ex = assertThrows(NonUniqueException.class, () -> deviceService.updateDevice(second.id, input02));
        assertAll(
                "Exception parameters",
                () -> assertEquals("assetId", ex.getKey()),
                () -> assertEquals(sosAsset.id, ex.getValue())
        );
    }

    @Test
    @DisplayName("Update rejected when the device type would change")
    void deviceTypeUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-TYPE-CHANGE-001";
        var first = deviceService.createDevice(input);

        var changed = new MeteoSensorDeviceCreateDto();
        changed.organisationId = 1L;
        changed.assetId = metAsset.id;
        changed.name = "MET-TYPE-CHANGE-001";
        var ex = assertThrows(ImmutableDeviceTypeException.class, () -> deviceService.updateDevice(first.id, changed));
        assertThat(ex.getMessage(), containsString("device type is immutable"));
    }

    @Test
    @DisplayName("SOS device creation rejected when the asset has no IP address")
    void sosAssetWithoutIpCreate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAssetWoIp.id;
        input.name = "SOS-CREATE-NO-IP-001";
        assertMissingIpViolation(assertThrows(ConstraintViolationException.class, () -> deviceService.createDevice(input)));
    }

    @Test
    @DisplayName("MET device creation rejected when the asset has no IP address")
    void meteoAssetWithoutIpCreate() {
        var input = new MeteoSensorDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = metAssetWoIp.id;
        input.name = "MET-CREATE-NO-IP-001";
        assertMissingIpViolation(assertThrows(ConstraintViolationException.class, () -> deviceService.createDevice(input)));
    }

    @Test
    @DisplayName("Update rejected when the modified asset has no IP address")
    void assetWithoutIpUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-UPDATE-NO-IP-001";
        var result = deviceService.createDevice(input);

        input.assetId = sosAssetWoIp.id;
        assertMissingIpViolation(
                assertThrows(ConstraintViolationException.class, () -> deviceService.updateDevice(result.id, input))
        );
        assertEquals(sosAsset.id, deviceService.getById(result.id).asset.id);
    }

    private void assertMissingIpViolation(ConstraintViolationException ex) {
        var violation = ex.getConstraintViolations().iterator().next();
        assertAll(
                "Violation parameters",
                () -> assertEquals(1, ex.getConstraintViolations().size()),
                () -> assertEquals("asset", violation.getPropertyPath().toString()),
                () -> assertEquals("Asset must have an IP address", violation.getMessage())
        );
    }
}
