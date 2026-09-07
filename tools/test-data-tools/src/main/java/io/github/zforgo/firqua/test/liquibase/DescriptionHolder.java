package io.github.zforgo.firqua.test.liquibase;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class DescriptionHolder {

    private record Description(Class<?> cls, Method method) {}

    private static final Set<Description> STORE = ConcurrentHashMap.newKeySet();

    public static boolean hasClass(Class<?> cls) {
        return STORE.stream().anyMatch(d -> d.cls.equals(cls));
    }

    public static boolean hasMethod(Class<?> cls, Method method) {
        return STORE.contains(new Description(cls, method));
    }

    public static void store(Class<?> cls, Method method) {
        STORE.add(new Description(cls, method));
    }

    public static void discard(Class<?> cls) {
        STORE.removeIf(d -> d.cls.equals(cls));
    }
}
