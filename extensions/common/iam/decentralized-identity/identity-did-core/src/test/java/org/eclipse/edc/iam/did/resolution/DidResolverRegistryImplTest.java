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

package org.eclipse.edc.iam.did.resolution;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link DidResolverRegistryImpl}.
 */
class DidResolverRegistryImplTest {
    public static final String FOO_METHOD = "foo";
    private DidResolverRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new DidResolverRegistryImpl();
    }

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

    @Test
    void isSupported() {
        registry.register(new MockResolver());
        assertThat(registry.isSupported("did:%s:whatever".formatted(FOO_METHOD))).isTrue();
        assertThat(registry.isSupported("did:unsupported:whatever")).isFalse();
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
