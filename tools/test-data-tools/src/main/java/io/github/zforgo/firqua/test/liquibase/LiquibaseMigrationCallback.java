package io.github.zforgo.firqua.test.liquibase;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Subclass;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.LiquibaseFactoryUtil;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import liquibase.Contexts;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.exception.LiquibaseException;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class LiquibaseMigrationCallback
        implements QuarkusTestBeforeEachCallback, QuarkusTestAfterConstructCallback, QuarkusTestAfterAllCallback {

    @Override
    public void afterConstruct(Object testInstance) {
        var testClass = testClassOf(testInstance);
        var ann = testClass.getDeclaredAnnotation(LiquibaseMigration.class);
        if (ann != null && ann.runMode() == RunMode.PER_CLASS) {
            try {
                if (!DescriptionHolder.hasClass(testClass)) {
                    run(ann);
                }
            } finally {
                DescriptionHolder.store(testClass, null);
            }
        }
    }

    @Override
    public void beforeEach(QuarkusTestMethodContext ctx) {
        var testClass = testClassOf(ctx.getTestInstance());
        try {
            configOf(testClass, ctx.getTestMethod())
                    .filter(ann -> needsRun(ann, testClass, ctx.getTestMethod()))
                    .ifPresent(LiquibaseMigrationCallback::run);
        } finally {
            DescriptionHolder.store(testClass, ctx.getTestMethod());
        }
    }

    @Override
    public void afterAll(QuarkusTestContext ctx) {
        Optional.ofNullable(ctx.getTestInstance())
                .map(LiquibaseMigrationCallback::testClassOf)
                .ifPresent(DescriptionHolder::discard);
    }

    private static void run(LiquibaseMigration ann) {
        var factory = factoryOf(ann.datasource());

        try (var liquibase = factory.createLiquibase()) {
            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
            if (ann.dropFirst()) {
                liquibase.dropAll();
            }
            liquibase.update(populateContext(factory.createContexts(), ann.additionalContexts()), factory.createLabels());
        } catch (LiquibaseException e) {
            throw new IllegalStateException(
                    "Unable to run Liquibase migration for datasource '%s'".formatted(ann.datasource()), e
            );
        }
    }

    private static Class<?> testClassOf(Object testInstance) {
        var testClass = testInstance.getClass();
        return testInstance instanceof Subclass ? testClass.getSuperclass() : testClass;
    }

    private static Optional<LiquibaseMigration> configOf(Class<?> testClass, Method testMethod) {
        return findAnnotation(testMethod, LiquibaseMigration.class)
                .or(() -> Optional.ofNullable(testClass.getDeclaredAnnotation(LiquibaseMigration.class)));
    }

    private static LiquibaseFactory factoryOf(String datasource) {
        return Optional.of(LiquibaseFactoryUtil.getLiquibaseFactory(datasource))
                .filter(InstanceHandle::isAvailable)
                .map(InstanceHandle::get)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No Liquibase factory found for datasource '%s'".formatted(datasource)
                        )
                );
    }

    private static boolean needsRun(LiquibaseMigration ann, Class<?> testClass, Method testMethod) {
        return switch (ann.runMode()) {
            case ALWAYS -> true;
            case PER_CLASS -> !DescriptionHolder.hasClass(testClass);
            case PER_METHOD -> !DescriptionHolder.hasMethod(testClass, testMethod);
        };
    }

    private static Contexts populateContext(Contexts contexts, String... additionalContexts) {
        Arrays.stream(additionalContexts).forEach(contexts::add);
        return contexts;
    }
}
