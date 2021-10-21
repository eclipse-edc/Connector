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
package org.eclipse.dataspaceconnector.iam.did.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link DidResolverRegistryImpl}.
 */
class DidResolverRegistryImplTest {
    public static final String FOO_METHOD = "foo";
    private DidResolverRegistryImpl registry;

    @Test
    void verifyResolveInvalidDid() {
        var result = registry.resolve("invalid");
        Assertions.assertTrue(result.invalid());
    }

    @Test
    void verifyResolveUnknownDidMethod() {
        var result = registry.resolve("did:unknown:id");
        Assertions.assertTrue(result.invalid());
    }

    @Test
    void verifyResolveDid() {
        registry.register(new MockResolver());
        var result = registry.resolve("did:foo:id");
        Assertions.assertNotNull(result.getDidDocument());
    }

    @BeforeEach
    void setUp() {
        registry = new DidResolverRegistryImpl();
    }

    /**
     * Mock resolver class.
     */
    private static class MockResolver implements DidResolver {

        @Override
        public @NotNull String getMethod() {
            return FOO_METHOD;
        }

        @Override
        public DidDocument resolve(String didKey) {
            return DidDocument.Builder.newInstance().build();
        }
    }

}
