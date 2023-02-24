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

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.ids.spi.types.container.OfferedAsset;
import org.eclipse.edc.protocol.ids.transform.type.connector.CatalogToIdsResourceCatalogTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void testSuccessfulSimple() {
        var a1 = Asset.Builder.newInstance().id("a1").build();
        var a2 = Asset.Builder.newInstance().id("a2").build();
        var o1 = createContractOffer("o1", a1);
        var o2 = createContractOffer("o2", a2);
        var resource = new ResourceBuilder().build();
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

    private static ContractOffer createContractOffer(String id, Asset asset) {
        return ContractOffer.Builder.newInstance()
                .id(id)
                .asset(asset)
                .policy(Policy.Builder.newInstance().build())
                .contractStart(ZonedDateTime.now().toInstant().toEpochMilli())
                .contractEnd(ZonedDateTime.now().toInstant().toEpochMilli())
                .build();
    }
}
