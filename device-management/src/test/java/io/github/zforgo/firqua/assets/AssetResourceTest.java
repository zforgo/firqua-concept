package io.github.zforgo.firqua.assets;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;
import io.github.zforgo.firqua.test.liquibase.RunMode;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true, additionalContexts = "asset_by_id")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestHTTPEndpoint(AssetResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AssetResourceTest {

    private static final int insertCount = 97;
    private static final int total = 98; //one more element inserted by Liquibase

    @Inject
    AssetAdminService adminService;

    @BeforeAll
    void init() {
        for (int i = 1; i <= insertCount; i++) {
            var dto = new MeteoSensorAssetCreateDto();
            dto.name = "TEST-0000" + i;
            dto.stationId = "TEST-MET-000" + i;
            dto.vendor = "Lufft";
            adminService.createAsset(dto);
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
                .pathParam("id", 100)
                .get("{id}");

        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = assertInstanceOf(MeteoSensorAssetDto.class, response.body().as(AssetDto.class));
        assertNotNull(responseBody);
        assertAll(
                () -> assertNotNull(responseBody.id),
                () -> assertEquals(100, responseBody.id)
        );
        assertEquals("MET-FOO-001", responseBody.stationId);
        assertEquals("Lufft", responseBody.vendor);
        assertEquals("WS500", responseBody.model);
        assertEquals("MET-001", responseBody.name);
    }

    @Test
    void defaultOrder() {
        var response = given()
                .when()
                .get();
        assertNotNull(response);
        assertEquals(OK.getStatusCode(), response.getStatusCode());
        var responseBody = response.body().as(new TypeRef<FilterResult<AssetDto>>() {
        });
        assertNotNull(responseBody);
        assertAll(
                "Total",
                () -> assertEquals(total, responseBody.items().size()),
                () -> assertEquals(total, responseBody.pagination().total())
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
        var responseBody = response.body().as(new TypeRef<FilterResult<AssetDto>>() {
        });
        assertNotNull(responseBody);
        assertEquals(total, responseBody.pagination().total());
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
        var responseBody = response.body().as(new TypeRef<FilterResult<AssetDto>>() {
        });
        assertNotNull(responseBody);
        assertEquals(total, responseBody.pagination().total());
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
        var responseBody = response.body().as(new TypeRef<FilterResult<AssetDto>>() {
        });
        assertNotNull(responseBody);
        assertEquals(total, responseBody.pagination().total());
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
        var responseBody = response.body().as(new TypeRef<FilterResult<AssetDto>>() {
        });
        assertNotNull(responseBody);
        assertEquals(total, responseBody.pagination().total());
        assertEquals(pageIndex, responseBody.pagination().pageIndex());
        assertEquals(pageSize, responseBody.pagination().pageSize());
        assertEquals(7, responseBody.items().size());
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
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
        assertThat(response.body().asString(), containsString("Page index must be >= 0"));
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
        var responseBody = response.body().as(new TypeRef<FilterResult<AssetDto>>() {
        });
        assertEquals(total, responseBody.pagination().total());
        assertEquals(pageSize, responseBody.pagination().pageSize());
        assertEquals(pageSize, responseBody.items().size());
        var items = responseBody.items();
        assertEquals(100, items.getFirst().id); //hard coded id defined in liquibase changeset
        assertEquals(total - 1, items.getLast().id);
    }

}
