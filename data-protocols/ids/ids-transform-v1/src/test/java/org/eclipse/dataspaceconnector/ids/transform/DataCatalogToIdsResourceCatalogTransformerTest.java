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
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataCatalogToIdsResourceCatalogTransformerTest {
    private static final String CATALOG_ID = "test_id";
    private static final URI ID_URI = URI.create("urn:test:test_id");

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
        Resource r1 = EasyMock.createMock(Resource.class);
        Resource r2 = EasyMock.createMock(Resource.class);
        List<Asset> assets = Arrays.asList(a1, a2);
        List<Resource> resources = Arrays.asList(r1, r2);
        IdsId id = IdsId.Builder.newInstance().value(CATALOG_ID).type(IdsType.CATALOG).build();

        EasyMock.expect(dataCatalog.getId()).andReturn(CATALOG_ID);
        EasyMock.expect(dataCatalog.getAssets()).andReturn(assets);

        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(ID_URI);

        EasyMock.expect(context.transform(EasyMock.eq(a1), EasyMock.eq(Resource.class))).andReturn(r1);
        EasyMock.expect(context.transform(EasyMock.eq(a2), EasyMock.eq(Resource.class))).andReturn(r2);

        // record
        EasyMock.replay(a1, a2, r1, r2, dataCatalog, context);

        // invoke
        var result = dataCatalogToIdsResourceCatalogTransformer.transform(dataCatalog, context);

        // verify
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ID_URI);
        assertThat(result.getOfferedResource()).hasSize(resources.size());
        assertThat(result.getOfferedResource().get(0)).isEqualTo(r1);
        assertThat(result.getOfferedResource().get(1)).isEqualTo(r2);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(dataCatalog, context);
    }
}