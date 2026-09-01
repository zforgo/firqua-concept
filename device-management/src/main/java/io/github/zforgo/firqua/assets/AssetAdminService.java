package io.github.zforgo.firqua.assets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;

import io.github.zforgo.firqua.common.NonUniqueIpAddressException;

@ApplicationScoped
class AssetAdminService {

    @Inject
    AssetMapper assetMapper;

    @Transactional
    public AssetDto createAsset(AssetCreateDto dto) {
        var asset = assetMapper.toEntity(dto);
        try {
            asset.persistAndFlush();

            return assetMapper.toDto(asset);
        } catch (ConstraintViolationException e) { //TODO dedup
            if (
                e.getKind() == ConstraintKind.UNIQUE && e.getConstraintName() != null
                        && e.getConstraintName().toUpperCase().contains("PK_ASSETS_IP_ADDRESSES")
            ) {
                throw NonUniqueIpAddressException.byAddress(((IpAddressAware) asset).getIpAddress().getAddress(), e);
            }
            throw e;
        }
    }

    @Transactional
    public AssetDto updateAsset(@NotNull Long id, AssetCreateDto dto) {
        var asset = Asset.<Asset> findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new EntityNotFoundException("No asset found by id: %d".formatted(id)));

        assetMapper.updateEntity(dto, asset);
        try {
            asset.persistAndFlush();

            return assetMapper.toDto(asset);
        } catch (ConstraintViolationException e) { //TODO dedup
            if (
                e.getKind() == ConstraintKind.UNIQUE && e.getConstraintName() != null
                        && e.getConstraintName().toUpperCase().contains("PK_ASSETS_IP_ADDRESSES")
            ) {
                throw NonUniqueIpAddressException.byAddress(((IpAddressAware) asset).getIpAddress().getAddress(), e);
            }
            throw e;
        }
    }
}
