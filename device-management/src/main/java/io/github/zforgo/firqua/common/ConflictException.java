package io.github.zforgo.firqua.common;

import org.apache.commons.lang3.StringUtils;

public class ConflictException extends RuntimeException {

    private final String key;
    private final Object value;

    public ConflictException(String key, Object value) {
        this(key, value, null);
    }

    public ConflictException(String key, Object value, Throwable cause) {
        this.key = key;
        this.value = value;
        super(
                "The field %s with value %s must be unique"
                        .formatted(key, StringUtils.abbreviate(String.valueOf(value), 50)),
                cause
        );
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
