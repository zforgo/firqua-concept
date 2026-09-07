package io.github.zforgo.firqua.devices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.organisations.OrganisationUnitDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SosDeviceDto.class, name = "SOS"),
        @JsonSubTypes.Type(value = MeteoSensorDeviceDto.class, name = "METEO")
})
@Schema(name = "Device", discriminatorProperty = "type")
public sealed abstract class DeviceDto<T extends AssetDto> permits SosDeviceDto, MeteoSensorDeviceDto {

    @NotNull
    public Long id;

    @NotNull
    public DeviceType type;

    @NotBlank
    public String name;

    @NotNull
    public T asset;

    @NotNull
    public OrganisationUnitDto organisationUnit;

    public abstract void setAsset(T asset);
}
