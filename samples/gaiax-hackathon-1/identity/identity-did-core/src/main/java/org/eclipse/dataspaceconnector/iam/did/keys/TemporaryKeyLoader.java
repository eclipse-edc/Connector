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
package org.eclipse.dataspaceconnector.iam.did.keys;

import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;

import static java.lang.String.format;

/**
 * Temporary key loader until DID key management is implemented.
 *
 * TODO HACKATHON-1 Usage of these functions in main code needs to be removed. Test usage should eventually be moved to common/util/testFixtures.
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


}
