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
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.JwkKeys;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdentityProviderKeyResolverTest {
    private static final String JWKS_URL = "https://login.microsoftonline.com/common/discovery/v2.0/keys";
    private static final String JWKS_FILE = "jwks_response.json";
    private IdentityProviderKeyResolver resolver;
    private JwkKeys keys;

    @Test
    void verifyJwksDeserialization() {
        assertThat(keys.getKeys()).isNotNull();
        Map<String, RSAPublicKey> keyCache = resolver.deserializeKeys(keys.getKeys());

        assertThat(keyCache)
                .hasSize(5)
                .containsKey("nOo3ZDrODXEK1jKWhXslHR_KXEg");
    }

    @BeforeEach
    void setUp() {
        resolver = new IdentityProviderKeyResolver(JWKS_URL, new Monitor() {
        }, mock(OkHttpClient.class), new TypeManager());

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(JWKS_FILE)) {
            keys = new ObjectMapper().readValue(in, JwkKeys.class);
        } catch (IOException e) {
            throw new EdcException("Failed to load keys from file");
        }
    }
}
