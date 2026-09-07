package io.github.zforgo.firqua.devices;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import io.github.zforgo.firqua.assets.MeteoSensorAssetDto;

@Schema(name = "MeteoSensorDevice",
        allOf = { DeviceDto.class },
        properties = @SchemaProperty(name = "type", enumeration = "METEO"))
public final class MeteoSensorDeviceDto extends DeviceDto<MeteoSensorAssetDto> {

    @Override
    public void setAsset(MeteoSensorAssetDto asset) {
        this.asset = asset;
    }
}
