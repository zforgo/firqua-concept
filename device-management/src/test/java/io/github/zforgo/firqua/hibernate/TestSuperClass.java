package io.github.zforgo.firqua.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@MappedSuperclass
abstract class TestSuperClass extends PanacheEntityBase {

    @Column(name = "BASE_PROPERTY")
    String baseProperty;
}
