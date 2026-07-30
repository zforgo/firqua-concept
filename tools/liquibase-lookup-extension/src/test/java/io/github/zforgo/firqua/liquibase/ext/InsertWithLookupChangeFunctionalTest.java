package io.github.zforgo.firqua.liquibase.ext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

class InsertWithLookupChangeFunctionalTest {

    @Test
    void changeLog() throws SQLException, LiquibaseException {
        final var jdbcUrl = "jdbc:h2:mem:" + getClass().getSimpleName() + ";DB_CLOSE_DELAY=-1";
        try (var conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            var liquibase = new Liquibase(
                    "changelogs/sample-changelog.xml",
                    new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),
                    database);
            liquibase.update("");

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
