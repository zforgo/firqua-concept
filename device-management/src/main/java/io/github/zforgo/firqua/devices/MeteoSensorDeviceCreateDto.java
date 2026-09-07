package io.github.zforgo.firqua.devices;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

@Schema(name = "MeteoSensorDeviceCreate",
        allOf = { DeviceCreateDto.class },
        properties = @SchemaProperty(name = "type", enumeration = "METEO"))
public final class MeteoSensorDeviceCreateDto extends DeviceCreateDto {

    {
        type = DeviceType.METEO;
    }
}
