package io.github.zforgo.firqua.common;

import io.github.zforgo.firqua.devices.DeviceType;

public class IncompatibleAssetTypeException extends UnexpectedValueException {

    public IncompatibleAssetTypeException(Long id, DeviceType expected) {
        super("assetId", id, expected);
    }

    @Override
    protected String buildMessage(String key, Object value, Object expected) {
        return "Asset %s cannot be attached to device type %s: incompatible asset type".formatted(value, expected);
    }
}
