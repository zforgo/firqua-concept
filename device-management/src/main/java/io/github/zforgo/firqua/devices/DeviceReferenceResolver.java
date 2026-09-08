package io.github.zforgo.firqua.devices;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.organisations.OrganisationUnit;

@ApplicationScoped
class DeviceReferenceResolver {

    @Transactional
    <A extends Asset> A toAsset(Long id) {
        return Asset.<A> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No asset found by id: %d".formatted(id)));
    }

    @Transactional
    OrganisationUnit toOrganisationUnit(Long id) {
        return OrganisationUnit.<OrganisationUnit> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No organisation unit found by id: %d".formatted(id)));
    }
}
