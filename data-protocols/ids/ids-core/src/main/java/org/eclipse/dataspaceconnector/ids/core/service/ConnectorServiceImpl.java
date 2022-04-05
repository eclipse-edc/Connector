/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.ids.core.service;

import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.types.Connector;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

public class ConnectorServiceImpl implements ConnectorService {
    private final Monitor monitor;
    private final ConnectorServiceSettings connectorServiceSettings;
    private final ConnectorVersionProvider connectorVersionProvider;
    private final CatalogService dataCatalogService;

    public ConnectorServiceImpl(
            @NotNull Monitor monitor,
            @NotNull ConnectorServiceSettings connectorServiceSettings,
            @NotNull ConnectorVersionProvider connectorVersionProvider,
            @NotNull CatalogService dataCatalogService) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorServiceSettings = Objects.requireNonNull(connectorServiceSettings);
        this.connectorVersionProvider = Objects.requireNonNull(connectorVersionProvider);
        this.dataCatalogService = Objects.requireNonNull(dataCatalogService);
    }

    @NotNull
    @Override
    public Connector getConnector(@NotNull ClaimToken claimToken) {
        Objects.requireNonNull(claimToken);

        Catalog catalog = dataCatalogService.getDataCatalog(claimToken);

        return Connector.Builder
                .newInstance()
                .id(connectorServiceSettings.getId())
                .title(connectorServiceSettings.getTitle())
                .description(connectorServiceSettings.getDescription())
                .connectorVersion(connectorVersionProvider.getVersion())
                .securityProfile(connectorServiceSettings.getSecurityProfile())
                .dataCatalogs(Collections.singletonList(catalog))
                .endpoint(connectorServiceSettings.getEndpoint())
                .maintainer(connectorServiceSettings.getMaintainer())
                .curator(connectorServiceSettings.getCurator())
                .build();
    }
}
