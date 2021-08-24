package org.eclipse.dataspaceconnector.iam.did.hub;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Temporary key loader until DID key management is implemented.
 */
public class TemporaryKeyLoader {
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";
    private static final String PASSWORD = "test123";
    private static RSAKey keys;

    public static PublicKey loadPublicKey(Monitor monitor) {
        try {
            return Objects.requireNonNull(loadKeys(monitor)).toPublicKey();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }

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
}
