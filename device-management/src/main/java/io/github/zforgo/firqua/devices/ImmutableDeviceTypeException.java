package io.github.zforgo.firqua.devices;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.common.ConflictException;

public class ImmutableDeviceTypeException extends ConflictException {

    public ImmutableDeviceTypeException(DeviceCreateDto dto, Device<? extends Asset> entity) {
        super(
                "Cannot apply %s on %s, device type is immutable"
                        .formatted(dto.getClass().getSimpleName(), entity.getClass().getSimpleName())
        );
    }
}
