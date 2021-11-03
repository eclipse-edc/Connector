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

import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The IDS service is able to create IDS compliant description of a data catalog.
 * That description may be used to create a self-description or answer a Description Request Message.
 */
public class DataCatalogServiceImpl implements DataCatalogService {
    private final Monitor monitor;
    private final DataCatalogServiceSettings dataCatalogServiceSettings;
    private final AssetIndex assetIndex;

    public DataCatalogServiceImpl(
            @NotNull Monitor monitor,
            @NotNull DataCatalogServiceSettings dataCatalogServiceSettings,
            @NotNull AssetIndex assetIndex) {
        this.monitor = monitor;
        this.dataCatalogServiceSettings = Objects.requireNonNull(dataCatalogServiceSettings);
        this.assetIndex = Objects.requireNonNull(assetIndex);
    }

    /**
     * Provides the dataCatalog object, which may be used by the IDS self-description of the connector.
     *
     * @return data catalog
     */
    @Override
    public DataCatalog getDataCatalog() {
        Stream<Asset> assetStream = assetIndex
                .queryAssets(AssetSelectorExpression.Builder.newInstance().build());

        return DataCatalog.Builder
                .newInstance()
                .id(dataCatalogServiceSettings.getCatalogId())
                .assets(assetStream.collect(Collectors.toList()))
                .build();
    }
}
