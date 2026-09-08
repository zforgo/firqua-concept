package io.github.zforgo.firqua.devices;

import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_deviceUnion;

@Schema(name = TYPE_deviceUnion,
        oneOf = { SosDeviceDto.class, MeteoSensorDeviceDto.class },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SOS", schema = SosDeviceDto.class),
                @DiscriminatorMapping(value = "METEO", schema = MeteoSensorDeviceDto.class)
        })
public interface DeviceDtoUnion {}
