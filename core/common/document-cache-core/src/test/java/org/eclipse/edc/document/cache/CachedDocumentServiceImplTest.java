/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.document.cache;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.document.cache.spi.resolver.DocumentResolver;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedDocumentServiceImplTest {

    private static final String URL = "https://example.com/context.jsonld";

    private final CachedDocumentStore store = mock();
    private final DocumentResolver resolver = mock();
    private final JsonLd jsonLd = mock();
    private final CachedDocumentServiceImpl service = new CachedDocumentServiceImpl(
            new NoopTransactionContext(), store, resolver, jsonLd, mock(Monitor.class), Clock.systemUTC());

    private static JsonObject resolved(String term) {
        return Json.createObjectBuilder().add("@context", Json.createObjectBuilder().add(term, "https://example.com/ns/" + term)).build();
    }

    @Test
    void create_never_withContent_registersWithoutResolving() {
        var context = CachedDocument.Builder.newInstance()
                .url(URL)
                .content("{\"@context\":{\"foo\":\"https://example.com/ns/foo\"}}")
                .pullStrategy(PullStrategy.NEVER)
                .build();
        when(store.create(any())).thenReturn(StoreResult.success(context));

        var result = service.create(context);

        assertThat(result).matches(r -> r.succeeded());
        verify(jsonLd).registerCachedDocument(eq(URL), any(JsonObject.class));
        verify(resolver, never()).resolve(any());
    }

    @Test
    void create_never_withoutContent_returnsBadRequest() {
        var context = CachedDocument.Builder.newInstance()
                .url(URL)
                .pullStrategy(PullStrategy.NEVER)
                .build();

        var result = service.create(context);

        assertThat(result).matches(r -> r.failed());
        verify(store, never()).create(any());
        verify(resolver, never()).resolve(any());
    }

    @Test
    void create_ifNotPresent_withContent_usesContent() {
        var context = CachedDocument.Builder.newInstance()
                .url(URL)
                .content("{\"@context\":{\"foo\":\"https://example.com/ns/foo\"}}")
                .pullStrategy(PullStrategy.IF_NOT_PRESENT)
                .build();
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(context);

        assertThat(result).matches(r -> r.succeeded());
        verify(resolver, never()).resolve(any());
        verify(jsonLd).registerCachedDocument(eq(URL), any(JsonObject.class));
    }

    @Test
    void create_ifNotPresent_withoutContent_pullsFromUrl() {
        var context = CachedDocument.Builder.newInstance()
                .url(URL)
                .pullStrategy(PullStrategy.IF_NOT_PRESENT)
                .build();
        when(resolver.resolve(eq(URL))).thenReturn(Result.success(resolved("bar")));
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(context);

        assertThat(result).matches(r -> r.succeeded());
        assertThat(result.getContent().getContent()).contains("bar");
        verify(resolver).resolve(eq(URL));
        verify(jsonLd).registerCachedDocument(eq(URL), any(JsonObject.class));
    }

    @Test
    void create_ifNotPresent_pullFails_returnsBadRequest() {
        var context = CachedDocument.Builder.newInstance()
                .url(URL)
                .pullStrategy(PullStrategy.IF_NOT_PRESENT)
                .build();
        when(resolver.resolve(any())).thenReturn(Result.failure("boom"));

        var result = service.create(context);

        assertThat(result).matches(r -> r.failed());
        verify(store, never()).create(any());
        verify(jsonLd, never()).registerCachedDocument(any(), any(JsonObject.class));
    }

    @Test
    void create_always_pullsEvenWhenContentSupplied() {
        var context = CachedDocument.Builder.newInstance()
                .url(URL)
                .content("{\"@context\":{\"stale\":\"https://example.com/ns/stale\"}}")
                .pullStrategy(PullStrategy.ALWAYS)
                .build();
        when(resolver.resolve(eq(URL))).thenReturn(Result.success(resolved("fresh")));
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(context);

        assertThat(result).matches(r -> r.succeeded());
        assertThat(result.getContent().getContent()).contains("fresh").doesNotContain("stale");
        verify(resolver).resolve(eq(URL));
    }

    @Test
    void delete_unregistersFromJsonLd() {
        var context = CachedDocument.Builder.newInstance()
                .id("id1").url(URL).content("{}").build();
        when(store.delete("id1")).thenReturn(StoreResult.success(context));

        var result = service.deleteById("id1");

        assertThat(result).matches(r -> r.succeeded());
        verify(jsonLd).unregisterCachedDocument(URL);
    }

    @Test
    void refresh_always_reResolvesAndReRegisters() {
        var existing = CachedDocument.Builder.newInstance()
                .id("id1").url(URL).content("{}").pullStrategy(PullStrategy.ALWAYS).build();
        when(store.findById("id1")).thenReturn(existing);
        when(resolver.resolve(eq(URL))).thenReturn(Result.success(resolved("fresh")));
        when(store.update(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.refresh("id1");

        assertThat(result).matches(r -> r.succeeded());
        assertThat(result.getContent().getContent()).contains("fresh");
        verify(resolver).resolve(eq(URL));
        verify(jsonLd).registerCachedDocument(eq(URL), any(JsonObject.class));
    }

    @Test
    void refresh_never_reRegistersWithoutResolving() {
        var existing = CachedDocument.Builder.newInstance()
                .id("id1").url(URL)
                .content("{\"@context\":{\"foo\":\"https://example.com/ns/foo\"}}")
                .pullStrategy(PullStrategy.NEVER).build();
        when(store.findById("id1")).thenReturn(existing);

        var result = service.refresh("id1");

        assertThat(result).matches(r -> r.succeeded());
        verify(resolver, never()).resolve(any());
        verify(store, never()).update(any());
        verify(jsonLd).registerCachedDocument(eq(URL), any(JsonObject.class));
    }
}
