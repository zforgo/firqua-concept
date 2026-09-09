package io.github.zforgo.firqua.assets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.LaunchMode;

@IfBuildProfile(anyOf = { LaunchMode.DEV_PROFILE, LaunchMode.TEST_PROFILE })
@ApplicationScoped
public class AssetAdminService {

    @Inject
    AssetMapper assetMapper;

    @Transactional
    public AssetDto createAsset(AssetCreateDto dto) {
        var asset = assetMapper.toEntity(dto);
        try {
            asset.persistAndFlush();
            return assetMapper.toDto(asset);
        } catch (ConstraintViolationException e) {
            throw AssetServiceExceptionHandler.INSTANCE.handleException(e, dto);
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
        } catch (ConstraintViolationException e) {
            throw AssetServiceExceptionHandler.INSTANCE.handleException(e, dto);
        }
    }
}
