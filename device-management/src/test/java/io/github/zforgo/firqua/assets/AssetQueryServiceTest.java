package io.github.zforgo.firqua.assets;

import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_METHOD;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@LiquibaseMigration(runMode = PER_METHOD, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
public class AssetQueryServiceTest {

    @Inject
    AssetQueryService service;

    @Test
    void notFound() {
        assertThrows(EntityNotFoundException.class, () -> service.getById(-1L));
    }

    @Test
    @LiquibaseMigration(runMode = PER_METHOD, dropFirst = true, additionalContexts = "asset_by_id")
    void found() {
        var dto = service.getById(100L);
        assertNotNull(dto);
        var meteo = assertInstanceOf(MeteoSensorAssetDto.class, dto);
        assertAll(
                () -> assertEquals("MET-001", meteo.name),
                () -> assertEquals("Lufft", meteo.vendor),
                () -> assertEquals("WS500", meteo.model),
                () -> assertEquals("MET-FOO-001", meteo.stationId),
                () -> assertNull(meteo.ipAddress)
        );
    }
}
