package io.github.zforgo.firqua.common;

public class NonUniqueIpAddressException extends RuntimeException {

    private static final String template = "Ip address %s already in use.";

    public NonUniqueIpAddressException(String message) {
        super(message);
    }

    public NonUniqueIpAddressException(String message, Throwable cause) {
        super(message, cause);
    }

    public static NonUniqueIpAddressException byAddress(String address) {
        return new NonUniqueIpAddressException(template.formatted(address));
    }

    public static NonUniqueIpAddressException byAddress(String address, Throwable cause) {
        return new NonUniqueIpAddressException(template.formatted(address), cause);
    }
}
