package io.github.zforgo.firqua.hibernate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "TEST_PARENT")
class ParentEntity extends TestSuperClass {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_parent_seq")
    @SequenceGenerator(name = "test_parent_seq", sequenceName = "TEST_PARENT_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false, updatable = false)
    Long id;

    @Embedded
    Name name;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "TEST_ENTITY_ID")
    TestEntity testEntity;

    @Column(name = "STRING_VALUE")
    String stringValue;
}
