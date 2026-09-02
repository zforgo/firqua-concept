package io.github.zforgo.firqua.assets;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.zforgo.firqua.common.NonUniqueIpAddressException;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static io.github.zforgo.firqua.test.liquibase.RunMode.ALWAYS;
import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_METHOD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@QuarkusTest
@LiquibaseMigration(runMode = PER_METHOD, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
class AssetAdminServiceTest {

    @Inject
    AssetAdminService service;

    @ParameterizedTest
    @MethodSource("validCreateParams")
    @DisplayName("Every valid AssetCreateDto subtype is persisted successfully")
    <I extends AssetCreateDto & IpAddressAwareDto, R extends AssetDto & IpAddressAwareDto> void sampleCreate(
            I input,
            Class<R> resultClass
    ) {
        var result = service.createAsset(input);
        assertNotNull(result);

        var dto = assertInstanceOf(resultClass, result);
        assertAll(
                () -> assertNotNull(result.id),
                () -> assertTrue(result.id > 0)
        );
        assertEquals(input.getIpAddress(), dto.getIpAddress());
    }

    @ParameterizedTest
    @MethodSource("duplicatedIpAddresses")
    @DisplayName("Duplicate ipAddress on the same AssetCreateDto subtype is rejected with NonUniqueIpAddressException")
    <I extends AssetCreateDto & IpAddressAwareDto, R extends AssetDto & IpAddressAwareDto> void createDuplicatedIpSameEntity(
            I input,
            I duplication, Class<R> resultClass
    ) {
        var result = service.createAsset(input);
        assertNotNull(result);
        var dto = assertInstanceOf(resultClass, result);
        assertAll(
                () -> assertNotNull(result.id),
                () -> assertTrue(result.id > 0)
        );

        var ex = assertThrows(NonUniqueIpAddressException.class, () -> service.createAsset(duplication));
        assertThat(ex.getMessage(), containsString(dto.getIpAddress()));
    }

    @Test
    @DisplayName("Duplicate ipAddress across AssetCreateDto subtypes is rejected with NonUniqueIpAddressException")
    void createDuplicatedIpAcrossEntities() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.1.1");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "FOO-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var metWithIp = new MeteoSensorAssetCreateDto();
        metWithIp.stationId = "MET-TEST-001";
        metWithIp.name = "MET-001";
        metWithIp.vendor = "Lufft";
        metWithIp.model = "WS500";
        metWithIp.setIpAddress("192.168.1.1");

        var result = service.createAsset(sosWithIp);
        assertNotNull(result);
        var dto = assertInstanceOf(SosAssetDto.class, result);
        assertAll(
                () -> assertNotNull(result.id),
                () -> assertTrue(result.id > 0)
        );
        var ex = assertThrows(NonUniqueIpAddressException.class, () -> service.createAsset(metWithIp));
        assertThat(ex.getMessage(), containsString(dto.getIpAddress()));

    }

    @LiquibaseMigration(runMode = ALWAYS, dropFirst = true)
    @ParameterizedTest
    @MethodSource("modifiedEntities")
    @DisplayName("Every valid Asset modification is persisted successfully")
    <I extends AssetCreateDto & IpAddressAwareDto, R extends AssetDto & IpAddressAwareDto> void modify(
            I orig,
            I modified, Class<R> resultClass
    ) {
        var createResult = service.createAsset(orig);
        assertNotNull(createResult);
        var modifiedResult = service.updateAsset(createResult.id, modified);
        var dto = assertInstanceOf(resultClass, modifiedResult);
        assertEquals(modified.getIpAddress(), dto.getIpAddress());
    }

    @Test
    @DisplayName("Modifying Asset to an existing ipAddress is rejected with NonUniqueIpAddressException")
    void modifyToExistingIp() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.1.1");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "FOO-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var metWithIp = new MeteoSensorAssetCreateDto();
        metWithIp.stationId = "MET-TEST-001";
        metWithIp.name = "MET-001";
        metWithIp.vendor = "Lufft";
        metWithIp.model = "WS500";
        metWithIp.setIpAddress("192.168.2.1");

        var _ = service.createAsset(sosWithIp);
        var metResult = service.createAsset(metWithIp);
        metWithIp.setIpAddress(sosWithIp.getIpAddress());
        var ex = assertThrows(NonUniqueIpAddressException.class, () -> service.updateAsset(metResult.id, metWithIp));
        assertThat(ex.getMessage(), containsString(metWithIp.getIpAddress()));

    }

    static Stream<Arguments> modifiedEntities() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.1.1");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "FOO-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var sosWithIpMod = new SosAssetCreateDto();
        sosWithIpMod.setIpAddress(sosWithIp.getIpAddress());
        sosWithIpMod.stationId = sosWithIp.stationId + "-MOD";
        sosWithIpMod.name = sosWithIp.name + "-MOD";
        sosWithIpMod.serviceProvider = sosWithIp.serviceProvider;
        sosWithIpMod.vendor = sosWithIp.vendor;
        sosWithIpMod.model = sosWithIp.model;

        var sosWoIp = new SosAssetCreateDto();
        sosWoIp.stationId = "SOS-TEST-002";
        sosWoIp.name = "SOS-002";
        sosWoIp.serviceProvider = "AT&T";
        sosWoIp.vendor = "J&R Technology";
        sosWoIp.model = "JR321-SC";

        return Stream.of(
                argumentSet("Sos all fields modified except ipAddress", sosWithIp, sosWithIpMod, SosAssetDto.class),
                argumentSet("Sos all fields modified and ipAddress cleaned", sosWithIp, sosWoIp, SosAssetDto.class),
                argumentSet("Sos all fields modified and ipAddress added", sosWoIp, sosWithIpMod, SosAssetDto.class)
        );
    }

    static Stream<Arguments> duplicatedIpAddresses() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.1.1");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "FOO-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var sosWithIpMod = new SosAssetCreateDto();
        sosWithIpMod.setIpAddress(sosWithIp.getIpAddress());
        sosWithIpMod.stationId = sosWithIp.stationId + "-MOD";
        sosWithIpMod.name = sosWithIp.name + "-MOD";
        sosWithIpMod.serviceProvider = sosWithIp.serviceProvider;
        sosWithIpMod.vendor = sosWithIp.vendor;
        sosWithIpMod.model = sosWithIp.model;

        var metWithIp = new MeteoSensorAssetCreateDto();
        metWithIp.stationId = "MET-TEST-001";
        metWithIp.name = "MET-001";
        metWithIp.vendor = "Lufft";
        metWithIp.model = "WS500";
        metWithIp.setIpAddress("192.168.2.1");

        var metWithIpMod = new MeteoSensorAssetCreateDto();
        metWithIpMod.name = metWithIp.name + "-MOD";
        metWithIpMod.vendor = metWithIp.vendor;
        metWithIpMod.model = metWithIp.model;
        metWithIpMod.setIpAddress(metWithIp.getIpAddress());
        metWithIpMod.stationId = metWithIp.stationId + "-MOD";

        return Stream.of(
                argumentSet("SosAssetCreateDto", sosWithIp, sosWithIpMod, SosAssetDto.class),
                argumentSet("MeteoSensorAssetCreateDto", metWithIp, metWithIpMod, MeteoSensorAssetDto.class)
        );

    }

    static Stream<Arguments> validCreateParams() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.1.1");
        sosWithIp.stationId = "SOS-TEST-001";
        sosWithIp.name = "FOO-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var sosWoIp = new SosAssetCreateDto();
        sosWoIp.stationId = "SOS-TEST-002";
        sosWoIp.name = "SOS-002";
        sosWoIp.serviceProvider = "AT&T";
        sosWoIp.vendor = "J&R Technology";
        sosWoIp.model = "JR321-SC";

        var sosWoIp2 = new SosAssetCreateDto();
        sosWoIp2.stationId = "SOS-TEST-003";
        sosWoIp2.name = "SOS-003";
        sosWoIp2.serviceProvider = "AT&T";
        sosWoIp2.vendor = "J&R Technology";
        sosWoIp2.model = "JR321-SC";

        var metWithIp = new MeteoSensorAssetCreateDto();
        metWithIp.stationId = "MET-TEST-001";
        metWithIp.name = "MET-001";
        metWithIp.vendor = "Lufft";
        metWithIp.model = "WS500";
        metWithIp.setIpAddress("192.168.2.1");

        var metWoIp = new MeteoSensorAssetCreateDto();
        metWoIp.stationId = "MET-TEST-002";
        metWoIp.name = "MET-002";
        metWoIp.vendor = "Lufft";
        metWoIp.model = "WS500";

        var metWoIp2 = new MeteoSensorAssetCreateDto();
        metWoIp2.stationId = "MET-TEST-003";
        metWoIp2.name = "MET-003";
        metWoIp2.vendor = "Lufft";
        metWoIp2.model = "WS500";

        return Stream.of(
                argumentSet("SosAssetCreateDto with ipAddress", sosWithIp, SosAssetDto.class),
                argumentSet("SosAssetCreateDto without ipAddress", sosWoIp, SosAssetDto.class),
                argumentSet("SosAssetCreateDto without ipAddress (null must not collide)", sosWoIp2, SosAssetDto.class),
                argumentSet("MeteoSensorAssetCreateDto with ipAddress", metWithIp, MeteoSensorAssetDto.class),
                argumentSet("MeteoSensorAssetCreateDto without ipAddress", metWoIp, MeteoSensorAssetDto.class),
                argumentSet(
                        "MeteoSensorAssetCreateDto without ipAddress (null must not collide)", metWoIp2,
                        MeteoSensorAssetDto.class
                )
        );

    }
}
