package io.github.zforgo.firqua.hibernate;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "TEST_HOLDER")
class HolderEntity extends TestSuperClass {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_holder_seq")
    @SequenceGenerator(name = "test_holder_seq", sequenceName = "TEST_HOLDER_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false, updatable = false)
    Long id;

    @Column(name = "STRING_VALUE")
    String stringValue;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(name = "TEST_HOLDER_ENTITIES",
            joinColumns = @JoinColumn(name = "HOLDER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ENTITY_ID"))
    List<TestEntity> testEntities;

    public List<TestEntity> getTestEntities() {
        if (testEntities == null) {
            testEntities = new ArrayList<>();
        }
        return testEntities;
    }
}
