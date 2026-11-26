package io.github.zforgo.firqua.test.liquibase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
class CallbackVerbosityTest {

    @LiquibaseMigration(additionalContexts = "verbose-failed")
    static class FailingMigration {}

    private final CapturingHandler handler = new CapturingHandler();
    private final Logger liquibaseLogger = Logger.getLogger("liquibase");

    @BeforeEach
    void attachHandler() {
        liquibaseLogger.addHandler(handler);
    }

    @AfterEach
    void detachHandler() {
        liquibaseLogger.removeHandler(handler);
        DescriptionHolder.discard(FailingMigration.class);
    }

    @Test
    @DisplayName("Redirected Liquibase output to the logger still reports a failing migration")
    void failedLogged() {
        var callback = new LiquibaseMigrationCallback();
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var originalOut = System.out;
        var originalErr = System.err;

        JUnitException ex;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            ex = assertThrows(JUnitException.class, () -> callback.afterConstruct(new FailingMigration()));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        assertAll(
                () -> assertTrue(
                        ex.getMessage().contains("Unable to run Liquibase migration"), "unexpected message: " + ex.getMessage()
                ),
                () -> assertEquals("", out.toString(StandardCharsets.UTF_8), "Liquibase must not write to stdout"),
                () -> assertEquals("", err.toString(StandardCharsets.UTF_8), "Liquibase must not write to stderr"),
                () -> assertTrue(
                        handler.records.stream().anyMatch(
                                r -> r.getLevel().intValue() >= Level.SEVERE.intValue()
                                        && r.getMessage().contains("always_failing_changeset")
                        ),
                        "the failing changeset is expected to be logged as an error"
                )
        );
    }

    private static final class CapturingHandler extends Handler {

        final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
