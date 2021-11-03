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
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectorServiceImplTest {
    private static final String CONNECTOR_ID = "connectorId";
    private static final String CONNECTOR_TITLE = "connectorTitle";
    private static final String CONNECTOR_DESCRIPTION = "connectorDescription";
    private static final SecurityProfile CONNECTOR_SECURITY_PROFILE = SecurityProfile.TRUST_PLUS_SECURITY_PROFILE;
    private static final URI CONNECTOR_ENDPOINT = URI.create("https://example.com/connector/endpoint");
    private static final URI CONNECTOR_MAINTAINER = URI.create("https://example.com/connector/maintainer");
    private static final URI CONNECTOR_CURATOR = URI.create("https://example.com/connector/curator");
    private static final String CONNECTOR_VERSION = "connectorVersion";

    // subject
    private ConnectorServiceImpl connectorService;

    // mocks
    private Monitor monitor;
    private ConnectorServiceSettings connectorServiceSettings;
    private ConnectorVersionProvider connectorVersionProvider;
    private DataCatalogService dataCatalogService;

    @BeforeEach
    void setUp() {
        monitor = EasyMock.createMock(Monitor.class);
        connectorServiceSettings = EasyMock.createMock(ConnectorServiceSettings.class);
        connectorVersionProvider = EasyMock.createMock(ConnectorVersionProvider.class);
        dataCatalogService = EasyMock.createMock(DataCatalogService.class);

        connectorService = new ConnectorServiceImpl(monitor, connectorServiceSettings, connectorVersionProvider, dataCatalogService);
    }

    @Test
    void getConnector() {
        // prepare
        DataCatalog dataCatalog = EasyMock.createMock(DataCatalog.class);

        EasyMock.expect(dataCatalogService.getDataCatalog()).andReturn(dataCatalog);
        EasyMock.expect(connectorServiceSettings.getId()).andReturn(CONNECTOR_ID);
        EasyMock.expect(connectorServiceSettings.getTitle()).andReturn(CONNECTOR_TITLE);
        EasyMock.expect(connectorServiceSettings.getDescription()).andReturn(CONNECTOR_DESCRIPTION);
        EasyMock.expect(connectorServiceSettings.getSecurityProfile()).andReturn(CONNECTOR_SECURITY_PROFILE);
        EasyMock.expect(connectorServiceSettings.getEndpoint()).andReturn(CONNECTOR_ENDPOINT);
        EasyMock.expect(connectorServiceSettings.getMaintainer()).andReturn(CONNECTOR_MAINTAINER);
        EasyMock.expect(connectorServiceSettings.getCurator()).andReturn(CONNECTOR_CURATOR);

        EasyMock.expect(connectorVersionProvider.getVersion()).andReturn(CONNECTOR_VERSION);
        // record
        EasyMock.replay(monitor, connectorServiceSettings, connectorVersionProvider, dataCatalogService);

        // invoke
        var result = connectorService.getConnector();

        // verify
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CONNECTOR_ID);
        assertThat(result.getTitle()).isEqualTo(CONNECTOR_TITLE);
        assertThat(result.getDescription()).isEqualTo(CONNECTOR_DESCRIPTION);
        assertThat(result.getSecurityProfile()).isEqualTo(CONNECTOR_SECURITY_PROFILE);
        assertThat(result.getEndpoint()).isEqualTo(CONNECTOR_ENDPOINT);
        assertThat(result.getMaintainer()).isEqualTo(CONNECTOR_MAINTAINER);
        assertThat(result.getCurator()).isEqualTo(CONNECTOR_CURATOR);
        assertThat(result.getConnectorVersion()).isEqualTo(CONNECTOR_VERSION);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(monitor, connectorServiceSettings, connectorVersionProvider, dataCatalogService);
    }
}