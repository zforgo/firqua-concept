package io.github.zforgo.firqua.assets;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import static io.github.zforgo.firqua.common.DataTypes.slugIdLength;

@Entity
@DiscriminatorValue(value = "SOS")
@Table(name = "ASSETS_SOS_STATIONS")
public class SosAsset extends Asset implements IpAddressAware {
    {
        type = AssetType.SOS;
    }

    @NotBlank
    @Column(name = "STATION_ID", nullable = false, unique = true, length = slugIdLength)
    public String stationId;

    @OneToOne(cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "IP_ADDRESS", referencedColumnName = "IP_ADDRESS")
    public Ipv4Address ipAddress;

    @Column(name = "SERVICE_PROVIDER", length = slugIdLength)
    public String serviceProvider;

    @Override
    public Ipv4Address getIpAddress() {
        return ipAddress;
    }

    @Override
    public void setIpAddress(Ipv4Address ipAddress) {
        this.ipAddress = ipAddress;
    }
}
