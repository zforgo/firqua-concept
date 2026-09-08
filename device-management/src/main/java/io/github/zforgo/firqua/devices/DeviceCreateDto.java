package io.github.zforgo.firqua.devices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SosDeviceCreateDto.class, name = "SOS"),
        @JsonSubTypes.Type(value = MeteoSensorDeviceCreateDto.class, name = "METEO")
})
@Schema(name = "DeviceCreate", discriminatorProperty = "type")
public abstract sealed class DeviceCreateDto permits SosDeviceCreateDto, MeteoSensorDeviceCreateDto {

    @NotNull
    public DeviceType type;

    @NotBlank
    public String name;

    @NotNull
    public Long assetId;

    @NotNull
    public Long organisationId;
}
