package io.github.zforgo.firqua.devices;

import java.util.Map;
import java.util.function.BiFunction;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;

import io.github.zforgo.firqua.common.ConflictException;

public final class DeviceServiceExceptionHandler {

    private static final Map<String, BiFunction<DeviceCreateDto, Throwable, ConflictException>> UNIQUE_CONSTRAINTS = Map
            .of(
                    "UQ_DEVICES_NAME", (dto, cause) -> new ConflictException("name", dto.name, cause),
                    "UQ_DEVICES_ASSET", (dto, cause) -> new ConflictException("assetId", dto.assetId, cause)
            );

    private DeviceServiceExceptionHandler() {
        // avoid direct instantiation
    }

    static RuntimeException handleException(ConstraintViolationException e, DeviceCreateDto obj) {
        if (obj == null || e.getKind() != ConstraintKind.UNIQUE || e.getConstraintName() == null) {
            return e;
        }
        var constraintName = e.getConstraintName().toUpperCase();

        return UNIQUE_CONSTRAINTS.entrySet().stream()
                .filter(entry -> constraintName.contains(entry.getKey()))
                .findFirst()
                .<RuntimeException> map(entry -> entry.getValue().apply(obj, e))
                .orElse(e);
    }
}
