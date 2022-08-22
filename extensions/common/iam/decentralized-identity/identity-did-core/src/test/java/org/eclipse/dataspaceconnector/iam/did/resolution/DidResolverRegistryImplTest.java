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
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link DidResolverRegistryImpl}.
 */
class DidResolverRegistryImplTest {
    public static final String FOO_METHOD = "foo";
    private DidResolverRegistryImpl registry;

    @Test
    void verifyResolveInvalidDid() {
        var result = registry.resolve("invalid");

        assertTrue(result.failed());
    }

    @Test
    void verifyResolveUnknownDidMethod() {
        var result = registry.resolve("did:unknown:id");

        assertTrue(result.failed());
    }

    @Test
    void verifyResolveDid() {
        registry.register(new MockResolver());

        var result = registry.resolve("did:foo:id");

        assertNotNull(result.getContent());
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
        @NotNull
        public Result<DidDocument> resolve(String didKey) {
            return Result.success(DidDocument.Builder.newInstance().build());
        }
    }

}
