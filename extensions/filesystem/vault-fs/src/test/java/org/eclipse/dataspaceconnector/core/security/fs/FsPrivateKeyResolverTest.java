/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.core.security.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class FsPrivateKeyResolverTest {
    private static final String PASSWORD = "test123";
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";

    private FsPrivateKeyResolver keyResolver;

    @Test
    void verifyResolutionOk() {
        assertThat(keyResolver.getEncodedKey("testkey"))
                .isNotNull()
                .contains("-----BEGIN RSA PRIVATE KEY-----");
    }

    @Test
    void verifyResolutionKo() {
        assertThat(keyResolver.getEncodedKey("testkeyxx"))
                .isNull();
    }

    @BeforeEach
    void setUp() throws Exception {
        var url = getClass().getClassLoader().getResource(FsPrivateKeyResolverTest.TEST_KEYSTORE);
        Objects.requireNonNull(url);
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (var stream = url.openStream()) {
            keyStore.load(stream, FsPrivateKeyResolverTest.PASSWORD.toCharArray());
        }
        keyResolver = new FsPrivateKeyResolver(FsPrivateKeyResolverTest.PASSWORD, keyStore);
    }
}
