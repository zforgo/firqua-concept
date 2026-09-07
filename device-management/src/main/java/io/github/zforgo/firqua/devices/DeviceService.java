package io.github.zforgo.firqua.devices;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collector;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.common.IncompatibleAssetTypeException;
import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;

//TODO get rid of generic warning
//TODo dedup
@ApplicationScoped
public class DeviceService {

    private static final Sort DEFAULT_SORT = Sort.ascending(Device_.NAME).and(Device_.ID, Sort.Direction.Ascending);

    @Inject
    DeviceMapper deviceMapper;

    public DeviceDto<? extends AssetDto> getById(@NotNull Long id) {
        var device = Device.<Device<? extends Asset>> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No device found by id: %d".formatted(id)));
        return deviceMapper.toDto(device);
    }

    public FilterResult<DeviceDto<? extends AssetDto>> filter(@Valid PagingAndSorting pas) {
        var finalSort = Optional.ofNullable(pas.sortingCriteria)
                .map(cr -> Sort.by(cr, pas.sortDirection))
                .map(s -> mergeSort(s, DEFAULT_SORT))
                .orElse(DEFAULT_SORT);

        var baseQuery = Device.<Device> findAll(finalSort);

        Optional.of(pas)
                .filter(ps -> ps.pageSize > 0)
                .map(ps -> Page.of(ps.pageIndex, ps.pageSize))
                .ifPresent(baseQuery::page);

        var totalCount = baseQuery.count();
        var items = baseQuery.stream()
                .<DeviceDto<? extends AssetDto>> map(deviceMapper::toDto)
                .toList();
        return new FilterResult<>(items, totalCount, pas.pageIndex, pas.pageSize);
    }

    @Transactional
    public DeviceDto<? extends AssetDto> createDevice(DeviceCreateDto dto) {
        try {
            var entity = deviceMapper.toEntity(dto);
            entity.persistAndFlush();
            return deviceMapper.toDto(entity);

        } catch (ClassCastException e) {
            throw new IncompatibleAssetTypeException(dto.assetId, dto.type);
        }
    }

    @Transactional
    public DeviceDto<? extends AssetDto> updateDevice(@NotNull Long id, @NotNull @Valid DeviceCreateDto dto) {
        var device = Device.<Device<? extends Asset>> findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new EntityNotFoundException("No device found by id: %d".formatted(id)));
        try {
            deviceMapper.updateEntity(dto, device);
            device.persistAndFlush();
            return deviceMapper.toDto(device);
        } catch (ClassCastException e) {
            throw new IncompatibleAssetTypeException(dto.assetId, dto.type);
        }
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
