package io.github.zforgo.firqua.assets;

import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SosAssetDto.class, name = "SOS"),
        @JsonSubTypes.Type(value = MeteoSensorAssetDto.class, name = "METEO")
})
@Schema(name = "Asset", discriminatorProperty = "type")
public sealed abstract class AssetDto extends AssetCommonFields permits SosAssetDto, MeteoSensorAssetDto {

    @NotNull
    public Long id;
}
