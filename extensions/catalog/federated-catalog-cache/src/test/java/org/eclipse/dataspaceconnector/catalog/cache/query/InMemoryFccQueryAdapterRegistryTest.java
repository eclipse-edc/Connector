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

package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
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
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createOffer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InMemoryFccQueryAdapterRegistryTest {
    private static final ContractOffer ASSET_ABC = createOffer("ABC");
    private static final ContractOffer ASSET_DEF = createOffer("DEF");
    private static final ContractOffer ASSET_XYZ = createOffer("XYZ");
    private CacheQueryAdapterRegistryImpl registry;


    @Test
    void getAllAdapters() {
        // initialize
        CacheQueryAdapter adapter1 = mock(CacheQueryAdapter.class);
        CacheQueryAdapter adapter2 = mock(CacheQueryAdapter.class);
        CacheQueryAdapter adapter3 = mock(CacheQueryAdapter.class);

        when(adapter1.executeQuery(any())).thenReturn(Stream.of(ASSET_ABC));
        when(adapter2.executeQuery(any())).thenReturn(Stream.of(ASSET_DEF));
        when(adapter3.executeQuery(any())).thenReturn(Stream.of(ASSET_XYZ));

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        Collection<CacheQueryAdapter> adapters = registry.getAllAdapters();

        assertThat(collectAssetsFromAdapters(adapters)).isEqualTo(Arrays.asList(ASSET_ABC, ASSET_DEF, ASSET_XYZ));
        verify(adapter1).executeQuery(any());
        verify(adapter2).executeQuery(any());
        verify(adapter3).executeQuery(any());
    }

    @BeforeEach
    public void setUp() {
        registry = new CacheQueryAdapterRegistryImpl();
    }

    @Test
    void executeQuery_whenNoAdapter() {
        var result = registry.executeQuery(mock(FederatedCatalogCacheQuery.class));

        assertThat(result).isNotNull();
        assertThat(result.getOffers()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.NO_ADAPTER_FOUND);
    }

    @Test
    void executeQuery_whenNotAllMatch() {
        var adapter1 = matchingAdapter();
        var adapter2 = matchingAdapter();
        var adapter3 = mismatchingAdapter();

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        var result = registry.executeQuery(mock(FederatedCatalogCacheQuery.class));
        assertThat(result.getOffers()).hasSize(6);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
    }

    @Test
    void executeQuery_whenAllSucceed() {
        var adapter1 = matchingAdapter();
        var adapter2 = matchingAdapter();

        registry.register(adapter1);
        registry.register(adapter2);

        var result = registry.executeQuery(mock(FederatedCatalogCacheQuery.class));
        assertThat(result.getOffers()).hasSize(6);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
    }

    @Test
    void executeQuery_whenSomeFail() {
        var adapter1 = matchingAdapter();
        var adapter2 = matchingAdapter();
        var adapter3 = failingAdapter();

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        var result = registry.executeQuery(mock(FederatedCatalogCacheQuery.class));
        assertThat(result.getOffers()).hasSize(6);
        assertThat(result.getErrors()).isNotEmpty().hasSize(1);
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
    }

    @Test
    void executeQuery_whenAllFail() {
        var adapter1 = failingAdapter();
        var adapter2 = failingAdapter();
        var adapter3 = failingAdapter();

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        var result = registry.executeQuery(mock(FederatedCatalogCacheQuery.class));
        assertThat(result.getOffers()).hasSize(0);
        assertThat(result.getErrors()).isNotEmpty().hasSize(3);
        assertThat(result.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
    }

    private CacheQueryAdapter failingAdapter() {
        CacheQueryAdapter adapter1 = mock(CacheQueryAdapter.class);
        when(adapter1.canExecute(any())).thenReturn(true);
        when(adapter1.executeQuery(any())).thenThrow(new EdcException("timeout"));
        return adapter1;
    }

    @NotNull
    private CacheQueryAdapter matchingAdapter() {
        CacheQueryAdapter adapter1 = mock(CacheQueryAdapter.class);
        when(adapter1.canExecute(any())).thenReturn(true);
        Supplier<ContractOffer> as = () -> createOffer("test-offer");
        when(adapter1.executeQuery(any())).thenReturn(Stream.of(as.get(), as.get(), as.get()));
        return adapter1;
    }

    @NotNull
    private CacheQueryAdapter mismatchingAdapter() {
        CacheQueryAdapter adapter1 = mock(CacheQueryAdapter.class);
        when(adapter1.canExecute(any())).thenReturn(false);
        return adapter1;
    }

    private List<ContractOffer> collectAssetsFromAdapters(Collection<CacheQueryAdapter> adapters) {
        List<ContractOffer> assets = new ArrayList<>();
        adapters.forEach(cacheQueryAdapter -> assets.addAll(cacheQueryAdapter.executeQuery(null).collect(Collectors.toList())));
        return assets;
    }
}
