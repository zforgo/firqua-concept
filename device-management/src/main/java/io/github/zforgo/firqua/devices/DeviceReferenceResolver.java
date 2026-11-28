package io.github.zforgo.firqua.devices;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.common.IncompatibleAssetTypeException;
import io.github.zforgo.firqua.organisations.OrganisationUnit;

@ApplicationScoped
public class DeviceReferenceResolver {

    @Transactional
    public <A extends Asset> A toAsset(DeviceCreateDto dto) {
        var asset = Asset.<A> findByIdOptional(dto.assetId)
                .orElseThrow(() -> new EntityNotFoundException("No asset found by id: %d".formatted(dto.assetId)));
        if (!dto.type.isAllowedAssetType(asset.type)) {
            throw new IncompatibleAssetTypeException(dto.assetId, dto.type);
        }
        return asset;
    }

    @Transactional
    public OrganisationUnit toOrganisationUnit(Long id) {
        return OrganisationUnit.<OrganisationUnit> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No organisation unit found by id: %d".formatted(id)));
    }

    public boolean isAssociatedAsset(Long assetId) {
        return Device.count("asset.id", assetId) > 0;
    }
}
