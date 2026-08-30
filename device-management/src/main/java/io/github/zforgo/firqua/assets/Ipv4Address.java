package io.github.zforgo.firqua.assets;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.validator.constraints.IpAddress;

import static io.github.zforgo.firqua.common.DataTypes.ipv4Length;
import static org.hibernate.validator.constraints.IpAddress.Type.IPv4;

@Entity
@Table(name = "ASSETS_IP_ADDRESSES")
public class Ipv4Address {

    @Id
    @Column(name = "IP_ADDRESS", unique = true, updatable = false, length = ipv4Length)
    @IpAddress(type = IPv4)
    private String address;

    protected Ipv4Address() {
    }

    public Ipv4Address(String ipAddress) {
        this.address = ipAddress;
    }

    public String getAddress() {
        return address;
    }
}
