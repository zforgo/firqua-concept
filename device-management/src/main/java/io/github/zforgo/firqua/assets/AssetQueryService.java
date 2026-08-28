package io.github.zforgo.firqua.assets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;

import io.github.zforgo.firqua.common.FilterResult;

@ApplicationScoped
public class AssetQueryService {

    @Inject
    AssetMapper assetMapper;

    public AssetDto getById(@NotNull Long id) {
        var asset = Asset.<Asset> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No asset found by id: %d".formatted(id)));
        return assetMapper.toDto(asset);
    }

    public FilterResult<AssetDto> filter(int pageIndex, int pageSize) {
        var baseQuery = Asset.<Asset> findAll()
                .page(pageIndex, pageSize);
        var totalCount = baseQuery.count();
        var items = baseQuery.stream().map(assetMapper::toDto)
                .toList();
        return new FilterResult<>(items, totalCount, pageIndex, pageSize);
    }
}
