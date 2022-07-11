/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaultPrivateKeyResolverTest {

    private static final String TEST_SECRET_ALIAS = "test-secret";
    private final Vault vault = mock(Vault.class);
    private VaultPrivateKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new VaultPrivateKeyResolver(vault);
    }

    @Test
    void verifyResolutionOk() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        assertThat(resolver.getEncodedKey(TEST_SECRET_ALIAS))
                .isNotNull()
                .isEqualTo(PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        verify(vault).resolveSecret(TEST_SECRET_ALIAS);
    }

    @Test
    void verifyResolutionKo() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(null);

        assertThat(resolver.getEncodedKey(TEST_SECRET_ALIAS)).isNull();

        verify(vault).resolveSecret(TEST_SECRET_ALIAS);
    }
}

