package io.github.zforgo.firqua.common;

import io.github.zforgo.firqua.devices.DeviceType;

//TODO mapper (moreover extend a ConflictException)
public class IncompatibleAssetTypeException extends RuntimeException {

    private final Long assetId;
    private final String expectedType;

    public IncompatibleAssetTypeException(Long id, DeviceType expected) {
        this.assetId = id;
        this.expectedType = expected.toString();
        super("Asset %d cannot be attached to device type %s: incompatible asset type".formatted(id, expected));
    }

    public Long getAssetId() {
        return assetId;
    }

    public String getExpectedType() {
        return expectedType;
    }
}
