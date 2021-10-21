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
package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;


class InMemoryCacheCacheQueryAdapterRegistryTest {
    private static final Asset ASSET_ABC = Asset.Builder.newInstance().id("ABC").build();
    private static final Asset ASSET_DEF = Asset.Builder.newInstance().id("DEF").build();
    private static final Asset ASSET_XYZ = Asset.Builder.newInstance().id("XYZ").build();

    private InMemoryCacheQueryAdapterRegistry registry;

    @Test
    void getAllAdapters() {
        // initialize
        CacheQueryAdapter adapter1 = strictMock(CacheQueryAdapter.class);
        CacheQueryAdapter adapter2 = strictMock(CacheQueryAdapter.class);
        CacheQueryAdapter adapter3 = strictMock(CacheQueryAdapter.class);

        expect(adapter1.executeQuery(anyObject())).andReturn(Stream.of(ASSET_ABC));
        expect(adapter2.executeQuery(anyObject())).andReturn(Stream.of(ASSET_DEF));
        expect(adapter3.executeQuery(anyObject())).andReturn(Stream.of(ASSET_XYZ));

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        replay(adapter1, adapter2, adapter3);

        // start testing
        Collection<CacheQueryAdapter> adapters = registry.getAllAdapters();

        assertThat(collectAssetsFromAdapters(adapters)).isEqualTo(Arrays.asList(ASSET_ABC, ASSET_DEF, ASSET_XYZ));

        verify(adapter1, adapter2, adapter3);
    }

    @BeforeEach
    public void setUp() {
        registry = new InMemoryCacheQueryAdapterRegistry();
    }

    @Test
    void executeQuery_whenNoAdapter() {
        var result = registry.executeQuery(niceMock(CacheQuery.class));

        assertThat(result).isNotNull();
        assertThat(result.getAssets()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.NO_ADAPTER_FOUND);
    }

    @Test
    void executeQuery_whenNotAllMatch() {
        var adapter1 = matchingAdapter();
        var adapter2 = matchingAdapter();
        var adapter3 = mismatchingAdapter();
        replay(adapter1, adapter2, adapter3);

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        var result = registry.executeQuery(niceMock(CacheQuery.class));
        assertThat(result.getAssets()).hasSize(6);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        verify(adapter1, adapter2, adapter3);
    }

    @Test
    void executeQuery_whenAllSucceed() {
        var adapter1 = matchingAdapter();
        var adapter2 = matchingAdapter();
        replay(adapter1, adapter2);

        registry.register(adapter1);
        registry.register(adapter2);

        var result = registry.executeQuery(niceMock(CacheQuery.class));
        assertThat(result.getAssets()).hasSize(6);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        verify(adapter1, adapter2);
    }

    @Test
    void executeQuery_whenSomeFail() {
        var adapter1 = matchingAdapter();
        var adapter2 = matchingAdapter();
        var adapter3 = failingAdapter();
        replay(adapter1, adapter2, adapter3);

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        var result = registry.executeQuery(niceMock(CacheQuery.class));
        assertThat(result.getAssets()).hasSize(6);
        assertThat(result.getErrors()).isNotEmpty().hasSize(1);
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        verify(adapter1, adapter2, adapter3);
    }

    @Test
    void executeQuery_whenAllFail() {
        var adapter1 = failingAdapter();
        var adapter2 = failingAdapter();
        var adapter3 = failingAdapter();
        replay(adapter1, adapter2, adapter3);

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        var result = registry.executeQuery(niceMock(CacheQuery.class));
        assertThat(result.getAssets()).hasSize(0);
        assertThat(result.getErrors()).isNotEmpty().hasSize(3);
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        verify(adapter1, adapter2, adapter3);
    }

    private CacheQueryAdapter failingAdapter() {
        CacheQueryAdapter adapter1 = niceMock(CacheQueryAdapter.class);
        expect(adapter1.canExecute(anyObject())).andReturn(true);
        expect(adapter1.executeQuery(anyObject())).andThrow(new EdcException("timeout"));
        return adapter1;
    }

    @NotNull
    private CacheQueryAdapter matchingAdapter() {
        CacheQueryAdapter adapter1 = niceMock(CacheQueryAdapter.class);
        expect(adapter1.canExecute(anyObject())).andReturn(true);
        Supplier<Asset> as = () -> Asset.Builder.newInstance().build();
        expect(adapter1.executeQuery(anyObject())).andReturn(Stream.of(as.get(), as.get(), as.get()));
        return adapter1;
    }

    @NotNull
    private CacheQueryAdapter mismatchingAdapter() {
        CacheQueryAdapter adapter1 = niceMock(CacheQueryAdapter.class);
        expect(adapter1.canExecute(anyObject())).andReturn(false);
        return adapter1;
    }

    private List<Asset> collectAssetsFromAdapters(Collection<CacheQueryAdapter> adapters) {
        List<Asset> assets = new ArrayList<>();
        adapters.forEach(cacheQueryAdapter -> assets.addAll(cacheQueryAdapter.executeQuery(null).collect(Collectors.toList())));
        return assets;
    }
}
