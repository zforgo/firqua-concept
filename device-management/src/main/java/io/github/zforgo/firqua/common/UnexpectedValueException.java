package io.github.zforgo.firqua.common;

public class UnexpectedValueException extends ConflictException {

    private final String key;
    private final Object value;
    private final Object expected;

    public UnexpectedValueException(String key, Object value, Object expected) {
        this(key, value, expected, null);
    }

    public UnexpectedValueException(String key, Object value, Throwable cause) {
        this(key, value, null, cause);
    }

    public UnexpectedValueException(String key, Object value, Object expected, Throwable cause) {
        this.key = key;
        this.value = value;
        this.expected = expected;
        super(null, cause);

    }

    @Override
    public String getMessage() {
        return buildMessage(key, value, expected);
    }

    protected String buildMessage(String key, Object value, Object expected) {
        return "Cannot set %s to unexpected value [%s]%s"
                .formatted(key, value, expected != null ? ", expected: %s".formatted(expected) : "");

    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Object getExpected() {
        return expected;
    }
}
