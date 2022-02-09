package org.eclipse.dataspaceconnector.dataplane.spi.token;

/**
 * Interface for encryption of sensible data.
 */
@FunctionalInterface
public interface DataEncrypter {
    String encrypt(String raw);
}
