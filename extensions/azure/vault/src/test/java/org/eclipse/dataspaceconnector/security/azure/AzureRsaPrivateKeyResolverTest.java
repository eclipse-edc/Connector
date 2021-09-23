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

package org.eclipse.dataspaceconnector.security.azure;

import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.dataspaceconnector.security.azure.PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER;
import static org.eclipse.dataspaceconnector.security.azure.PrivateTestKeys.ENCODED_PRIVATE_KEY_NOHEADER;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(EasyMockExtension.class)
class AzureRsaPrivateKeyResolverTest {

    @Mock
    Vault vault;

    private AzurePrivateKeyResolver keyResolver;

    @Test
    void verifyResolution_withHeader() {
        EasyMock.expect(vault.resolveSecret("test_header")).andReturn(ENCODED_PRIVATE_KEY_HEADER);
        EasyMock.replay(vault);

        assertNotNull(keyResolver.resolvePrivateKey("test_header"));

        EasyMock.verify(vault);
    }

    @Test
    void verifyResolution_withoutHeader() {
        EasyMock.expect(vault.resolveSecret("test_no_header")).andReturn(ENCODED_PRIVATE_KEY_NOHEADER);
        EasyMock.replay(vault);

        assertNotNull(keyResolver.resolvePrivateKey("test_no_header"));

        EasyMock.verify(vault);
    }

    @BeforeEach
    void setUp() {
        keyResolver = new AzurePrivateKeyResolver(vault);
    }
}
