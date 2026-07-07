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

package org.eclipse.edc.jsonld.cache.spi.store.testfixtures;

import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.jsonld.cache.spi.PullStrategy;
import org.eclipse.edc.jsonld.cache.spi.store.CachedJsonLdContextStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

public abstract class CachedJsonLdContextStoreTestBase {

    protected abstract CachedJsonLdContextStore getStore();

    protected CachedJsonLdContext createContext(String id, String url) {
        return CachedJsonLdContext.Builder.newInstance()
                .id(id)
                .url(url)
                .content("{\"@context\":{\"foo\":\"https://example.com/foo\"}}")
                .pullStrategy(PullStrategy.NEVER)
                .build();
    }

    private String randomId() {
        return UUID.randomUUID().toString();
    }

    @Test
    void create_findById() {
        var context = createContext(randomId(), "https://example.com/context.jsonld");

        var result = getStore().create(context);

        assertThat(result).extracting(r -> r.getContent()).usingRecursiveComparison().isEqualTo(context);
        assertThat(getStore().findById(context.getId())).usingRecursiveComparison().isEqualTo(context);
    }

    @Test
    void create_whenExists_shouldReturnAlreadyExists() {
        var context = createContext(randomId(), "https://example.com/context.jsonld");
        getStore().create(context);

        var result = getStore().create(context);

        assertThat(result).extracting(r -> r.reason()).isEqualTo(ALREADY_EXISTS);
    }

    @Test
    void findByUrl() {
        var url = "https://example.com/by-url.jsonld";
        var context = createContext(randomId(), url);
        getStore().create(context);

        assertThat(getStore().findByUrl(url)).usingRecursiveComparison().isEqualTo(context);
        assertThat(getStore().findByUrl("https://example.com/missing.jsonld")).isNull();
    }

    @Test
    void findById_whenMissing_shouldReturnNull() {
        assertThat(getStore().findById("missing")).isNull();
    }

    @Test
    void update() {
        var context = createContext(randomId(), "https://example.com/context.jsonld");
        getStore().create(context);

        var updated = context.toBuilder().content("{\"@context\":{\"bar\":\"https://example.com/bar\"}}").build();
        var result = getStore().update(updated);

        assertThat(result.succeeded()).isTrue();
        assertThat(getStore().findById(context.getId()).getContent()).contains("bar");
    }

    @Test
    void update_whenMissing_shouldReturnNotFound() {
        var context = createContext(randomId(), "https://example.com/context.jsonld");

        var result = getStore().update(context);

        assertThat(result).extracting(r -> r.reason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete() {
        var context = createContext(randomId(), "https://example.com/context.jsonld");
        getStore().create(context);

        var result = getStore().delete(context.getId());

        assertThat(result.succeeded()).isTrue();
        assertThat(getStore().findById(context.getId())).isNull();
    }

    @Test
    void delete_whenMissing_shouldReturnNotFound() {
        var result = getStore().delete("missing");

        assertThat(result).extracting(r -> r.reason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void findAll() {
        IntStream.range(0, 5).forEach(i -> getStore().create(createContext(randomId(), "https://example.com/context-" + i + ".jsonld")));

        var result = getStore().findAll(QuerySpec.max()).toList();

        assertThat(result).hasSize(5);
    }

    @Test
    void findAll_withFilter() {
        var target = createContext(randomId(), "https://example.com/target.jsonld");
        getStore().create(target);
        getStore().create(createContext(randomId(), "https://example.com/other.jsonld"));

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("url", "=", "https://example.com/target.jsonld"))
                .build();

        assertThat(getStore().findAll(query).toList()).hasSize(1)
                .allSatisfy(c -> assertThat(c.getId()).isEqualTo(target.getId()));
    }
}
