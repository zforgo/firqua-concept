package io.github.zforgo.firqua.devices;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import io.github.zforgo.firqua.assets.SosAssetDto;

@Schema(name = "SOSDevice",
        allOf = { DeviceDto.class },
        properties = @SchemaProperty(name = "type", enumeration = "SOS"))
public final class SosDeviceDto extends DeviceDto<SosAssetDto> {

    @Override
    public void setAsset(SosAssetDto asset) {
        this.asset = asset;
    }
}
