package io.github.zforgo.firqua.hibernate;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import io.quarkus.hibernate.orm.panache.Panache;

public class DirtyProperties {

    private DirtyProperties() {
        // avoid direct instantiation
    }

    private record Holder(String name, Type type, Object oldValue, Object currentValue) {

        boolean isFetched() {
            return currentValue == LazyPropertyInitializer.UNFETCHED_PROPERTY;
        }

        boolean isDirty(SessionImplementor session) {
            return type.isDirty(oldValue, currentValue, session);
        }
    }

    public static Set<String> collect(Object entity) {
        var unproxied = Hibernate.unproxy(entity);
        var em = Panache.getEntityManager(unproxied.getClass());
        var session = em.unwrap(SessionImplementor.class);
        var result = new LinkedHashSet<String>();
        collectEntity(entity, "", session, result, Collections.newSetFromMap(new IdentityHashMap<>()));
        return result;
    }

    private static void collectEntity(
            Object entity, String prefix, SessionImplementor session, Set<String> outcome, Set<Object> visited
    ) {
        if (!visited.add(entity)) {
            return;
        }
        var entry = session.getPersistenceContextInternal().getEntry(entity);
        if (entry == null || entry.getLoadedState() == null) {
            return;
        }

        toHolder(entry, entity).stream()
                .filter(h -> !h.isFetched())
                .forEach(record -> {
                    final var path = prefix + record.name();
                    switch (record.type) {
                        case ComponentType _ -> collectComponent(record, path, session, outcome);
                        case CollectionType _ -> collectCollection(record, path, session, outcome, visited);
                        case EntityType _ -> {
                            if (record.isDirty(session)) {
                                outcome.add(path);
                            } else if (record.currentValue != null && Hibernate.isInitialized(record.currentValue)) {
                                collectEntity(Hibernate.unproxy(record.currentValue), path + ".", session, outcome, visited);
                            }
                        }
                        default -> {
                            if (record.isDirty(session)) {
                                outcome.add(path);
                            }
                        }
                    }
                });
    }

    private static void collectComponent(Holder holder, String path, SessionImplementor session, Set<String> outcome) {
        if (holder.oldValue() == null || holder.currentValue() == null) {
            if (holder.oldValue() != holder.currentValue()) {
                outcome.add(path);
            }
            return;
        }
        toHolder(holder).forEach(component -> {
            if (component.type() instanceof ComponentType) {
                collectComponent(component, path + "." + component.name(), session, outcome);
            } else if (component.isDirty(session)) {
                outcome.add(path + "." + component.name());
            }
        });
    }

    private static void collectCollection(
            Holder collection, String path, SessionImplementor session, Set<String> outcome, Set<Object> visited
    ) {
        if (!(collection.currentValue() instanceof PersistentCollection<?> pc)) {
            outcome.add(path); // completely replaced with a new instance
            return;
        }
        if (!pc.wasInitialized()) {
            return; // never loaded, implicitly unchanged
        }

        var entry = session.getPersistenceContextInternal().getCollectionEntry(pc);
        if (pc.isDirty() || !pc.equalsSnapshot(entry.getLoadedPersister())) {
            outcome.add(path);
        }
        var elements = collection.currentValue instanceof Map<?, ?> map ? map.values()
                : (Iterable<?>) collection.currentValue();
        elements.forEach(element -> {
            var elementEntry = element == null ? null : session.getPersistenceContextInternal().getEntry(element);
            if (elementEntry != null) {
                collectEntity(element, path + "[%s].".formatted(elementEntry.getId()), session, outcome, visited);
            }
        });
    }

    private static List<Holder> toHolder(EntityEntry entry, Object entity) {
        final var persister = entry.getPersister();
        var names = persister.getPropertyNames();
        var types = persister.getPropertyTypes();
        var currentValues = persister.getValues(entity);
        var oldValues = entry.getLoadedState();
        return IntStream.range(0, names.length)
                .mapToObj(i -> new Holder(names[i], types[i], oldValues[i], currentValues[i]))
                .toList();
    }

    private static List<Holder> toHolder(Holder component) {
        final ComponentType ct = (ComponentType) component.type();
        var names = ct.getPropertyNames();
        var types = ct.getSubtypes();
        var currentValues = ct.getPropertyValues(component.currentValue);
        var oldValues = ct.getPropertyValues(component.oldValue());
        return IntStream.range(0, names.length)
                .mapToObj(i -> new Holder(names[i], types[i], oldValues[i], currentValues[i]))
                .toList();

    }
}
