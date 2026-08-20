package io.github.zforgo.firqua.test.liquibase;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.LiquibaseFactoryUtil;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import liquibase.Contexts;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.exception.LiquibaseException;

public class LiquibaseMigrationCallback implements QuarkusTestBeforeEachCallback {

    private static final Function<QuarkusTestMethodContext, Optional<LiquibaseMigration>> getConfig = ctx -> findAnnotation(
            ctx.getTestMethod(), LiquibaseMigration.class)
            .or(() -> Optional.ofNullable(ctx.getTestInstance().getClass().getDeclaredAnnotation(LiquibaseMigration.class)));

    @Override
    public void beforeEach(QuarkusTestMethodContext ctx) {
        try {
            getConfig
                    .apply(ctx)
                    .filter(ann -> needsRun(ann, ctx))
                    .ifPresent(LiquibaseMigrationCallback::run);
        } finally {
            DescriptionHolder.store(ctx.getTestInstance().getClass(), ctx.getTestMethod());
        }
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
                    "Unable to run Liquibase migration for datasource '%s'".formatted(ann.datasource()), e);
        }
    }

    private static LiquibaseFactory factoryOf(String datasource) {
        return Optional.of(LiquibaseFactoryUtil.getLiquibaseFactory(datasource))
                .filter(InstanceHandle::isAvailable)
                .map(InstanceHandle::get)
                .orElseThrow(() -> new IllegalStateException(
                        "No Liquibase factory found for datasource '%s'".formatted(datasource)));
    }

    private static boolean needsRun(LiquibaseMigration ann, QuarkusTestMethodContext ctx) {
        return switch (ann.runMode()) {
            case ALWAYS -> true;
            case PER_CLASS -> !DescriptionHolder.hasClass(ctx.getTestInstance().getClass());
            case PER_METHOD -> !DescriptionHolder.hasMethod(ctx.getTestInstance().getClass(), ctx.getTestMethod());
        };
    }

    private static Contexts populateContext(Contexts contexts, String... additionalContexts) {
        Arrays.stream(additionalContexts).forEach(contexts::add);
        return contexts;
    }
}
