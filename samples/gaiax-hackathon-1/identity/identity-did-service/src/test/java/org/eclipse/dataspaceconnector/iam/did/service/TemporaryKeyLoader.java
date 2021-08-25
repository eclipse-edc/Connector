/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 *
 * TODO HACKATHON-1 Test usage should eventually be moved to common/util/testFixtures.
 */
public class TemporaryKeyLoader {
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";
    private static final String PASSWORD = "test123";
    private static RSAKey keys;

    public static RSAKey loadKeys() {
        if (keys == null) {
            try {
                var url = TemporaryKeyLoader.class.getClassLoader().getResource(TEST_KEYSTORE);
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                assert url != null;
                try (InputStream stream = url.openStream()) {
                    keyStore.load(stream, PASSWORD.toCharArray());
                }
                keys= RSAKey.load(keyStore, "testkey", PASSWORD.toCharArray());
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
