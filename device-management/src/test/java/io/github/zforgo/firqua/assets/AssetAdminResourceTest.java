package io.github.zforgo.firqua.assets;

import java.net.URI;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import io.github.zforgo.firqua.ConflictExceptionMapper;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;
import io.github.zforgo.firqua.test.liquibase.RunMode;

import static io.github.zforgo.firqua.test.liquibase.RunMode.ALWAYS;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_METHOD, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestHTTPEndpoint(AssetAdminResource.class)
public class AssetAdminResourceTest {

    @TestHTTPResource
    @TestHTTPEndpoint(AssetResource.class)
    URI assetsEndpoint;

    @Inject
    AssetAdminService service;

    @ParameterizedTest
    @MethodSource("io.github.zforgo.firqua.assets.AssetAdminServiceTest#validCreateParams")
    @DisplayName("Every valid AssetCreateDto subtype is CREATED")
    <I extends AssetCreateDto & IpAddressAwareDto, R extends AssetDto & IpAddressAwareDto> void validCreate(
            I input,
            Class<R> resultClass
    ) {
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .post();
        assertEquals(CREATED.getStatusCode(), response.getStatusCode());
        var responseBody = assertInstanceOf(resultClass, response.body().as(resultClass));
        assertNotNull(responseBody);
        assertAll(
                () -> assertNotNull(responseBody.id),
                () -> assertTrue(responseBody.id > 0)
        );

        var expectedLocation = UriBuilder.fromUri(assetsEndpoint)
                .path(AssetResource.class, "get")
                .build(responseBody.id);
        assertEquals(expectedLocation, URI.create(response.getHeader(LOCATION)));

        assertEquals(responseBody.getIpAddress(), input.getIpAddress());
    }

    @ParameterizedTest
    @MethodSource("duplicatedIpAddresses")
    @DisplayName("Duplicate ipAddress on the same AssetCreateDto subtype is rejected with CONFLICT")
    <I extends AssetCreateDto & IpAddressAwareDto> void createDuplicatedIpSameEntity(I input, I duplication) {

        service.createAsset(input);

        var duplicationResponse = given()
                .when()
                .contentType(ContentType.JSON)
                .body(duplication)
                .post();

        assertNotNull(duplicationResponse);
        assertEquals(CONFLICT.getStatusCode(), duplicationResponse.getStatusCode());
        assertThat(duplicationResponse.body().asString(), containsString(input.getIpAddress()));
    }

    @Test
    @DisplayName("Duplicate ipAddress across AssetCreateDto subtypes is rejected with CONFLICT")
    void createDuplicatedIpAcrossEntities() {
        var sosWithIp = new SosAssetCreateDto();
        sosWithIp.setIpAddress("192.168.1.1");
        sosWithIp.stationId = "FOO-001";
        sosWithIp.name = "SOS-SOS-001";
        sosWithIp.serviceProvider = "AT&T";
        sosWithIp.vendor = "J&R Technology";
        sosWithIp.model = "JR321-SC";

        var metWithIp = new MeteoSensorAssetCreateDto();
        metWithIp.name = "MET-001";
        metWithIp.vendor = "Lufft";
        metWithIp.model = "WS500";
        metWithIp.setIpAddress("192.168.1.1");
        metWithIp.stationId = "MET-FOO-001";

        service.createAsset(sosWithIp);

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(metWithIp)
                .post();
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertThat(responseFailed.body().asString(), containsString(sosWithIp.getIpAddress()));
    }

    @Test
    @DisplayName("Creation rejected when the name is already taken")
    void createDuplicatedName() {
        var sos = new SosAssetCreateDto();
        sos.stationId = "SOS-TEST-001";
        sos.name = "DUPLICATION-001";
        sos.serviceProvider = "AT&T";
        sos.vendor = "J&R Technology";
        sos.model = "JR321-SC";
        service.createAsset(sos);

        var met = new MeteoSensorAssetCreateDto();
        met.stationId = "MET-TEST-001";
        met.name = "DUPLICATION-001";
        met.vendor = "Lufft";
        met.model = "WS500";

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(met)
                .post();
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("name", responseFailed.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(met.name, responseFailed.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Creation SOS asset rejected when the stationId is already taken")
    void createDuplicatedSosStationId() {
        var sos = new SosAssetCreateDto();
        sos.stationId = "DUPLICATED-001";
        sos.name = "SOS-001";
        sos.serviceProvider = "AT&T";
        sos.vendor = "J&R Technology";
        sos.model = "JR321-SC";
        service.createAsset(sos);

        var duplication = new SosAssetCreateDto();
        duplication.stationId = "DUPLICATED-001";
        duplication.name = "SOS-002";
        duplication.serviceProvider = "AT&T";
        duplication.vendor = "J&R Technology";
        duplication.model = "JR321-SC";

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(duplication)
                .post();
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("stationId", responseFailed.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(duplication.stationId, responseFailed.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Creation MET asset rejected when the stationId is already taken")
    void createDuplicatedMeteoStationId() {
        var met = new MeteoSensorAssetCreateDto();
        met.stationId = "DUPLICATED-001";
        met.name = "METS-001";
        met.vendor = "Lufft";
        met.model = "WS500";
        service.createAsset(met);

        var duplication = new MeteoSensorAssetCreateDto();
        duplication.stationId = "DUPLICATED-001";
        duplication.name = "MET-002";
        duplication.vendor = "Lufft";
        duplication.model = "WS500";

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(duplication)
                .post();
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("stationId", responseFailed.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(duplication.stationId, responseFailed.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @LiquibaseMigration(runMode = ALWAYS, dropFirst = true)
    @ParameterizedTest
    @MethodSource("io.github.zforgo.firqua.assets.AssetAdminServiceTest#modifiedEntities")
    @DisplayName("Every valid Asset modification is persisted successfully")
    <I extends AssetCreateDto & IpAddressAwareDto, R extends AssetDto & IpAddressAwareDto> void validModification(
            I orig,
            I modified, Class<R> resultClass
    ) {

        var createdAsset = service.createAsset(orig);

        var modificationResponse = given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", createdAsset.id)
                .body(modified)
                .put("{id}");
        assertEquals(OK.getStatusCode(), modificationResponse.getStatusCode());
        var modifiedAsset = modificationResponse.body().as(resultClass);
        assertNotNull(modifiedAsset);
        assertEquals(modified.getIpAddress(), modifiedAsset.getIpAddress());
    }

    @Test
    @DisplayName("Update rejected when the name is already taken")
    void modifyToExistingName() {
        var sos = new SosAssetCreateDto();
        sos.stationId = "SOS-FOO-001";
        sos.name = "SOS-001";
        sos.serviceProvider = "AT&T";
        sos.vendor = "J&R Technology";
        sos.model = "JR321-SC";

        var met = new MeteoSensorAssetCreateDto();
        met.stationId = "MET-FOO-001";
        met.name = "METS-001";
        met.vendor = "Lufft";
        met.model = "WS500";
        service.createAsset(sos);
        var created = service.createAsset(met);
        met.name = sos.name;

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(met)
                .pathParam("id", created.id)
                .put("{id}");
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("name", responseFailed.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(met.name, responseFailed.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Update SOS asset rejected when stationId is taken")
    void modifySosToExistingStationId() {
        var first = new SosAssetCreateDto();
        first.stationId = "SOS-FOO-001";
        first.name = "SOS-001";
        first.serviceProvider = "AT&T";
        first.vendor = "J&R Technology";
        first.model = "JR321-SC";
        service.createAsset(first);

        var second = new SosAssetCreateDto();
        second.stationId = "SOS-FOO-002";
        second.name = "SOS-002";
        second.serviceProvider = "AT&T";
        second.vendor = "J&R Technology";
        second.model = "JR321-SC";
        var created = service.createAsset(second);
        second.stationId = first.stationId;

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(second)
                .pathParam("id", created.id)
                .put("{id}");
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("stationId", responseFailed.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(second.stationId, responseFailed.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Update SOS asset rejected when stationId is taken")
    void modifyMetToExistingStationId() {
        var first = new MeteoSensorAssetCreateDto();
        first.stationId = "MET-FOO-001";
        first.name = "METS-001";
        first.vendor = "Lufft";
        first.model = "WS500";
        service.createAsset(first);

        var second = new MeteoSensorAssetCreateDto();
        second.stationId = "MET-FOO-002";
        second.name = "METS-002";
        second.vendor = "Lufft";
        second.model = "WS500";
        var created = service.createAsset(second);
        second.stationId = first.stationId;

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(second)
                .pathParam("id", created.id)
                .put("{id}");
        assertNotNull(responseFailed);
        assertEquals(CONFLICT.getStatusCode(), responseFailed.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("stationId", responseFailed.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(second.stationId, responseFailed.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Modifying Asset to an existing ipAddress is rejected with CONFLICT")
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

        service.createAsset(sosWithIp);

        var metResult = service.createAsset(metWithIp);
        metWithIp.setIpAddress(sosWithIp.getIpAddress());

        var modificationResponse = given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", metResult.id)
                .body(metWithIp)
                .put("{id}");
        assertEquals(CONFLICT.getStatusCode(), modificationResponse.getStatusCode());
        assertThat(modificationResponse.body().asString(), containsString(metWithIp.getIpAddress()));
    }

    @Test
    @DisplayName("Update rejected when the modified entity type does not match the dto type")
    void incompatibleUpdate() {
        var sos = new SosAssetCreateDto();
        sos.stationId = "SOS-FOO-001";
        sos.name = "SOS-001";
        sos.serviceProvider = "AT&T";
        sos.vendor = "J&R Technology";
        sos.model = "JR321-SC";
        var created = service.createAsset(sos);

        var met = new MeteoSensorAssetCreateDto();
        met.stationId = "MET-FOO-001";
        met.name = "METS-001";
        met.vendor = "Lufft";
        met.model = "WS500";

        var responseFailed = given()
                .when()
                .contentType(ContentType.JSON)
                .body(met)
                .pathParam("id", created.id)
                .put("{id}");
        assertNotNull(responseFailed);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), responseFailed.getStatusCode());
        assertThat(responseFailed.body().asString(), containsString("asset type is immutable"));
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
                argumentSet("SosAssetCreateDto", sosWithIp, sosWithIpMod),
                argumentSet("MeteoSensorAssetCreateDto", metWithIp, metWithIpMod)
        );

    }
}
