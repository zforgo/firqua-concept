package io.github.zforgo.firqua.devices;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

@Schema(name = "SOSDeviceCreate",
        allOf = { DeviceCreateDto.class },
        properties = @SchemaProperty(name = "type", enumeration = "SOS"))
public final class SosDeviceCreateDto extends DeviceCreateDto {

    {
        type = DeviceType.SOS;
    }
}
