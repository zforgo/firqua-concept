package io.github.zforgo.firqua.devices;

import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_deviceCreateUnion;

@Schema(name = TYPE_deviceCreateUnion,
        oneOf = { SosDeviceCreateDto.class, MeteoSensorDeviceCreateDto.class },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SOS", schema = SosDeviceCreateDto.class),
                @DiscriminatorMapping(value = "METEO", schema = MeteoSensorDeviceCreateDto.class)
        })
public interface DeviceCreateDtoUnion {}
