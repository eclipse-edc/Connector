package org.eclipse.dataspaceconnector.transfer.dataplane.core.security;

import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

/**
 * Implementation of {@link DataEncrypter} which does not perform any encryption.
 */
public class NoopDataEncrypter implements DataEncrypter {
    @Override
    public String encrypt(String raw) {
        return raw;
    }

    @Override
    public String decrypt(String encrypted) {
        return encrypted;
    }
}
