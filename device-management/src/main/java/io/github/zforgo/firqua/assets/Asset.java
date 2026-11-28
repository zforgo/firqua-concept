package io.github.zforgo.firqua.assets;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import static io.github.zforgo.firqua.common.DataTypes.enumNameLength;

@Entity
@DiscriminatorColumn(name = "ASSET_TYPE", length = enumNameLength)
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "ASSETS")
public abstract class Asset extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assets_seq")
    @SequenceGenerator(name = "assets_seq", sequenceName = "ASSETS_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false, updatable = false)
    public Long id;

    @Column(name = "ASSET_TYPE", nullable = false, updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    public AssetType type;

    @Column(name = "NAME", nullable = false)
    public String name;

    @Column(name = "ENABLED", nullable = false)
    public boolean enabled = false;

    @Column(name = "VENDOR_NAME", nullable = false)
    public String vendor;

    @Column(name = "VENDOR_MODEL")
    public String model;
}
