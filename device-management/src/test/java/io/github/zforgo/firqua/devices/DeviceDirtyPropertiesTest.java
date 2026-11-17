package io.github.zforgo.firqua.devices;

import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.hibernate.DirtyProperties;
import io.github.zforgo.firqua.organisations.OrganisationUnit;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true, additionalContexts = "dirty_props")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestTransaction
public class DeviceDirtyPropertiesTest {

    @Inject
    DeviceMapper deviceMapper;

    @Test
    @DisplayName("Changed properties to same value are ignored")
    void changeToSame() {
        var entity = Device.<Device<? extends Asset>> findByIdOptional(1L, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new EntityNotFoundException("No device found"));
        var changes = new MeteoSensorDeviceCreateDto();
        changes.enabled = false;
        changes.assetId = entity.asset.id;
        changes.organisationId = entity.organisationUnit.id;
        changes.name = "DEV-METEO-001";
        deviceMapper.updateEntity(changes, entity);
        var changedFields = DirtyProperties.collect(entity);
        assertTrue(changedFields.isEmpty());
    }

    @Test
    @DisplayName("Related entity changes occurs in changeSet")
    void relationChanges() {
        var entity = Device.<Device<? extends Asset>> findByIdOptional(1L, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new EntityNotFoundException("No device found"));
        var changedAsset = Asset.<Asset> list("name", "MET-002").getFirst();
        var changedOU = OrganisationUnit.<OrganisationUnit> list("slug", "OU_BAR").getFirst();
        var changes = new MeteoSensorDeviceCreateDto();
        changes.enabled = true;
        changes.assetId = changedAsset.id;
        changes.organisationId = changedOU.id;
        changes.name = "CHG-METEO-001";
        deviceMapper.updateEntity(changes, entity);
        entity.evaluateState();
        var changedFields = DirtyProperties.collect(entity);
        assertThat(changedFields, containsInAnyOrder("name", "enabled", "asset", "organisationUnit", "effectiveEnabled"));

    }
}
