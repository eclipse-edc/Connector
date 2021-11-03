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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.service;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataCatalogServiceImplTest {
    private static final String CATALOG_ID = "catalogId";

    // subject
    private DataCatalogServiceImpl dataCatalogService;

    // mocks
    private Monitor monitor;
    private DataCatalogServiceSettings dataCatalogServiceSettings;
    private AssetIndex assetIndex;

    @BeforeEach
    void setUp() {
        monitor = EasyMock.createMock(Monitor.class);
        dataCatalogServiceSettings = EasyMock.createMock(DataCatalogServiceSettings.class);
        assetIndex = EasyMock.createMock(AssetIndex.class);

        dataCatalogService = new DataCatalogServiceImpl(monitor, dataCatalogServiceSettings, assetIndex);
    }

    @Test
    void getDataCatalog() {
        // prepare
        List<Asset> assets = Arrays.asList(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        EasyMock.expect(assetIndex.queryAssets(EasyMock.anyObject(AssetSelectorExpression.class)))
                .andReturn(assets.stream());

        EasyMock.expect(dataCatalogServiceSettings.getCatalogId()).andReturn(CATALOG_ID);

        // record
        EasyMock.replay(monitor, dataCatalogServiceSettings, assetIndex);

        // invoke
        var result = dataCatalogService.getDataCatalog();

        // verify
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getAssets()).hasSameElementsAs(assets);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(monitor, dataCatalogServiceSettings, assetIndex);
    }
}