package io.github.zforgo.firqua.organisations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import static io.github.zforgo.firqua.common.DataTypes.slugIdLength;

@Entity
@Table(name = "ORGANISATION_UNITS")
public class OrganisationUnit extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organisation_units_seq")
    @SequenceGenerator(name = "organisation_units_seq", sequenceName = "ORGANISATION_UNITS_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false, updatable = false)
    public Long id;

    @NotBlank
    @Column(name = "NAME", nullable = false)
    public String name;

    @NotBlank
    @Size(max = slugIdLength)
    @Column(name = "SLUG", nullable = false, unique = true, length = slugIdLength)
    public String slug;

}
