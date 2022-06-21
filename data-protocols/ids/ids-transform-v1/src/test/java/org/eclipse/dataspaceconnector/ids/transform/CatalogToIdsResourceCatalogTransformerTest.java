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
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogToIdsResourceCatalogTransformerTest {
    private static final String CATALOG_ID = "test_id";
    private static final URI EXPECTED_CATALOG_ID = URI.create("urn:catalog:test_id");

    private CatalogToIdsResourceCatalogTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new CatalogToIdsResourceCatalogTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        assertThrows(NullPointerException.class, () -> {
            transformer.transform(Catalog.Builder.newInstance().build(), null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        assertThat(result).isNull();
    }

    @Test
    void testSuccessfulSimple() {
        var a1 = Asset.Builder.newInstance().id("a1").build();
        var a2 = Asset.Builder.newInstance().id("a2").build();
        ContractOffer o1 = ContractOffer.Builder.newInstance().id("o1").asset(a1).policy(Policy.Builder.newInstance().build()).build();
        ContractOffer o2 = ContractOffer.Builder.newInstance().id("o2").asset(a2).policy(Policy.Builder.newInstance().build()).build();
        Resource resource = new ResourceBuilder().build();
        var catalog = Catalog.Builder.newInstance()
                .id(CATALOG_ID)
                .contractOffers(List.of(o1, o2))
                .build();
        when(context.transform(isA(OfferedAsset.class), eq(Resource.class))).thenReturn(resource);

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EXPECTED_CATALOG_ID);
        assertThat(result.getOfferedResource()).hasSize(2);
        verify(context, times(2)).transform(isA(OfferedAsset.class), eq(Resource.class));
    }

}
