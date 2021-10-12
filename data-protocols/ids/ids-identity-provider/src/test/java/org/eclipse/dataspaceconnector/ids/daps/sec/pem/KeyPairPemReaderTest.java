package org.eclipse.dataspaceconnector.ids.daps.sec.pem;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KeyPairPemReaderTest extends AbstractResourceLoadingTest {
    private static final String PASSPHRASE = "test";

    @Test
    void readKeyPair() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException {
        final KeyPairPemReader keyPairPemReader = new KeyPairPemReader();

        final KeyPair keyPair;
        try (final InputStream inputStream = getResource("test-rsa-private-key.pem")) {
            keyPair = keyPairPemReader.readKeyPair(inputStream);
        }

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
    }

    @Test
    void readEncryptedKeyPair() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException {
        final KeyPairPemReader keyPairPemReader = new KeyPairPemReader();

        final KeyPair keyPair;
        try (final InputStream inputStream = getResource("test-rsa-encrypted-private-key.pem")) {
            keyPair = keyPairPemReader.readKeyPair(inputStream, PASSPHRASE.toCharArray());
        }

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
    }
}