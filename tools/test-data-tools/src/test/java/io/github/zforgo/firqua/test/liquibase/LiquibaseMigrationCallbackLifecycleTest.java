package io.github.zforgo.firqua.test.liquibase;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LiquibaseMigrationCallbackLifecycleTest {

    private static final int bootstrapCount = 3;
    private static final int repetitionInsertCount = 5;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    DataSource dataSource;

    @BeforeAll
    void init() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO sample_table(id, name) VALUES (?, ?)");
            for (int i = 0; i < bootstrapCount; i++) {
                statement.clearParameters();
                statement.setString(1, "BOOT_ID_" + i);
                statement.setString(2, "BOOT_NAME_" + i);
                statement.execute();
            }
        }
    }

    @BeforeEach
    void beforeEach() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO sample_table(id, name) VALUES (?, ?)");
            for (int i = 0; i < repetitionInsertCount; i++) {
                var suffix = "" + System.nanoTime();
                statement.clearParameters();
                statement.setString(1, "REP_ID_%s_%s".formatted(suffix, i));
                statement.setString(2, "REP_NAME_%s_%s".formatted(suffix, i));
                statement.execute();
            }
        }

    }

    @RepeatedTest(5)
    void insertsExecuted(RepetitionInfo repetitionInfo) {
        var rep = repetitionInfo.getCurrentRepetition();
        var expected = rep * repetitionInsertCount + bootstrapCount;
        assertEquals(expected, actualCount());
    }

    private long actualCount() {
        var sql = "select count(id) from sample_table";
        try (
                var conn = dataSource.getConnection();
                var st = conn.createStatement();
                var rs = st.executeQuery(sql)
        ) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
