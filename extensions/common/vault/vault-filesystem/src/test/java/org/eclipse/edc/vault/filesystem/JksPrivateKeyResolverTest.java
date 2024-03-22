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

package org.eclipse.edc.vault.filesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled("This resolver will be refactored shortly")
class JksPrivateKeyResolverTest {
    private static final String PASSWORD = "test123";
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";

    private JksPrivateKeyResolver keyResolver;
    private KeyParserRegistry registry;

    @Test
    public void resolve_rsaKey() {
        assertThat(keyResolver.resolvePrivateKey("testkey"))
                .isNotNull();
    }

    @Test
    public void resolve_ecKey() {
        assertThat(keyResolver.resolvePrivateKey("testkey-ec"))
                .isNotNull();
    }

    @BeforeEach
    void setUp() throws Exception {
        var url = getClass().getClassLoader().getResource(JksPrivateKeyResolverTest.TEST_KEYSTORE);
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        assert url != null;
        try (var stream = url.openStream()) {
            keyStore.load(stream, JksPrivateKeyResolverTest.PASSWORD.toCharArray());
        }
        registry = mock();
        var parser = new PemParser(mock());
        when(registry.parse(anyString())).thenAnswer(a -> Result.success(parser.parse(a.getArgument(0))));
        keyResolver = new JksPrivateKeyResolver(registry, JksPrivateKeyResolverTest.PASSWORD, keyStore, mock(), mock());
        Security.addProvider(new BouncyCastleProvider());
    }

}
