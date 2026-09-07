package io.github.zforgo.firqua.devices;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.organisations.OrganisationUnit;

import static io.github.zforgo.firqua.common.DataTypes.enumNameLength;

@Entity
@DiscriminatorColumn(name = "DEVICE_TYPE", length = enumNameLength)
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "DEVICES")
public abstract class Device<T extends Asset> extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "devices_seq")
    @SequenceGenerator(name = "devices_seq", sequenceName = "DEVICES_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false, updatable = false)
    public Long id;

    @Column(name = "DEVICE_TYPE", nullable = false, updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    public DeviceType type;

    @NotBlank
    @Column(name = "NAME", nullable = false, unique = true)
    public String name;

    @OneToOne(optional = false, targetEntity = Asset.class)
    @JoinColumn(name = "ASSET_ID", nullable = false)
    public T asset;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ORGANISATION_UNIT_ID", nullable = false)
    public OrganisationUnit organisationUnit;

    public void setAsset(T asset) {
        this.asset = asset;
    }
}
