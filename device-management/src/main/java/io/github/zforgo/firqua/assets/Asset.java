package io.github.zforgo.firqua.assets;

import static io.github.zforgo.firqua.common.DataTypes.enumNameLength;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@DiscriminatorColumn(name = "ASSET_TYPE", length = enumNameLength)
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "ASSETS")
@AttributeOverride(name = "id", column = @Column(name = "ID", nullable = false, updatable = false))
public abstract class Asset extends PanacheEntity {

    @Column(name = "ASSET_TYPE", nullable = false, updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    public AssetType type;

    @Column(name = "NAME", nullable = false)
    public String name;

    @Column(name = "VENDOR_NAME", nullable = false)
    public String vendor;

    @Column(name = "VENDOR_MODEL")
    public String model;

}
