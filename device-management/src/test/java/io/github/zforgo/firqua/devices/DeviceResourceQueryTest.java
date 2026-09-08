package io.github.zforgo.firqua.devices;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.panache.common.Sort;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

import io.github.zforgo.firqua.assets.AssetAdminService;
import io.github.zforgo.firqua.assets.MeteoSensorAssetCreateDto;
import io.github.zforgo.firqua.assets.MeteoSensorAssetDto;
import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;
import io.github.zforgo.firqua.test.liquibase.RunMode;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestHTTPEndpoint(DeviceResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeviceResourceQueryTest {

    private static final int insertCount = 97;

    @Inject
    AssetAdminService assetAdminService;

    @Inject
    DeviceService deviceService;

    @BeforeAll
    void init() {
        for (int i = 1; i <= insertCount; i++) {
            var assetCreate = new MeteoSensorAssetCreateDto();
            assetCreate.name = "TEST-0000" + i;
            assetCreate.stationId = "TEST-MET-000" + i;
            assetCreate.vendor = "Lufft";
            var asset = assetAdminService.createAsset(assetCreate);
            var deviceCreate = new MeteoSensorDeviceCreateDto();
            deviceCreate.name = "TEST-0000" + i;
            deviceCreate.organisationId = 1L;
            deviceCreate.assetId = asset.id;
            deviceService.createDevice(deviceCreate);
        }
    }

    @Test
    void notFound() {
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", -1)
                .get("{id}");
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatusCode());
    }

    @Test
    void found() {
        var response = given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", 1)
                .get("{id}");

        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = assertInstanceOf(MeteoSensorDeviceDto.class, response.body().as(DeviceDto.class));
        assertNotNull(responseBody);
        assertAll(
                () -> assertNotNull(responseBody.id),
                () -> assertEquals(1, responseBody.id)
        );
        assertEquals("TEST-00001", responseBody.name);
        assertNotNull(responseBody.asset);
        var assetDto = assertInstanceOf(MeteoSensorAssetDto.class, responseBody.asset);
        assertNotNull(assetDto);
        assertEquals("TEST-00001", assetDto.name);
    }

    @Test
    void defaultOrder() {
        var response = given()
                .when()
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<DeviceDto<?>>>() {});
        assertNotNull(responseBody);
        assertAll(
                "Total",
                () -> assertEquals(insertCount, responseBody.items().size()),
                () -> assertEquals(insertCount, responseBody.pagination().total())
        );
    }

    @Test
    void firstPage() {
        final var pageSize = 5;
        var response = given()
                .when()
                .queryParam(PagingAndSorting.param_PageSize, pageSize)
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<DeviceDto<?>>>() {});
        assertNotNull(responseBody);
        assertEquals(insertCount, responseBody.pagination().total());
        assertAll(
                "PageSize",
                () -> assertEquals(pageSize, responseBody.items().size()),
                () -> assertEquals(pageSize, responseBody.pagination().pageSize())
        );
    }

    @Test
    void nthPage() {
        var pageSize = 6;
        var pageIndex = 4;

        var response = given()
                .when()
                .queryParam(PagingAndSorting.param_PageSize, pageSize)
                .queryParam(PagingAndSorting.param_PageIndex, pageIndex)
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<DeviceDto<?>>>() {});
        assertNotNull(responseBody);
        assertEquals(insertCount, responseBody.pagination().total());
        assertEquals(pageIndex, responseBody.pagination().pageIndex());
        assertAll(
                "PageSize",
                () -> assertEquals(pageSize, responseBody.items().size()),
                () -> assertEquals(pageSize, responseBody.pagination().pageSize())
        );
    }

    @Test
    void overPaged() {
        var pageSize = 6;
        var pageIndex = 200;

        var response = given()
                .when()
                .queryParam(PagingAndSorting.param_PageSize, pageSize)
                .queryParam(PagingAndSorting.param_PageIndex, pageIndex)
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<DeviceDto<?>>>() {});
        assertNotNull(responseBody);
        assertEquals(insertCount, responseBody.pagination().total());
        assertEquals(pageIndex, responseBody.pagination().pageIndex());
        assertEquals(pageSize, responseBody.pagination().pageSize());
        assertEquals(0, responseBody.items().size());
    }

    @Test
    void lastBrokenPage() {
        var pageSize = 13;
        var pageIndex = 7;

        var response = given()
                .when()
                .queryParam(PagingAndSorting.param_PageSize, pageSize)
                .queryParam(PagingAndSorting.param_PageIndex, pageIndex)
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<DeviceDto<?>>>() {});
        assertNotNull(responseBody);
        assertEquals(insertCount, responseBody.pagination().total());
        assertEquals(pageIndex, responseBody.pagination().pageIndex());
        assertEquals(pageSize, responseBody.pagination().pageSize());
        assertEquals(6, responseBody.items().size());
    }

    @Test
    void negativePageIndex() {
        var pageSize = 13;
        var pageIndex = -7;

        var response = given()
                .when()
                .queryParam(PagingAndSorting.param_PageSize, pageSize)
                .queryParam(PagingAndSorting.param_PageIndex, pageIndex)
                .get();
        assertNotNull(response);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertThat(response.body().asString(), containsString("must be greater than or equal to 0"));
    }

    @Test
    void orderedPaged() {
        var pageSize = 2;

        var response = given()
                .when()
                .queryParam(PagingAndSorting.param_PageSize, pageSize)
                .queryParam(PagingAndSorting.param_SortCriteria, "id")
                .queryParam(PagingAndSorting.param_SortDirection, Sort.Direction.Descending)
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<DeviceDto<?>>>() {});
        assertEquals(insertCount, responseBody.pagination().total());
        assertEquals(pageSize, responseBody.pagination().pageSize());
        assertEquals(pageSize, responseBody.items().size());
        var items = responseBody.items();
        assertEquals(insertCount, items.getFirst().id);
        assertEquals(insertCount - 1, items.getLast().id);
    }
}
