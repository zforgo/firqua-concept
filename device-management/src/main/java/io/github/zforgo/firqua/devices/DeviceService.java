package io.github.zforgo.firqua.devices;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.common.IncompatibleAssetTypeException;
import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;

import static io.github.zforgo.firqua.devices.DeviceServiceExceptionHandler.handleException;

//TODo dedup
@ApplicationScoped
public class DeviceService implements PagedFilter<Device<? extends Asset>> {

    private static final Sort DEFAULT_SORT = Sort.ascending(Device_.NAME).and(Device_.ID, Sort.Direction.Ascending);

    @Inject
    DeviceMapper deviceMapper;

    @Override
    public Sort defaultSort() {
        return DEFAULT_SORT;
    }

    @Override
    public PanacheQuery<Device<? extends Asset>> baseQuery(Sort sort) {
        return Device.findAll(sort);
    }

    public DeviceDto<? extends AssetDto> getById(@NotNull Long id) {
        var device = Device.<Device<? extends Asset>> findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("No device found by id: %d".formatted(id)));
        return deviceMapper.toDto(device);
    }

    public FilterResult<DeviceDto<? extends AssetDto>> filter(@Valid PagingAndSorting pas) {
        return pagedResult(pas, deviceMapper::toDto);
    }

    @Transactional
    public DeviceDto<? extends AssetDto> createDevice(DeviceCreateDto dto) {
        try {
            var entity = deviceMapper.toEntity(dto);
            entity.persistAndFlush();
            return deviceMapper.toDto(entity);

        } catch (ClassCastException e) {
            throw new IncompatibleAssetTypeException(dto.assetId, dto.type);
        } catch (ConstraintViolationException e) {
            throw handleException(e, dto);
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
        } catch (ConstraintViolationException e) {
            throw handleException(e, dto);
        }
    }
}
