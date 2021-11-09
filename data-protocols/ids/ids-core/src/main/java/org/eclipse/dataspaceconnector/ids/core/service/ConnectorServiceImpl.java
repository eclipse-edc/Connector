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

import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.types.Connector;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

public class ConnectorServiceImpl implements ConnectorService {
    private final Monitor monitor;
    private final ConnectorServiceSettings connectorServiceSettings;
    private final ConnectorVersionProvider connectorVersionProvider;
    private final DataCatalogService dataCatalogService;

    public ConnectorServiceImpl(
            @NotNull Monitor monitor,
            @NotNull ConnectorServiceSettings connectorServiceSettings,
            @NotNull ConnectorVersionProvider connectorVersionProvider,
            @NotNull DataCatalogService dataCatalogService) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorServiceSettings = Objects.requireNonNull(connectorServiceSettings);
        this.connectorVersionProvider = Objects.requireNonNull(connectorVersionProvider);
        this.dataCatalogService = Objects.requireNonNull(dataCatalogService);
    }

    @NotNull
    public Connector getConnector() {
        DataCatalog dataCatalog = dataCatalogService.getDataCatalog();

        return Connector.Builder
                .newInstance()
                .id(connectorServiceSettings.getId())
                .title(connectorServiceSettings.getTitle())
                .description(connectorServiceSettings.getDescription())
                .connectorVersion(connectorVersionProvider.getVersion())
                .securityProfile(connectorServiceSettings.getSecurityProfile())
                .dataCatalogs(dataCatalog != null ? Collections.singletonList(dataCatalog) : Collections.emptyList())
                .endpoint(connectorServiceSettings.getEndpoint())
                .maintainer(connectorServiceSettings.getMaintainer())
                .curator(connectorServiceSettings.getCurator())
                .build();
    }
}
