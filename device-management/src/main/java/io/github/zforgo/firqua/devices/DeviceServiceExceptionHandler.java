package io.github.zforgo.firqua.devices;

import java.util.Map;
import java.util.function.BiFunction;

import io.github.zforgo.firqua.common.ConflictException;
import io.github.zforgo.firqua.common.ServiceExceptionHandler;

public final class DeviceServiceExceptionHandler implements ServiceExceptionHandler<DeviceCreateDto> {

    public static final DeviceServiceExceptionHandler INSTANCE = new DeviceServiceExceptionHandler();

    private final Map<String, BiFunction<DeviceCreateDto, Throwable, RuntimeException>> UNIQUE_CONSTRAINTS = Map
            .of(
                    "UQ_DEVICES_NAME", (dto, cause) -> new ConflictException("name", dto.name, cause),
                    "UQ_DEVICES_ASSET", (dto, cause) -> new ConflictException("assetId", dto.assetId, cause)
            );

    private DeviceServiceExceptionHandler() {
        // avoid direct instantiation
    }

    @Override
    public Map<String, BiFunction<DeviceCreateDto, Throwable, RuntimeException>> constraints() {
        return UNIQUE_CONSTRAINTS;
    }
}
