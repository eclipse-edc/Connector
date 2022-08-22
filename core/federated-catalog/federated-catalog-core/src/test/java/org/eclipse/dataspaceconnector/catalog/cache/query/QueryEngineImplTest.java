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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createOffer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryEngineImplTest {

    private static final ContractOffer ASSET_ABC = createOffer("ABC");
    private static final ContractOffer ASSET_DEF = createOffer("DEF");
    private static final ContractOffer ASSET_XYZ = createOffer("XYZ");

    @Test
    void getCatalog() {
        CacheQueryAdapterRegistry registry = mock(CacheQueryAdapterRegistry.class);

        when(registry.executeQuery(any())).thenReturn(QueryResponse.ok(List.of(ASSET_ABC, ASSET_DEF, ASSET_XYZ)));

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        QueryResponse catalog = queryEngine.getCatalog(FederatedCatalogCacheQuery.Builder.newInstance().build());
        assertThat(catalog.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        assertThat(catalog.getErrors()).isEmpty();
        assertThat(catalog.getOffers()).containsExactlyInAnyOrder(ASSET_ABC, ASSET_DEF, ASSET_XYZ);

        verify(registry).executeQuery(any());
    }

    @Test
    void getCatalog_withErrors() {
        CacheQueryAdapterRegistry registry = mock(CacheQueryAdapterRegistry.class);

        when(registry.executeQuery(any())).thenReturn(QueryResponse.Builder.newInstance()
                .offers(List.of(ASSET_ABC, ASSET_DEF, ASSET_XYZ))
                .error("some error")
                .build());

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        QueryResponse catalog = queryEngine.getCatalog(FederatedCatalogCacheQuery.Builder.newInstance().build());
        assertThat(catalog.getStatus()).isEqualTo(QueryResponse.Status.ACCEPTED);
        assertThat(catalog.getErrors()).hasSize(1).containsExactly("some error");
        assertThat(catalog.getOffers()).containsExactlyInAnyOrder(ASSET_ABC, ASSET_DEF, ASSET_XYZ);
        verify(registry).executeQuery(any());
    }

    @Test
    void getCatalog_notAccepted() {
        CacheQueryAdapterRegistry registry = mock(CacheQueryAdapterRegistry.class);

        when(registry.executeQuery(any())).thenReturn(QueryResponse.Builder.newInstance()
                .status(QueryResponse.Status.NO_ADAPTER_FOUND)
                .error("no adapter was found for that query")
                .build());

        QueryEngine queryEngine = new QueryEngineImpl(registry);

        QueryResponse catalog = queryEngine.getCatalog(FederatedCatalogCacheQuery.Builder.newInstance().build());
        assertThat(catalog.getStatus()).isEqualTo(QueryResponse.Status.NO_ADAPTER_FOUND);
        assertThat(catalog.getErrors()).hasSize(1);
        assertThat(catalog.getOffers()).isEmpty();
        verify(registry).executeQuery(any());
    }
}