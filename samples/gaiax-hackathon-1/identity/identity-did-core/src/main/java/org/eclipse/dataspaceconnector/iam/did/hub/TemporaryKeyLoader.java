package org.eclipse.dataspaceconnector.iam.did.hub;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static java.lang.String.format;

/**
 * Temporary key loader until DID key management is implemented.
 */
public class TemporaryKeyLoader {
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";
    private static final String PASSWORD = "test123";
    private static RSAKey keys;

    @Nullable
    public static RSAKey loadKeys(Monitor monitor) {
        if (keys == null) {
            try {
                var url = Paths.get("secrets" + File.separator + TEST_KEYSTORE).toUri().toURL();
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (InputStream stream = url.openStream()) {
                    keyStore.load(stream, PASSWORD.toCharArray());
                }
                keys = RSAKey.load(keyStore, "testkey", PASSWORD.toCharArray());
            } catch (Exception e) {
                monitor.info(format("Cannot load test keys - the keystore %s should be placed in the secrets directory", TEST_KEYSTORE));
                return null;
            }
        }
        return keys;
    }

    public static RSAKey loadKeys() {
        if (keys == null) {
            try {
                var url = TemporaryKeyLoader.class.getClassLoader().getResource(TEST_KEYSTORE);
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                assert url != null;
                try (InputStream stream = url.openStream()) {
                    keyStore.load(stream, PASSWORD.toCharArray());
                }
                return RSAKey.load(keyStore, "testkey", PASSWORD.toCharArray());
            } catch (Exception e) {
                throw new AssertionError(e);
            }

        }
        return keys;
    }


    public static RSAPrivateKey loadPrivateKey() {
        try {
            return loadKeys().toRSAPrivateKey();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }

    public static RSAPublicKey loadPublicKey() {
        try {
            return loadKeys().toRSAPublicKey();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }


}
