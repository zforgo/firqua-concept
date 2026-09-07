package io.github.zforgo.firqua.devices;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

//TODO displayName
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
    private AssetDto sosAssetWoIp;
    private AssetDto metAsset;

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
    }

    @Transactional
    @BeforeEach
    void beforeEach() {
        Device.deleteAll();
    }

    @Test
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
    void incompatibleTypeCreate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = metAsset.id;
        input.name = "SOS-CREATE-INCOMPATIBLE-001";
        assertThrows(IncompatibleAssetTypeException.class, () -> deviceService.createDevice(input));
    }

    //CREATION
    //TODO create duplicated name

    @Disabled("Constraint violation not handled yet")
    @Test
    //TODO refine assertions
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
        var wtf = deviceService.createDevice(input);
    }

    @Test
    void validUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-UPDATE-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        assertNotNull(result.id);

        input.name = "SOS-UPDATE-002";
        input.assetId = sosAssetWoIp.id;
        var modified = deviceService.updateDevice(result.id, input);
        assertNotNull(modified);
        assertEquals(result.id, modified.id);
        assertEquals(sosAssetWoIp.id, modified.asset.id);
    }

    @Test
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

    //MODIFICATION
    //TODO update to duplicated name

    @Disabled("Constraint violation not handled yet")
    @Test
    //TODO refine assertions
    void multipleAssignmentUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = sosAsset.id;
        input.name = "SOS-UPDATE-MULTIPLE-001";
        var first = deviceService.createDevice(input);

        var input02 = new SosDeviceCreateDto();
        input02.organisationId = 1L;
        input02.assetId = sosAssetWoIp.id;
        input02.name = "SOS-UPDATE-MULTIPLE-002";
        var second = deviceService.createDevice(input02);
        assertNotNull(second);
        assertNotNull(second.id);

        input02.assetId = first.asset.id;
        var x = deviceService.updateDevice(second.id, input02);
    }

    @Test
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
        var ex = assertThrows(IllegalArgumentException.class, () -> deviceService.updateDevice(first.id, changed));
        assertThat(ex.getMessage(), containsString("device type is immutable"));
    }
}
