package org.eclipse.dataspaceconnector.iam.did.hub.test;

import com.nimbusds.jose.jwk.RSAKey;

import java.io.InputStream;
import java.security.KeyStore;

/**
 *
 */
public class JweTestFunctions {
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";
    private static final String PASSWORD = "test123";

    public static RSAKey loadAndGetKey() throws Exception {
        var url = JweTestFunctions.class.getClassLoader().getResource(TEST_KEYSTORE);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        assert url != null;
        try (InputStream stream = url.openStream()) {
            keyStore.load(stream, PASSWORD.toCharArray());
        }
        return RSAKey.load(keyStore, "testkey", PASSWORD.toCharArray());
    }

}
