package io.github.zforgo.firqua.common;

public class ReferencedFieldRequiredException extends ConflictException {

    public ReferencedFieldRequiredException(String key) {
        this(key, null);
    }

    private ReferencedFieldRequiredException(String key, Throwable cause) {
        super("The field %s must not be null due to assignment".formatted(key), cause);
    }
}
