package io.github.zforgo.firqua.devices;

import io.github.zforgo.firqua.assets.AssetType;

public enum DeviceType {

    SOS(AssetType.SOS),
    METEO(AssetType.METEO);

    private final AssetType allowedAssetType;

    DeviceType(AssetType allowedAssetType) {
        this.allowedAssetType = allowedAssetType;
    }

    public boolean isAllowedAssetType(AssetType assetType) {
        return allowedAssetType == assetType;
    }
}
