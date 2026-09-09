package io.github.zforgo.firqua.common;

import java.util.Map;
import java.util.function.BiFunction;

import org.hibernate.exception.ConstraintViolationException;

public interface ServiceExceptionHandler<T> {

    Map<String, BiFunction<T, Throwable, RuntimeException>> constraints();

    default RuntimeException handleException(ConstraintViolationException e, T obj) {
        if (obj == null || e.getConstraintName() == null) {
            return e;
        }
        var constraintName = e.getConstraintName().toUpperCase();
        return constraints().entrySet().stream()
                .filter(entry -> constraintName.contains(entry.getKey()))
                .findFirst()
                .map(entry -> entry.getValue().apply(obj, e))
                .orElse(e);
    }
}
