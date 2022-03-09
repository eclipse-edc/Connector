package org.eclipse.dataspaceconnector.transfer.dataplane.spi.security;

/**
 * Interface for encryption/decryption of sensible data.
 * This is especially used to secure the data address encoded as claim in the security token.
 */
public interface DataEncrypter {
    String encrypt(String raw);

    String decrypt(String encrypted);
}
