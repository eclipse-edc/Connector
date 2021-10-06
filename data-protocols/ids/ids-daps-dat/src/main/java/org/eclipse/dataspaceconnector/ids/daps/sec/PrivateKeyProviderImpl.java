package org.eclipse.dataspaceconnector.ids.daps.sec;

import java.security.KeyPair;
import java.security.PrivateKey;

public class PrivateKeyProviderImpl implements PrivateKeyProvider {

    private final KeyPair keyPair;

    public PrivateKeyProviderImpl(final KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
