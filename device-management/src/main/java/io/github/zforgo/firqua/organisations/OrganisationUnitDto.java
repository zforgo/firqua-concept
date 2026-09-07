package io.github.zforgo.firqua.organisations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import static io.github.zforgo.firqua.common.DataTypes.slugIdLength;

@Schema(name = "OrganisationUnit")
public class OrganisationUnitDto {

    @NotNull
    public Long id;

    @NotBlank
    public String name;

    @NotBlank
    @Size(max = slugIdLength)
    public String slug;

}
