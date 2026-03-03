/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.cache.query;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.eclipse.edc.catalog.test.TestUtil.createCatalog;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryServiceImplTest {

    private static final Catalog CATALOG_ABC = createCatalog("ABC");
    private static final Catalog CATALOG_DEF = createCatalog("DEF");
    private static final Catalog CATALOG_XYZ = createCatalog("XYZ");

    private final FederatedCatalogCache storeMock = mock();
    private final QueryServiceImpl queryService = new QueryServiceImpl(storeMock);

    @Test
    void getCatalog() {

        when(storeMock.query(any())).thenReturn(List.of(CATALOG_ABC, CATALOG_DEF, CATALOG_XYZ));

        var catalog = queryService.getCatalog(QuerySpec.none());
        assertThat(catalog).isSucceeded();
        Assertions.assertThat(catalog.getContent()).containsExactlyInAnyOrder(CATALOG_ABC, CATALOG_DEF, CATALOG_XYZ);
        verify(storeMock).query(any());
    }

    @Test
    void getCatalog_storeThrowsException() {
        when(storeMock.query(any())).thenThrow(new RuntimeException("test exception"));

        var catalog = queryService.getCatalog(QuerySpec.none());
        assertThat(catalog).isFailed()
                .detail().isEqualTo("test exception");
        verify(storeMock).query(any());
    }

    @Test
    void getCatalog_empty() {
        when(storeMock.query(any())).thenReturn(Collections.emptyList());

        var catalog = queryService.getCatalog(QuerySpec.none());
        assertThat(catalog).isSucceeded();
        Assertions.assertThat(catalog.getContent()).isEmpty();
        verify(storeMock).query(any());
    }
}