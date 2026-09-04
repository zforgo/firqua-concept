package io.github.zforgo.firqua.assets;

import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_assetCreateUnion;

@Schema(name = TYPE_assetCreateUnion,
        oneOf = { SosAssetCreateDto.class, MeteoSensorAssetCreateDto.class },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SOS", schema = SosAssetCreateDto.class),
                @DiscriminatorMapping(value = "METEO", schema = MeteoSensorAssetCreateDto.class)
        })
public interface AssetCreateDtoUnion {
}
