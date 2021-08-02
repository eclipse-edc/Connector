/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FsPrivateKeyResolverTest {
    private static final String PASSWORD = "test123";
    private static final String TEST_KEYSTORE = "dagx-test-keystore.jks";

    private FsPrivateKeyResolver keyResolver;

    @Test
    public void verifyResolution() {
        assertNotNull(keyResolver.resolvePrivateKey("testkey"));
    }

    @BeforeEach
    void setUp() throws Exception {
        var url = getClass().getClassLoader().getResource(TEST_KEYSTORE);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        assert url != null;
        try (InputStream stream = url.openStream()) {
            keyStore.load(stream, PASSWORD.toCharArray());
        }
        keyResolver = new FsPrivateKeyResolver(PASSWORD, keyStore);
    }
}
