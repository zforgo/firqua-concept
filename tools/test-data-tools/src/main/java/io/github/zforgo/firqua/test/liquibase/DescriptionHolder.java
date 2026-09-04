package io.github.zforgo.firqua.test.liquibase;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

final class DescriptionHolder {

    private record Description(Class<?> cls, Method method) {}

    private static final ThreadLocal<Set<Description>> STORE = ThreadLocal.withInitial(HashSet::new);

    public static boolean hasClass(Class<?> cls) {
        return STORE.get().stream().anyMatch(d -> d.cls.equals(cls));
    }

    public static boolean hasMethod(Class<?> cls, Method method) {
        return STORE.get().contains(new Description(cls, method));
    }

    public static void store(Class<?> cls, Method method) {
        STORE.get().add(new Description(cls, method));
    }
}
