package io.github.zforgo.firqua.assets;

import jakarta.validation.constraints.NotBlank;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.hibernate.validator.constraints.IpAddress;
import org.hibernate.validator.constraints.Length;

import static io.github.zforgo.firqua.common.DataTypes.ipv4Length;
import static io.github.zforgo.firqua.common.DataTypes.slugIdLength;
import static org.hibernate.validator.constraints.IpAddress.Type.IPv4;

@Schema(name = "MeteoSensorAssetCreate",
        allOf = { AssetCreateDto.class },
        properties = @SchemaProperty(name = "type", enumeration = "METEO"))
public final class MeteoSensorAssetCreateDto extends AssetCreateDto implements IpAddressAwareDto {

    {
        type = AssetType.METEO;
    }

    @NotBlank
    @Length(max = slugIdLength)
    public String stationId;

    @Schema(type = SchemaType.STRING, format = "ipv4")
    @IpAddress(type = IPv4)
    @Length(max = ipv4Length)
    public String ipAddress;

    @Override
    public String getIpAddress() {
        return this.ipAddress;
    }

    @Override
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
