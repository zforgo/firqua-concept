package io.github.zforgo.firqua.hibernate;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "TEST_ENTITY")
class TestEntity extends TestSuperClass {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_entity_seq")
    @SequenceGenerator(name = "test_entity_seq", sequenceName = "TEST_ENTITY_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false, updatable = false)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ENUM")
    TestEnum testEnum;

    @Convert(converter = TestEnumListAttributeConverter.class)
    @Column(name = "ENUMS")
    List<TestEnum> enums;

    @Embedded
    Name name;

    @Column(name = "ENABLED")
    boolean enabled;

    @Version
    @Column(name = "VERSION")
    Long version;
}
