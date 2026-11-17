package io.github.zforgo.firqua.hibernate;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.hibernate.TestEnum.BAR;
import static io.github.zforgo.firqua.hibernate.TestEnum.BAZ;
import static io.github.zforgo.firqua.hibernate.TestEnum.FOO;
import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true, additionalContexts = "dirty_props")
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestTransaction
class DirtyPropertiesTest {

    @Test
    void newEntity() {
        var input = new TestEntity();
        input.baseProperty = "base";
        input.enums = Arrays.asList(FOO, BAR);
        input.testEnum = BAZ;
        var name = new Name();
        name.firstName = "firstName";
        name.lastName = "lastName";
        input.name = name;
        var changedFields = DirtyProperties.collect(input);
        assertTrue(changedFields.isEmpty());
    }

    @Test
    void nonChanged() {
        var input = new TestEntity();
        input.baseProperty = "base";
        input.enums = Arrays.asList(FOO, BAR);
        input.testEnum = BAZ;
        var name = new Name();
        name.firstName = "firstName";
        name.lastName = "lastName";
        input.name = name;
        input.persistAndFlush();
        var changedFields = DirtyProperties.collect(input);
        assertTrue(changedFields.isEmpty());
    }

    @Test
    void changedToSame() {
        var base = "base";
        var base2 = "ba%se".formatted('s');

        var name = new Name();
        name.firstName = "firstName";
        name.lastName = "lastName";

        var name2 = new Name();
        name2.firstName = "firstName";
        name2.lastName = "lastName";

        var input = new TestEntity();
        input.baseProperty = base;
        input.enums = Arrays.asList(FOO, BAR);
        input.testEnum = BAZ;
        input.name = name;
        input.persistAndFlush();

        input.baseProperty = base2;
        input.name = name2;
        var changedFields = DirtyProperties.collect(input);
        assertTrue(changedFields.isEmpty());
    }

    @Test
    void simpleChanges() {
        var input = new TestEntity();
        input.baseProperty = "base";
        input.enums = Arrays.asList(FOO, BAR);
        input.testEnum = BAZ;
        var name = new Name();
        name.firstName = "firstName";
        name.lastName = "lastName";
        input.name = name;
        input.persistAndFlush();

        input.name.firstName = "changedFirstName";
        input.enums = Collections.singletonList(BAZ);
        input.testEnum = null;
        input.baseProperty = "changedBase";

        var changedFields = DirtyProperties.collect(input);
        assertThat(changedFields, containsInAnyOrder("baseProperty", "enums", "testEnum", "name.firstName"));
    }

    @Test
    void childAssigned() {
        var parent = new ParentEntity();
        parent.baseProperty = "base";
        parent.stringValue = "stringValue";
        parent.persist();

        var child = new TestEntity();
        child.baseProperty = "baseChild";
        child.testEnum = BAZ;
        child.persist();
        parent.testEntity = child;

        var changedFields = DirtyProperties.collect(parent);
        assertThat(changedFields, contains("testEntity"));

    }

    @Test
    void childCreated() {
        var parent = new ParentEntity();
        parent.baseProperty = "base";
        parent.stringValue = "stringValue";

        var child = new TestEntity();
        child.baseProperty = "baseChild";
        child.testEnum = BAZ;
        parent.testEntity = child;
        parent.persistAndFlush();

        var changedFields = DirtyProperties.collect(parent);
        assertTrue(changedFields.isEmpty());
    }

    @Test
    void childRemoved() {
        var parent = new ParentEntity();
        parent.baseProperty = "base";
        parent.stringValue = "stringValue";

        var child = new TestEntity();
        child.baseProperty = "baseChild";
        child.testEnum = BAZ;
        parent.testEntity = child;
        parent.persistAndFlush();
        child.delete();
        parent.testEntity = null;
        var changedFields = DirtyProperties.collect(parent);
        assertThat(changedFields, contains("testEntity"));
    }

    @Test
    void manyToOneChanges() {
        var parent = new ParentEntity();
        parent.baseProperty = "base";
        parent.stringValue = "stringValue";

        var child = new TestEntity();
        child.baseProperty = "baseChild";
        child.testEnum = BAZ;
        parent.testEntity = child;
        parent.persistAndFlush();

        var name = new Name();
        name.firstName = "firstName";
        name.lastName = "lastName";
        parent.name = name;
        child.testEnum = FOO;
        var changedFields = DirtyProperties.collect(parent);
        assertThat(changedFields, containsInAnyOrder("name", "testEntity.testEnum"));
    }

    @Test
    void relatedObjectEmbeddedPropertyChanges() {
        var parent = new ParentEntity();
        parent.baseProperty = "base";
        parent.stringValue = "stringValue";

        var name = new Name();
        name.firstName = "firstName";
        name.lastName = "lastName";

        var child = new TestEntity();
        child.baseProperty = "baseChild";
        child.testEnum = BAZ;
        child.name = name;
        parent.testEntity = child;
        parent.persistAndFlush();

        child.testEnum = FOO;
        child.name.firstName = "changedFirstName";

        var changedFields = DirtyProperties.collect(parent);
        assertThat(changedFields, containsInAnyOrder("testEntity.name.firstName", "testEntity.testEnum"));
    }

    @Test
    void oneToManyCreated() {
        var holder = new HolderEntity();
        holder.baseProperty = "base";

        var item0 = new TestEntity();
        item0.baseProperty = "baseChild0";

        var item1 = new TestEntity();
        item1.baseProperty = "baseChild1";
        item1.testEnum = BAZ;

        var item2 = new TestEntity();
        item2.baseProperty = "baseChild2";

        holder.testEntities = Arrays.asList(item0, item1, item2);
        holder.persistAndFlush();

        var changedFields = DirtyProperties.collect(holder);
        assertTrue(changedFields.isEmpty());
    }

    @Test
    void oneToManyItemRemoved() {
        var holder = HolderEntity.<HolderEntity> findById(1L);
        holder.getTestEntities().remove(1);
        var changedFields = DirtyProperties.collect(holder);
        assertThat(changedFields, contains("testEntities"));
    }

    @Test
    void oneToManyItemsReplaced() {
        var item0 = new TestEntity();
        item0.baseProperty = "baseChild0";

        var item1 = new TestEntity();
        item1.baseProperty = "baseChild1";
        item1.testEnum = BAZ;

        var item2 = new TestEntity();
        item2.baseProperty = "baseChild2";

        var holder = HolderEntity.<HolderEntity> findById(1L);
        holder.testEntities = Arrays.asList(item0, item1, item2);
        var changedFields = DirtyProperties.collect(holder);
        assertThat(changedFields, contains("testEntities"));
    }

    @Test
    void oneToManyItemAdded() {
        var item0 = new TestEntity();
        item0.baseProperty = "baseChild0";

        var holder = HolderEntity.<HolderEntity> findById(1L);
        holder.getTestEntities().add(item0);
        var changedFields = DirtyProperties.collect(holder);
        assertThat(changedFields, contains("testEntities"));
    }

    @Test
    void oneToManyItemChanged() {
        var holder = HolderEntity.<HolderEntity> findById(1L);
        holder.getTestEntities().getFirst().testEnum = BAZ;
        var changedFields = DirtyProperties.collect(holder);
        assertThat(changedFields, contains("testEntities[1].testEnum"));
    }

    @Test
    void oneToManyHolderChanged() {
        var holder = HolderEntity.<HolderEntity> findById(1L);
        holder.stringValue = "changedValue";
        holder.baseProperty = "changedBase";
        var changedFields = DirtyProperties.collect(holder);
        assertThat(changedFields, containsInAnyOrder("stringValue", "baseProperty"));
    }
}
