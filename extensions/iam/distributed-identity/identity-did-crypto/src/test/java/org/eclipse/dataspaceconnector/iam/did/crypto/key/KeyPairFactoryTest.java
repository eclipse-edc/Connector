package org.eclipse.dataspaceconnector.iam.did.crypto.key;

import org.junit.jupiter.api.Test;

class KeyPairFactoryTest {

    @Test
    void generateKeyPair() {
        // assert no exception is thrown
        KeyPairFactory.generateKeyPair();
    }

    @Test
    void generateKeyPairP256() {
        // assert no exception is thrown
        KeyPairFactory.generateKeyPairP256();
    }
}
