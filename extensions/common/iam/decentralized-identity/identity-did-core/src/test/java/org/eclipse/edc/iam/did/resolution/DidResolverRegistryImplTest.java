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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link DidResolverRegistryImpl}.
 */
class DidResolverRegistryImplTest {
    public static final String FOO_METHOD = "foo";
    private DidResolverRegistryImpl registry;


    @BeforeEach
    void setUp() {
        registry = new DidResolverRegistryImpl(Clock.systemUTC());
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

    @Test
    void resolve_whenCached() {
        var resolver = mock(DidResolver.class);
        when(resolver.getMethod()).thenReturn(FOO_METHOD);
        when(resolver.resolve(any())).thenReturn(Result.success(DidDocument.Builder.newInstance().build()));
        registry.register(resolver);

        var result = registry.resolve("did:foo:id");

        assertThat(result).isSucceeded();

        assertThat(registry.resolve("did:foo:id")).isSucceeded(); //doc is cached

        verify(resolver, times(1)).resolve(anyString());

    }

    @Test
    void resolve_whenCacheExpired() {
        registry = new DidResolverRegistryImpl(Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.systemDefault()));
        var resolver = mock(DidResolver.class);
        when(resolver.getMethod()).thenReturn(FOO_METHOD);
        when(resolver.resolve(any())).thenReturn(Result.success(DidDocument.Builder.newInstance().build()));
        registry.register(resolver);

        assertThat(registry.resolve("did:foo:id")).isSucceeded();
        assertThat(registry.resolve("did:foo:id")).isSucceeded(); //cache entry is expired

        verify(resolver, times(2)).resolve(anyString());
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
            return Result.success(DidDocument.Builder.newInstance().id(UUID.randomUUID().toString()).build());
        }
    }

}
