package io.github.zforgo.firqua.assets;

import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_assetUnion;

@Schema(name = TYPE_assetUnion,
        oneOf = { SosAssetDto.class, MeteoSensorAssetDto.class },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SOS", schema = SosAssetDto.class),
                @DiscriminatorMapping(value = "METEO", schema = MeteoSensorAssetDto.class)
        })
public interface AssetDtoUnion {}
