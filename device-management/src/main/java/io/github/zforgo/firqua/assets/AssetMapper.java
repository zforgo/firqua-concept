package io.github.zforgo.firqua.assets;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = JAKARTA_CDI)
public interface AssetMapper {

    @IgnoredMapperFields
    SosAsset toEntity(SosAssetCreateDto dto);

    @IgnoredMapperFields
    MeteoAsset toEntity(MeteoSensorAssetCreateDto dto);

    default Asset toEntity(AssetCreateDto dto) {
        return switch (dto) {
            case SosAssetCreateDto sos -> toEntity(sos);
            case MeteoSensorAssetCreateDto meteo -> toEntity(meteo);
        };
    }

    @IgnoredMapperFields
    void updateEntity(SosAssetCreateDto dto, @MappingTarget SosAsset entity);

    @IgnoredMapperFields
    void updateEntity(MeteoSensorAssetCreateDto dto, @MappingTarget MeteoAsset entity);

    default void updateEntity(AssetCreateDto dto, @MappingTarget Asset entity) {
        switch (dto) {
            case SosAssetCreateDto sos when entity instanceof SosAsset target -> updateEntity(sos, target);
            case MeteoSensorAssetCreateDto meteo when entity instanceof MeteoAsset target -> updateEntity(meteo, target);
            default -> throw new IllegalArgumentException("Cannot apply %s on %s, asset type is immutable"
                    .formatted(dto.getClass().getSimpleName(), entity.getClass().getSimpleName()));
        }
    }

    default String map(Ipv4Address ipAddress) {
        return ipAddress == null ? null : ipAddress.getAddress();
    }

    SosAssetDto toDto(SosAsset entity);

    MeteoSensorAssetDto toDto(MeteoAsset entity);

    default AssetDto toDto(Asset entity) {
        return switch (entity) {
            case null -> null;
            case SosAsset sos -> toDto(sos);
            case MeteoAsset meteo -> toDto(meteo);
            default -> throw new IllegalArgumentException("Unsupported asset type: " + entity.getClass().getName());
        };
    }

    @AfterMapping
    default void updateIpAddress(IpAddressAwareDto dto, @MappingTarget IpAddressAware entity) {
        updateIpAddress(dto.getIpAddress(), entity);
    }

    private void updateIpAddress(String newIp, IpAddressAware entity) {
        var current = entity.getIpAddress();
        if (newIp == null) {
            entity.setIpAddress(null);
            return;
        }
        if (current != null && newIp.equals(current.getAddress())) {
            return;
        }
        entity.setIpAddress(new Ipv4Address(newIp));
    }
}
