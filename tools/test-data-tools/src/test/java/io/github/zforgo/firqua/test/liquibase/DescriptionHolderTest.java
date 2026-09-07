package io.github.zforgo.firqua.test.liquibase;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptionHolderTest {

    private static class OnAnotherThread {}

    private static class Discarded {}

    private static class Kept {}

    @Test
    @DisplayName("A description stored on one thread is visible from another one")
    void treadSafeStore() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> DescriptionHolder.store(OnAnotherThread.class, null)).get();
        }
        assertTrue(DescriptionHolder.hasClass(OnAnotherThread.class));
    }

    @Test
    @DisplayName("Discarding a class drops its descriptions and keeps the ones of every other class")
    void discardOnlyGivenClass(TestInfo testInfo) {
        var method = testInfo.getTestMethod().orElseThrow();
        DescriptionHolder.store(Discarded.class, null);
        DescriptionHolder.store(Discarded.class, method);
        DescriptionHolder.store(Kept.class, method);

        DescriptionHolder.discard(Discarded.class);

        assertFalse(DescriptionHolder.hasClass(Discarded.class));
        assertFalse(DescriptionHolder.hasMethod(Discarded.class, method));
        assertTrue(DescriptionHolder.hasMethod(Kept.class, method));
    }
}
