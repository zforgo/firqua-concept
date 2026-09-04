package io.github.zforgo.firqua.assets;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AssetQueryServiceFilterTest {

    private static final int total = 97;

    @Inject
    AssetAdminService adminService;

    @Inject
    AssetQueryService queryService;

    @BeforeAll
    void init() {
        for (int i = 1; i <= total; i++) {
            var dto = new MeteoSensorAssetCreateDto();
            dto.name = "TEST-0000" + i;
            dto.stationId = "TEST-MET-000" + i;
            dto.vendor = "Lufft";
            adminService.createAsset(dto);
        }
    }

    @Test
    void allDefaultOrder() {
        var result = queryService.filter(new PagingAndSorting());
        assertNotNull(result);
        assertAll(
                "Total",
                () -> assertEquals(total, result.items().size()),
                () -> assertEquals(total, result.pagination().total())
        );
    }

    @Test
    void firstPage() {
        var pageSize = 5;
        var pas = new PagingAndSorting();
        pas.pageSize = pageSize;
        var result = queryService.filter(pas);
        assertNotNull(result);
        assertEquals(total, result.pagination().total());
        assertAll(
                "PageSize",
                () -> assertEquals(pageSize, result.items().size()),
                () -> assertEquals(pageSize, result.pagination().pageSize())
        );
    }

    @Test
    void nthPage() {
        var pageSize = 6;
        var pageIndex = 4;
        var pas = new PagingAndSorting();
        pas.pageSize = pageSize;
        pas.pageIndex = pageIndex;
        var result = queryService.filter(pas);
        assertNotNull(result);
        assertEquals(total, result.pagination().total());
        assertEquals(pageIndex, result.pagination().pageIndex());
        assertAll(
                "PageSize",
                () -> assertEquals(pageSize, result.items().size()),
                () -> assertEquals(pageSize, result.pagination().pageSize())
        );
    }

    @Test
    void overPaged() {
        var pageSize = 6;
        var pageIndex = 200;
        var pas = new PagingAndSorting();
        pas.pageSize = pageSize;
        pas.pageIndex = pageIndex;
        var result = queryService.filter(pas);
        assertNotNull(result);
        assertEquals(total, result.pagination().total());
        assertEquals(pageIndex, result.pagination().pageIndex());
        assertEquals(pageSize, result.pagination().pageSize());
        assertEquals(0, result.items().size());
    }

    @Test
    void lastBrokenPage() {
        var pageSize = 13;
        var pageIndex = 7;
        var pas = new PagingAndSorting();
        pas.pageSize = pageSize;
        pas.pageIndex = pageIndex;
        var result = queryService.filter(pas);
        assertNotNull(result);
        assertEquals(total, result.pagination().total());
        assertEquals(pageIndex, result.pagination().pageIndex());
        assertEquals(pageSize, result.pagination().pageSize());
        assertEquals(6, result.items().size());
    }

    @Test
    void negativePageIndex() {
        var pageSize = 13;
        var pageIndex = -7;
        var pas = new PagingAndSorting();
        pas.pageSize = pageSize;
        pas.pageIndex = pageIndex;
        var ex = assertThrows(IllegalArgumentException.class, () -> queryService.filter(pas));
        assertThat(ex.getMessage(), startsWith("Page index must be >= 0"));
    }

    @Test
    void orderedPaged() {
        var pageSize = 2;
        var pas = new PagingAndSorting();
        pas.pageSize = pageSize;
        pas.sortDirection = Sort.Direction.Descending;
        pas.sortingCriteria = "id";
        var result = queryService.filter(pas);
        assertNotNull(result);
        assertEquals(total, result.pagination().total());
        var items = result.items();
        assertEquals(pageSize, items.size());
        assertEquals(total, items.getFirst().id);
        assertEquals(total - 1, items.getLast().id);
    }
}
