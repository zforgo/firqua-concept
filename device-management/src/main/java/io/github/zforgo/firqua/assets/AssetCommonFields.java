package io.github.zforgo.firqua.assets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public abstract class AssetCommonFields {

    @NotNull
    public AssetType type;

    @NotBlank
    public String name;

    @NotBlank
    public String vendor;

    public String model;
}
