package io.github.zforgo.firqua.common;

public class NonUniqueException extends UnexpectedValueException {

    public NonUniqueException(String key, Object value, Throwable cause) {
        super(key, value, null, cause);
    }

    @Override
    protected String buildMessage(String key, Object value, Object expected) {
        return "Field %s must be unique, but value [%s] already exists".formatted(key, value);
    }
}
