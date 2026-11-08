package io.github.zforgo.firqua.assets;

import io.github.zforgo.firqua.common.ConflictException;

public class ImmutableAssetTypeException extends ConflictException {

    public ImmutableAssetTypeException(AssetCreateDto dto, Asset entity) {
        super(
                "Cannot apply %s on %s, asset type is immutable"
                        .formatted(dto.getClass().getSimpleName(), entity.getClass().getSimpleName())
        );
    }
}
