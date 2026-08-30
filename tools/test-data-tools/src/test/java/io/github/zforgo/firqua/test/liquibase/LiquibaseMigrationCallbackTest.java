package io.github.zforgo.firqua.test.liquibase;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiquibaseMigrationCallbackTest {

    private static List<ChangeSetEntry> previousChangeLog = List.of();

    private record ChangeSetEntry(String id, Timestamp dateExecuted) {
    }

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    DataSource dataSource;

    @Test
    @Order(1)
    @DisplayName("Without additional contexts only the configured context is migrated")
    void baseRun() {
        assertEquals(List.of("1"), executedChangeSetIds());
    }

    @Test
    @Order(2)
    @DisplayName("An additional context migrates only the change sets of that context on top of the configured one")
    @LiquibaseMigration(runMode = RunMode.PER_METHOD, additionalContexts = "additional")
    void additionalRun() {
        assertEquals(List.of("1", "2"), executedChangeSetIds());
        assertEquals(List.of("2"), changeSetIdsExecutedForThisTest());
    }

    @Test
    @Order(3)
    @DisplayName("With dropFirst every change set of the configured and the additional context is applied again")
    @LiquibaseMigration(runMode = RunMode.PER_METHOD, additionalContexts = "additional", dropFirst = true)
    void dropFirst() {
        assertEquals(List.of("1", "2"), executedChangeSetIds());
    }

    @Test
    @Order(4)
    @DisplayName("An additional context without change sets migrates nothing")
    @LiquibaseMigration(runMode = RunMode.PER_METHOD, additionalContexts = "missing")
    void missingContext() {
        assertEquals(0, changeSetIdsExecutedForThisTest().size());
    }

    @AfterEach
    void captureChangeLog() {
        previousChangeLog = changeLog();
    }

    private List<ChangeSetEntry> changeLog() {
        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "SELECT ID, DATEEXECUTED FROM DATABASECHANGELOG ORDER BY DATEEXECUTED, ORDEREXECUTED"
                );
                var resultSet = statement.executeQuery()
        ) {
            var entries = new ArrayList<ChangeSetEntry>();
            while (resultSet.next()) {
                entries.add(new ChangeSetEntry(resultSet.getString(1), resultSet.getTimestamp(2)));
            }
            return entries;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read the Liquibase change log", e);
        }
    }

    private List<String> executedChangeSetIds() {
        return changeLog().stream().map(ChangeSetEntry::id).toList();
    }

    private List<String> changeSetIdsExecutedForThisTest() {
        var previous = previousChangeLog;
        return changeLog().stream()
                .filter(entry -> !previous.contains(entry))
                .map(ChangeSetEntry::id)
                .toList();
    }
}
