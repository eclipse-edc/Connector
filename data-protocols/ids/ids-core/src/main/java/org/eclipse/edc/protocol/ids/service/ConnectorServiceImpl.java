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
 *       ZF Friedrichshafen AG - enable asset filtering
 *
 */

package org.eclipse.edc.protocol.ids.service;

import org.eclipse.edc.protocol.ids.spi.domain.connector.Connector;
import org.eclipse.edc.protocol.ids.spi.service.CatalogService;
import org.eclipse.edc.protocol.ids.spi.service.ConnectorService;
import org.eclipse.edc.protocol.ids.spi.types.container.DescriptionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

public class ConnectorServiceImpl implements ConnectorService {
    private static final String SYSTEM_VERSION = "0.0.1-SNAPSHOT"; // TODO update before/during build

    private final ConnectorServiceSettings connectorServiceSettings;
    private final CatalogService dataCatalogService;

    public ConnectorServiceImpl(
            @NotNull ConnectorServiceSettings connectorServiceSettings,
            @NotNull CatalogService dataCatalogService) {
        this.connectorServiceSettings = Objects.requireNonNull(connectorServiceSettings);
        this.dataCatalogService = Objects.requireNonNull(dataCatalogService);
    }

    @Override
    public Connector getConnector(@NotNull DescriptionRequest descriptionRequest) {
        var catalog = dataCatalogService.getDataCatalog(descriptionRequest);

        return Connector.Builder
                .newInstance()
                .id(connectorServiceSettings.getId())
                .title(connectorServiceSettings.getTitle())
                .description(connectorServiceSettings.getDescription())
                .connectorVersion(SYSTEM_VERSION)
                .securityProfile(connectorServiceSettings.getSecurityProfile())
                .dataCatalogs(Collections.singletonList(catalog))
                .endpoint(connectorServiceSettings.getEndpoint())
                .maintainer(connectorServiceSettings.getMaintainer())
                .curator(connectorServiceSettings.getCurator())
                .build();
    }
}
