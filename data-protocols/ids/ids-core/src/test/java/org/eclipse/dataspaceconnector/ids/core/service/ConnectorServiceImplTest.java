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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.ids.core.service;

import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectorServiceImplTest {
    private static final String CONNECTOR_ID = "edc";
    private static final String CONNECTOR_TITLE = "connectorTitle";
    private static final String CONNECTOR_DESCRIPTION = "connectorDescription";
    private static final SecurityProfile CONNECTOR_SECURITY_PROFILE = SecurityProfile.TRUST_PLUS_SECURITY_PROFILE;
    private static final URI CONNECTOR_ENDPOINT = URI.create("https://example.com/connector/endpoint");
    private static final URI CONNECTOR_MAINTAINER = URI.create("https://example.com/connector/maintainer");
    private static final URI CONNECTOR_CURATOR = URI.create("https://example.com/connector/curator");
    private static final String CONNECTOR_VERSION = "connectorVersion";
    private final ConnectorServiceSettings connectorServiceSettings = mock(ConnectorServiceSettings.class);
    private final ConnectorVersionProvider connectorVersionProvider = mock(ConnectorVersionProvider.class);
    private final CatalogService dataCatalogService = mock(CatalogService.class);

    private ConnectorServiceImpl connectorService;

    @BeforeEach
    void setUp() {
        connectorService = new ConnectorServiceImpl(mock(Monitor.class), connectorServiceSettings, connectorVersionProvider, dataCatalogService);
    }

    @Test
    void getConnector() {
        when(dataCatalogService.getDataCatalog(any())).thenReturn(mock(Catalog.class));
        when(connectorServiceSettings.getId()).thenReturn(CONNECTOR_ID);
        when(connectorServiceSettings.getTitle()).thenReturn(CONNECTOR_TITLE);
        when(connectorServiceSettings.getDescription()).thenReturn(CONNECTOR_DESCRIPTION);
        when(connectorServiceSettings.getSecurityProfile()).thenReturn(CONNECTOR_SECURITY_PROFILE);
        when(connectorServiceSettings.getEndpoint()).thenReturn(CONNECTOR_ENDPOINT);
        when(connectorServiceSettings.getMaintainer()).thenReturn(CONNECTOR_MAINTAINER);
        when(connectorServiceSettings.getCurator()).thenReturn(CONNECTOR_CURATOR);
        when(connectorVersionProvider.getVersion()).thenReturn(CONNECTOR_VERSION);
        var claimToken = ClaimToken.Builder.newInstance().build();

        var result = connectorService.getConnector(claimToken);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CONNECTOR_ID);
        assertThat(result.getTitle()).isEqualTo(CONNECTOR_TITLE);
        assertThat(result.getDescription()).isEqualTo(CONNECTOR_DESCRIPTION);
        assertThat(result.getSecurityProfile()).isEqualTo(CONNECTOR_SECURITY_PROFILE);
        assertThat(result.getEndpoint()).isEqualTo(CONNECTOR_ENDPOINT);
        assertThat(result.getMaintainer()).isEqualTo(CONNECTOR_MAINTAINER);
        assertThat(result.getCurator()).isEqualTo(CONNECTOR_CURATOR);
        assertThat(result.getConnectorVersion()).isEqualTo(CONNECTOR_VERSION);
        verify(dataCatalogService).getDataCatalog(any());
        verify(connectorServiceSettings).getId();
        verify(connectorServiceSettings).getTitle();
        verify(connectorServiceSettings).getDescription();
        verify(connectorServiceSettings).getSecurityProfile();
        verify(connectorServiceSettings).getEndpoint();
        verify(connectorServiceSettings).getMaintainer();
        verify(connectorServiceSettings).getCurator();
        verify(connectorVersionProvider).getVersion();
    }

}