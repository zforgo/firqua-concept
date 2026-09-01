package io.github.zforgo.firqua.assets;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SosAssetCreateDto.class, name = "SOS"),
        @JsonSubTypes.Type(value = MeteoSensorAssetCreateDto.class, name = "METEO")
})
@Schema(name = "AssetCreate", discriminatorProperty = "type")
public sealed abstract class AssetCreateDto extends AssetCommonFields permits SosAssetCreateDto, MeteoSensorAssetCreateDto {

}
