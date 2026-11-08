package io.github.zforgo.firqua.assets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.LaunchMode;

import io.github.zforgo.firqua.common.ReferencedFieldRequiredException;
import io.github.zforgo.firqua.devices.DeviceReferenceResolver;

@IfBuildProfile(anyOf = { LaunchMode.DEV_PROFILE, LaunchMode.TEST_PROFILE })
@ApplicationScoped
public class AssetAdminService {

    @Inject
    AssetMapper assetMapper;

    @Inject
    DeviceReferenceResolver referenceResolver;

    @Inject
    Event<AssetStateChangeEvent> eventManager;

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

        //Update blocked when the asset associated to a device and the IP address field cleared during modification
        var origIp = (asset instanceof IpAddressAware ipAware) ? ipAware.getIpAddress() : null;
        var newIp = (dto instanceof IpAddressAwareDto ipAware) ? ipAware.getIpAddress() : null;
        if (origIp != null && newIp == null && referenceResolver.isAssociatedAsset(asset.id)) {
            throw new ReferencedFieldRequiredException("ipAddress");
        }

        final var enabledChanged = asset.enabled != dto.enabled;

        assetMapper.updateEntity(dto, asset);
        try {
            asset.persistAndFlush();
            if (enabledChanged) {
                eventManager.fire(new AssetStateChangeEvent(asset.id, asset.enabled));
            }
            return assetMapper.toDto(asset);
        } catch (ConstraintViolationException e) {
            throw AssetServiceExceptionHandler.INSTANCE.handleException(e, dto);
        }
    }
}
