package io.github.zforgo.firqua.assets;

import java.util.Map;
import java.util.function.BiFunction;

import io.github.zforgo.firqua.common.ConflictException;
import io.github.zforgo.firqua.common.NonUniqueIpAddressException;
import io.github.zforgo.firqua.common.ServiceExceptionHandler;

public final class AssetServiceExceptionHandler implements ServiceExceptionHandler<AssetCreateDto> {

    public static final AssetServiceExceptionHandler INSTANCE = new AssetServiceExceptionHandler();

    private final Map<String, BiFunction<AssetCreateDto, Throwable, RuntimeException>> UNIQUE_CONSTRAINTS = Map
            .of(
                    "UQ_ASSETS_NAME", (dto, cause) -> new ConflictException("name", dto.name, cause),
                    "_ASSETS_STATION_ID",
                    (dto, cause) -> new ConflictException("stationId", ((StationAwareDto) dto).getStationId(), cause),
                    "PK_ASSETS_IP_ADDRESSES",
                    (dto, cause) -> NonUniqueIpAddressException.byAddress(((IpAddressAwareDto) dto).getIpAddress(), cause)
            );

    private AssetServiceExceptionHandler() {
        // avoid direct instantiation
    }

    @Override
    public Map<String, BiFunction<AssetCreateDto, Throwable, RuntimeException>> constraints() {
        return UNIQUE_CONSTRAINTS;
    }
}
