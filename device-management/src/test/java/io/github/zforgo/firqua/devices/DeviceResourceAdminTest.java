package io.github.zforgo.firqua.devices;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

import io.github.zforgo.firqua.ConflictExceptionMapper;
import io.github.zforgo.firqua.IncompatibleAssetTypeExceptionMapper;
import io.github.zforgo.firqua.assets.AssetResource;
import io.github.zforgo.firqua.assets.SosAssetDto;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;
import io.github.zforgo.firqua.test.liquibase.RunMode;

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

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true, additionalContexts = "device_resource_admin")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestHTTPEndpoint(DeviceResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeviceResourceAdminTest {

    @TestHTTPResource
    @TestHTTPEndpoint(DeviceResource.class)
    URI devicesEndpoint;

    @Inject
    DeviceService deviceService;

    @BeforeEach
    @Transactional
    void clearDevices() {
        Device.deleteAll();
    }

    @Test
    @DisplayName("SOS device created successfully")
    void sosDeviceCreated() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 3L;
        input.name = "SOS-CREATE-001";
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .post();

        assertNotNull(response);
        assertEquals(CREATED.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<DeviceDto<?>>() {});
        var expectedLocation = UriBuilder.fromUri(devicesEndpoint)
                .path(AssetResource.class, "get")
                .build(responseBody.id);
        assertEquals(expectedLocation, URI.create(response.getHeader(LOCATION)));

        var dto = assertInstanceOf(SosDeviceDto.class, responseBody);
        assertAll(
                () -> assertNotNull(dto.id),
                () -> assertTrue(dto.id > 0)
        );
        assertAll(
                "Organisation Unit checks",
                () -> assertNotNull(dto.organisationUnit),
                () -> assertEquals("OU_FOO", dto.organisationUnit.slug)
        );
        assertEquals(input.name, dto.name);
        assertNotNull(dto.asset);
        assertEquals(input.assetId, dto.asset.id);

    }

    @Test
    @DisplayName("MET device created successfully")
    void meteoDeviceCreated() {
        var input = new MeteoSensorDeviceCreateDto();
        input.organisationId = 2L;
        input.assetId = 1L;
        input.name = "MET-CREATE-001";
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .post();

        assertNotNull(response);
        assertEquals(CREATED.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<DeviceDto<?>>() {});
        var expectedLocation = UriBuilder.fromUri(devicesEndpoint)
                .path(AssetResource.class, "get")
                .build(responseBody.id);
        assertEquals(expectedLocation, URI.create(response.getHeader(LOCATION)));

        var dto = assertInstanceOf(MeteoSensorDeviceDto.class, responseBody);
        assertAll(
                () -> assertNotNull(dto.id),
                () -> assertTrue(dto.id > 0)
        );
        assertAll(
                "Organisation Unit checks",
                () -> assertNotNull(dto.organisationUnit),
                () -> assertEquals("OU_BAR", dto.organisationUnit.slug)
        );
        assertEquals(dto.name, input.name);
        assertNotNull(dto.asset);
        assertEquals(input.assetId, dto.asset.id);

    }

    @Test
    @DisplayName("Creation rejected when the asset type does not match the device type")
    void incompatibleTypeCreate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 1L;
        input.name = "SOS-CREATE-INCOMPATIBLE-001";
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .post();

        assertNotNull(response);
        assertEquals(CONFLICT.getStatusCode(), response.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("assetId", response.header(IncompatibleAssetTypeExceptionMapper.HEADER_KEY)),
                () -> assertEquals(
                        input.assetId.toString(), response.header(IncompatibleAssetTypeExceptionMapper.HEADER_VALUE)
                ),
                () -> assertEquals("SOS", response.header("X-Conflict-Expected"))
        );
    }

    @Test
    @DisplayName("Creation rejected when the name is already taken")
    void duplicatedNameCreate() {
        var sos = new SosDeviceCreateDto();
        sos.organisationId = 1L;
        sos.assetId = 3L;
        sos.name = "SOS-CREATE-MULTI-001";
        deviceService.createDevice(sos);

        var met = new MeteoSensorDeviceCreateDto();
        met.organisationId = 1L;
        met.assetId = 1L;
        met.name = sos.name;
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(met)
                .post();

        assertNotNull(response);
        assertEquals(CONFLICT.getStatusCode(), response.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("name", response.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(met.name, response.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Creation rejected when the asset is already assigned to another device")
    void multipleAssignmentCreate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 3L;
        input.name = "SOS-CREATE-MULTI-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);

        var dto = assertInstanceOf(SosDeviceDto.class, result);
        var assetDto = assertInstanceOf(SosAssetDto.class, dto.asset);
        assertEquals(input.assetId, assetDto.id);

        input.organisationId = 2L;
        input.name = "SOS-TEST-002";
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .post();

        assertNotNull(response);
        assertEquals(CONFLICT.getStatusCode(), response.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("assetId", response.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(input.assetId.toString(), response.header(ConflictExceptionMapper.HEADER_VALUE))
        );
    }

    @Test
    @DisplayName("Name and asset updated successfully")
    void validUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 3L;
        input.name = "SOS-UPDATE-001";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        assertNotNull(result.id);

        input.name = "SOS-UPDATE-002";
        input.assetId = 4L;

        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .pathParam("id", result.id)
                .put("{id}");

        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<DeviceDto<?>>() {});

        var dto = assertInstanceOf(SosDeviceDto.class, responseBody);
        assertAll(
                () -> assertNotNull(dto.id),
                () -> assertEquals(result.id, dto.id)
        );
        assertEquals(input.name, dto.name);
        assertEquals(input.assetId, dto.asset.id);
    }

    @Test
    @DisplayName("Update rejected when the modified asset type does not match the device type")
    void incompatibleUpdate() {

        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 3L;
        input.name = "SOS-UPDATE-INCOMPATIBLE-002";
        var result = deviceService.createDevice(input);
        assertNotNull(result);
        assertNotNull(result.id);

        input.assetId = 1L;
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input)
                .pathParam("id", result.id)
                .put("{id}");
        assertNotNull(response);
        assertEquals(CONFLICT.getStatusCode(), response.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("assetId", response.header(IncompatibleAssetTypeExceptionMapper.HEADER_KEY)),
                () -> assertEquals(
                        input.assetId.toString(), response.header(IncompatibleAssetTypeExceptionMapper.HEADER_VALUE)
                ),
                () -> assertEquals("SOS", response.header("X-Conflict-Expected"))
        );

    }

    @Test
    @DisplayName("Update rejected when the name is already taken")
    void duplicatedNameUpdate() {
        var sos = new SosDeviceCreateDto();
        sos.organisationId = 1L;
        sos.assetId = 3L;
        sos.name = "SOS-UPDATE-MULTIPLE-001";
        deviceService.createDevice(sos);

        var met = new MeteoSensorDeviceCreateDto();
        met.organisationId = 1L;
        met.assetId = 1L;
        met.name = "MET-UPDATE-001";
        var dto = deviceService.createDevice(met);
        assertNotNull(dto);
        met.name = sos.name;

        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(met)
                .pathParam("id", dto.id)
                .put("{id}");

        assertNotNull(response);
        assertEquals(CONFLICT.getStatusCode(), response.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("name", response.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(met.name, response.header(ConflictExceptionMapper.HEADER_VALUE))
        );

    }

    @Test
    @DisplayName("Update rejected when the asset is already assigned to another device")
    void multipleAssignmentUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 3L;
        input.name = "SOS-UPDATE-MULTIPLE-001";
        var first = deviceService.createDevice(input);

        var input02 = new SosDeviceCreateDto();
        input02.organisationId = 1L;
        input02.assetId = 4L;
        input02.name = "SOS-UPDATE-MULTIPLE-002";
        var second = deviceService.createDevice(input02);
        assertNotNull(second);
        assertNotNull(second.id);

        input02.assetId = first.asset.id;

        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(input02)
                .pathParam("id", second.id)
                .put("{id}");

        assertNotNull(response);
        assertEquals(CONFLICT.getStatusCode(), response.getStatusCode());
        assertAll(
                "Conflict headers",
                () -> assertEquals("assetId", response.header(ConflictExceptionMapper.HEADER_KEY)),
                () -> assertEquals(input02.assetId.toString(), response.header(ConflictExceptionMapper.HEADER_VALUE))
        );

    }

    @Test
    @DisplayName("Update rejected when the device type would change")
    void deviceTypeUpdate() {
        var input = new SosDeviceCreateDto();
        input.organisationId = 1L;
        input.assetId = 3L;
        input.name = "SOS-TYPE-CHANGE-001";
        var first = deviceService.createDevice(input);

        var changed = new MeteoSensorDeviceCreateDto();
        changed.organisationId = 1L;
        changed.assetId = 2L;
        changed.name = "MET-TYPE-CHANGE-001";
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .body(changed)
                .pathParam("id", first.id)
                .put("{id}");

        assertNotNull(response);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
        assertThat(response.body().asString(), containsString("device type is immutable"));
    }
}
