package io.github.zforgo.firqua.liquibase.ext;

import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.statement.core.InsertStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsertWithLookupChangeTest {

    private Database database;

    @BeforeEach
    void setUp() throws Exception {
        final var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID());
        try (var st = connection.createStatement()) {
            st.execute("CREATE TABLE relatedTable (id BIGINT PRIMARY KEY, name VARCHAR(50))");
            st.execute("INSERT INTO relatedTable (id, name) VALUES (42, 'foo')");
        }
        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
    }

    @AfterEach
    void tearDown() throws Exception {
        database.close();
    }

    @Test
    @DisplayName("Existing lookup value resolves to the referenced row's id")
    void resolvesForeignKeyFromLookupTable() {
        var change = newChange("foo");

        var statements = change.generateStatements(database);

        assertEquals(1, statements.length);
        var insert = (InsertStatement) statements[0];
        assertEquals(42L, ((Number) insert.getColumnValue("relatedId")).longValue());
    }

    @Test
    @DisplayName("Missing lookup value fails the change")
    void failsWhenLookupValueIsMissing() {
        var change = newChange("missing");

        assertThrows(LookupNotFoundException.class, () -> change.generateStatements(database));
    }

    private static InsertWithLookupChange newChange(String keyValue) {
        var change = new InsertWithLookupChange();
        change.setTableName("targetTable");

        var columnConfig = new LookupColumnConfig();
        columnConfig.setKeyValue(keyValue);
        columnConfig.setKeyColumn("name");
        columnConfig.setIdColumn("id");
        columnConfig.setTargetColumn("relatedId");
        columnConfig.setTable("relatedTable");
        change.addLookupColumn(columnConfig);
        return change;
    }
}
