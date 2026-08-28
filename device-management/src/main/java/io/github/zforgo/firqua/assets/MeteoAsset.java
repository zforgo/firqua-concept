package io.github.zforgo.firqua.assets;

import static io.github.zforgo.firqua.common.DataTypes.slugIdLength;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@DiscriminatorValue(value = "METEO")
@Table(name = "ASSETS_METEO_SENSORS")
public class MeteoAsset extends Asset implements IpAddressAware {
    {
        type = AssetType.METEO;
    }

    @NotBlank
    @Column(name = "STATION_ID", nullable = false, unique = true, length = slugIdLength)
    public String stationId;

    @OneToOne(cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "IP_ADDRESS", referencedColumnName = "IP_ADDRESS")
    public Ipv4Address ipAddress;

    @Override
    public Ipv4Address getIpAddress() {
        return ipAddress;
    }

    @Override
    public void setIpAddress(Ipv4Address ipAddress) {
        this.ipAddress = ipAddress;
    }
}
