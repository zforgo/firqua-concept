package io.github.zforgo.firqua.assets;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collector;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class AssetQueryService {

    private static final Sort DEFAULT_SORT = Sort.ascending(Asset_.NAME).and(Asset_.ID, Sort.Direction.Ascending);

    @Inject
    AssetMapper assetMapper;

    public AssetDto getById(@NotNull Long id) {
        var asset = Asset.<Asset> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No asset found by id: %d".formatted(id)));
        return assetMapper.toDto(asset);
    }

    public FilterResult<AssetDto> filter(@Valid PagingAndSorting pas) {
        var finalSort = Optional.ofNullable(pas.sortingCriteria)
                .map(cr -> Sort.by(cr, pas.sortDirection))
                .map(s -> mergeSort(s, DEFAULT_SORT))
                .orElse(DEFAULT_SORT);

        var baseQuery = Asset.<Asset> findAll(finalSort);

        Optional.of(pas)
                .filter(ps -> ps.pageSize > 0)
                .map(ps -> Page.of(ps.pageIndex, ps.pageSize))
                .ifPresent(baseQuery::page);

        var totalCount = baseQuery.count();
        var items = baseQuery.stream()
                .map(assetMapper::toDto)
                .toList();
        return new FilterResult<>(items, totalCount, pas.pageIndex, pas.pageSize);
    }

    private static Sort mergeSort(Sort... parts) {
        return Arrays.stream(parts)
                .flatMap(part -> part.getColumns().stream())
                .collect(
                        Collector.of(
                                Sort::empty,
                                (s, c) -> s.and(c.getName(), c.getDirection(), c.getNullPrecedence()),
                                (a, _) -> a
                        )
                );
    }

}
