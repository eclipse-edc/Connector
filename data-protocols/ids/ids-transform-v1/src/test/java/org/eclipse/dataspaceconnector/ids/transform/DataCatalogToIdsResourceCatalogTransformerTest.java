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
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataCatalogToIdsResourceCatalogTransformerTest {
    private static final String CATALOG_ID = "test_id";
    private static final URI EXPECTED_CATALOG_ID = URI.create("urn:catalog:test_id");

    // subject
    private DataCatalogToIdsResourceCatalogTransformer dataCatalogToIdsResourceCatalogTransformer;

    // mocks
    private DataCatalog dataCatalog;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        dataCatalogToIdsResourceCatalogTransformer = new DataCatalogToIdsResourceCatalogTransformer();
        dataCatalog = EasyMock.createMock(DataCatalog.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(dataCatalog, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            dataCatalogToIdsResourceCatalogTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(dataCatalog, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            dataCatalogToIdsResourceCatalogTransformer.transform(dataCatalog, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(dataCatalog, context);

        var result = dataCatalogToIdsResourceCatalogTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare
        Asset a1 = EasyMock.createMock(Asset.class);
        Asset a2 = EasyMock.createMock(Asset.class);
        ContractOffer o1 = EasyMock.createMock(ContractOffer.class);
        ContractOffer o2 = EasyMock.createMock(ContractOffer.class);

        EasyMock.expect(a1.getId()).andReturn("a1").anyTimes();
        EasyMock.expect(a2.getId()).andReturn("a2").anyTimes();
        EasyMock.expect(o1.getAssets()).andReturn(Collections.singletonList(a1)).anyTimes();
        EasyMock.expect(o2.getAssets()).andReturn(Collections.singletonList(a2)).anyTimes();

        Resource resource = new ResourceBuilder().build();

        EasyMock.expect(dataCatalog.getId()).andReturn(CATALOG_ID);
        EasyMock.expect(dataCatalog.getContractOffers()).andReturn(List.of(o1, o2));

        EasyMock.expect(context.transform(EasyMock.isA(OfferedAsset.class), EasyMock.eq(Resource.class))).andReturn(resource).anyTimes();

        // record
        EasyMock.replay(o1, o2, a1, a2, dataCatalog, context);

        // invoke
        var result = dataCatalogToIdsResourceCatalogTransformer.transform(dataCatalog, context);

        // verify
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EXPECTED_CATALOG_ID);
        assertThat(result.getOfferedResource()).hasSize(2);

        EasyMock.verify(o1, o2, a1, a2);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(dataCatalog, context);
    }
}