package io.github.zforgo.firqua.devices;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.assets.AssetMapper;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

@Mapper(componentModel = JAKARTA_CDI, uses = { AssetMapper.class, DeviceReferenceResolver.class })
public interface DeviceMapper {

    SosDeviceDto toDto(SosDevice entity);

    MeteoSensorDeviceDto toDto(MeteoSensorDevice entity);

    default DeviceDto<? extends AssetDto> toDto(Device<? extends Asset> entity) {
        return switch (entity) {
            case null -> null;
            case SosDevice sos -> toDto(sos);
            case MeteoSensorDevice meteo -> toDto(meteo);
            default -> throw new IllegalArgumentException("Unsupported device type: " + entity.getClass().getName());
        };
    }

    @Mapping(target = "asset", source = "assetId")
    @Mapping(target = "organisationUnit", source = "organisationId")
    @Mapping(target = "id", ignore = true)
    MeteoSensorDevice toEntity(MeteoSensorDeviceCreateDto dto);

    @Mapping(target = "asset", source = "assetId")
    @Mapping(target = "organisationUnit", source = "organisationId")
    @Mapping(target = "id", ignore = true)
    SosDevice toEntity(SosDeviceCreateDto dto);

    default Device<? extends Asset> toEntity(DeviceCreateDto dto) {
        return switch (dto) {
            case SosDeviceCreateDto sos -> toEntity(sos);
            case MeteoSensorDeviceCreateDto meteo -> toEntity(meteo);
        };

    }

    @Mapping(target = "asset", source = "assetId")
    @Mapping(target = "organisationUnit", source = "organisationId")
    @Mapping(target = "id", ignore = true)
    void updateEntity(SosDeviceCreateDto dto, @MappingTarget SosDevice entity);

    @Mapping(target = "asset", source = "assetId")
    @Mapping(target = "organisationUnit", source = "organisationId")
    @Mapping(target = "id", ignore = true)
    void updateEntity(MeteoSensorDeviceCreateDto dto, @MappingTarget MeteoSensorDevice entity);

    default void updateEntity(DeviceCreateDto dto, @MappingTarget Device<? extends Asset> entity) {
        switch (dto) {
            case SosDeviceCreateDto sos when entity instanceof SosDevice target -> updateEntity(sos, target);
            case MeteoSensorDeviceCreateDto meteo when entity instanceof MeteoSensorDevice target ->
                updateEntity(meteo, target);
            default -> throw new IllegalArgumentException(
                    "Cannot apply %s on %s, device type is immutable"
                            .formatted(dto.getClass().getSimpleName(), entity.getClass().getSimpleName())
            );
        }
    }
}
