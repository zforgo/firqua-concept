package io.github.zforgo.firqua.test.liquibase;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.arc.Subclass;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
@LiquibaseMigration(runMode = RunMode.PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LiquibaseMigrationCallbackInterceptedTest {

    private static final int bootstrapCount = 3;

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

    @Transactional
    @BeforeEach
    void beforeEach() {
        // for using generated proxied class (@Transactional interceptor binding)
    }

    @RepeatedTest(2)
    @DisplayName("The migration of an intercepted test class runs before @BeforeAll and only once per class")
    void bootstrapOnProxied() {
        assertInstanceOf(Subclass.class, this, "the test class is expected to be intercepted");
        assertEquals(bootstrapCount, actualCount());
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
