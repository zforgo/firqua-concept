package io.github.zforgo.firqua.assets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.devices.PagedFilter;
import io.github.zforgo.firqua.filter.FilterResult;

@ApplicationScoped
public class AssetQueryService implements PagedFilter<Asset> {

    private static final Sort DEFAULT_SORT = Sort.ascending(Asset_.NAME).and(Asset_.ID, Sort.Direction.Ascending);

    @Inject
    AssetMapper assetMapper;

    @Override
    public Sort defaultSort() {
        return DEFAULT_SORT;
    }

    @Override
    public PanacheQuery<Asset> baseQuery(Sort sort) {
        return Asset.findAll(sort);
    }

    public AssetDto getById(@NotNull Long id) {
        var asset = Asset.<Asset> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No asset found by id: %d".formatted(id)));
        return assetMapper.toDto(asset);
    }

    public FilterResult<AssetDto> filter(@Valid PagingAndSorting pas) {
        return pagedResult(pas, assetMapper::toDto);
    }
}
