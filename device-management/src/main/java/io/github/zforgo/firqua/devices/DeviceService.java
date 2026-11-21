package io.github.zforgo.firqua.devices;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.assets.ReferencedAssetChangeEvent;
import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.devices.events.DeviceStateChanged;
import io.github.zforgo.firqua.filter.FilterResult;
import io.github.zforgo.firqua.hibernate.DirtyProperties;

@ApplicationScoped
public class DeviceService implements PagedFilter<Device<? extends Asset>> {

    private static final Set<String> changeIndicator = Set
            .of("name", "enabled", "asset", "organisationUnit", "effectiveEnabled");

    private static final Sort DEFAULT_SORT = Sort.ascending(Device_.NAME).and(Device_.ID, Sort.Direction.Ascending);

    @Inject
    DeviceMapper deviceMapper;

    @Inject
    Event<DeviceStateChanged> deviceChangedEvent;

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
            deviceChangedEvent.fire(DeviceStateChanged.created(entity.id));
            return deviceMapper.toDto(entity);
        } catch (ConstraintViolationException e) {
            throw DeviceServiceExceptionHandler.INSTANCE.handleException(e, dto);
        }
    }

    @Transactional
    public DeviceDto<? extends AssetDto> updateDevice(@NotNull Long id, @NotNull @Valid DeviceCreateDto dto) {
        var device = Device.<Device<? extends Asset>> findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new EntityNotFoundException("No device found by id: %d".formatted(id)));
        try {
            deviceMapper.updateEntity(dto, device);
            device.evaluateState();
            var changes = DirtyProperties.collect(device);
            device.persistAndFlush();

            if (needsIndicate(changes)) {
                deviceChangedEvent.fire(DeviceStateChanged.updated(device.id));
            }
            return deviceMapper.toDto(device);
        } catch (ConstraintViolationException e) {
            throw DeviceServiceExceptionHandler.INSTANCE.handleException(e, dto);
        }
    }

    @Transactional(TxType.MANDATORY)
    void handleStateChange(@Observes ReferencedAssetChangeEvent event) {
        Device.<Device<?>> find("asset.id", event.assetId())
                .stream()
                .forEach(device -> {
                    device.evaluateState();
                    deviceChangedEvent.fire(DeviceStateChanged.updated(device.id));
                });
    }

    private static boolean needsIndicate(Set<String> changes) {
        return !changes.isEmpty() && changes.stream().anyMatch(changeIndicator::contains);
    }

}
