/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.crawler.cache.query;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.eclipse.edc.catalog.crawler.TestUtil.createCatalog;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryServiceImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final Catalog CATALOG_ABC = createCatalog("ABC");
    private static final Catalog CATALOG_DEF = createCatalog("DEF");
    private static final Catalog CATALOG_XYZ = createCatalog("XYZ");

    private final FederatedCatalogCache storeMock = mock();
    private final QueryServiceImpl queryService = new QueryServiceImpl(storeMock);

    @Test
    void getCatalog() {

        when(storeMock.query(any(), any())).thenReturn(List.of(CATALOG_ABC, CATALOG_DEF, CATALOG_XYZ));

        var catalog = queryService.getCatalog(PARTICIPANT_CONTEXT_ID, QuerySpec.none());
        assertThat(catalog).isSucceeded();
        Assertions.assertThat(catalog.getContent()).containsExactlyInAnyOrder(CATALOG_ABC, CATALOG_DEF, CATALOG_XYZ);
        verify(storeMock).query(eq(PARTICIPANT_CONTEXT_ID), any());
    }

    @Test
    void getCatalog_storeThrowsException() {
        when(storeMock.query(any(), any())).thenThrow(new RuntimeException("test exception"));

        var catalog = queryService.getCatalog(PARTICIPANT_CONTEXT_ID, QuerySpec.none());
        assertThat(catalog).isFailed()
                .detail().isEqualTo("test exception");
        verify(storeMock).query(eq(PARTICIPANT_CONTEXT_ID), any());
    }

    @Test
    void getCatalog_empty() {
        when(storeMock.query(any(), any())).thenReturn(Collections.emptyList());

        var catalog = queryService.getCatalog(PARTICIPANT_CONTEXT_ID, QuerySpec.none());
        assertThat(catalog).isSucceeded();
        Assertions.assertThat(catalog.getContent()).isEmpty();
        verify(storeMock).query(eq(PARTICIPANT_CONTEXT_ID), any());
    }
}