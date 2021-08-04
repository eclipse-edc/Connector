/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.security.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FsPrivateKeyResolverTest {
    private static final String PASSWORD = "test123";
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";

    private FsPrivateKeyResolver keyResolver;

    @Test
    public void verifyResolution() {
        assertNotNull(keyResolver.resolvePrivateKey("testkey"));
    }

    @BeforeEach
    void setUp() throws Exception {
        var url = getClass().getClassLoader().getResource(FsPrivateKeyResolverTest.TEST_KEYSTORE);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        assert url != null;
        try (InputStream stream = url.openStream()) {
            keyStore.load(stream, FsPrivateKeyResolverTest.PASSWORD.toCharArray());
        }
        keyResolver = new FsPrivateKeyResolver(FsPrivateKeyResolverTest.PASSWORD, keyStore);
    }
}
