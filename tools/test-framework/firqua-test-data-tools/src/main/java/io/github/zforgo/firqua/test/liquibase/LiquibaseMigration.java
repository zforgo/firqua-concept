package io.github.zforgo.firqua.test.liquibase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.datasource.common.runtime.DataSourceUtil;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface LiquibaseMigration {

    boolean dropFirst() default false;

    RunMode runMode() default RunMode.PER_CLASS;

    String datasource() default DataSourceUtil.DEFAULT_DATASOURCE_NAME;

    String[] additionalContexts() default {};
}
