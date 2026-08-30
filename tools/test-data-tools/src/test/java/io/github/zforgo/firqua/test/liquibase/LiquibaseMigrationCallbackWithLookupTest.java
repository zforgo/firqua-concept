package io.github.zforgo.firqua.test.liquibase;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true, additionalContexts = "lookup")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiquibaseMigrationCallbackWithLookupTest {

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    DataSource dataSource;

    @Test
    @DisplayName("Lookup extension should activated")
    @LiquibaseMigration(runMode = RunMode.PER_METHOD, additionalContexts = "lookup", dropFirst = true)
    void lookup() throws SQLException {
        final var conn = dataSource.getConnection();

        assertTableExists(conn, "ORGANISATIONS");
        assertEquals(3, countRows(conn, "ORGANISATIONS"), "Wrong record count on ORGANISATIONS table");

        assertTableExists(conn, "DEVICES");
        assertEquals(1, countRows(conn, "DEVICES"), "DEVICES must contain exactly one record");

        var sql = """
                SELECT d.NAME, o.NAME
                FROM DEVICES d
                JOIN ORGANISATIONS o ON o.ID = d.ORGANISATION_ID
                """;
        try (var st = conn.createStatement(); var rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "the device must reference a row of ORGANISATIONS");
            assertEquals("dev_foo", rs.getString(1), "unexpected device name");
            assertEquals("foo", rs.getString(2), "ORGANISATION_ID was resolved to the wrong organisation");
            assertFalse(rs.next(), "only one device was expected");
        }
    }

    private static void assertTableExists(Connection conn, String tableName) throws SQLException {
        try (var rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            assertTrue(rs.next(), () -> tableName + " table must exist");
        }
    }

    private static int countRows(Connection conn, String tableName) throws SQLException {
        try (var st = conn.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
