/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogToIdsResourceCatalogTransformerTest {
    private static final String CATALOG_ID = "test_id";
    private static final URI EXPECTED_CATALOG_ID = URI.create("urn:catalog:test_id");

    private CatalogToIdsResourceCatalogTransformer transformer;

    private Catalog catalog;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new CatalogToIdsResourceCatalogTransformer();
        catalog = mock(Catalog.class);
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(catalog, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        Asset a1 = mock(Asset.class);
        Asset a2 = mock(Asset.class);
        ContractOffer o1 = mock(ContractOffer.class);
        ContractOffer o2 = mock(ContractOffer.class);

        when(a1.getId()).thenReturn("a1");
        when(a2.getId()).thenReturn("a2");
        when(o1.getAsset()).thenReturn(a1);
        when(o2.getAsset()).thenReturn(a2);

        Resource resource = new ResourceBuilder().build();

        when(catalog.getId()).thenReturn(CATALOG_ID);
        when(catalog.getContractOffers()).thenReturn(List.of(o1, o2));

        when(context.transform(isA(OfferedAsset.class), eq(Resource.class))).thenReturn(resource);

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EXPECTED_CATALOG_ID);
        assertThat(result.getOfferedResource()).hasSize(2);
    }

}
