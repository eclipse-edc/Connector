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

package org.eclipse.dataspaceconnector.ids.api.multipart.factory;

import de.fraunhofer.iais.eis.BaseConnector;
import org.assertj.core.api.Assertions;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

class BaseConnectorFactoryTest {

    private static class Fixtures {
        public static final URI ID = URI.create("https://example.com/connector");
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final URI MAINTAINER = URI.create("https://example.com/maintainer");
        public static final URI CURATOR = URI.create("https://example.com/curator");
        public static final URI CONNECTOR_ENDPOINT = URI.create("https://example.com/connector/endpoint");
        public static final SecurityProfile SECURITY_PROFILE = SecurityProfile.BASE_SECURITY_PROFILE;
        public static final String CONNECTOR_VERSION = "0.0.1";
    }

    // Mocks
    private BaseConnectorFactorySettings baseConnectorFactorySettings;
    private ConnectorVersionProvider connectorVersionProvider;

    @BeforeEach
    public void setUp() {
        // prepare/instantiate mock instances
        baseConnectorFactorySettings = EasyMock.createMock(BaseConnectorFactorySettings.class);
        connectorVersionProvider = EasyMock.createMock(ConnectorVersionProvider.class);
    }

    @AfterEach
    public void tearDown() {
        // verify - no more invocations on mock
        EasyMock.verify(baseConnectorFactorySettings, connectorVersionProvider);
    }

    @Test
    void testBaseConnectorFactoryReturnsAsExpected() {
        // prepare
        BaseConnectorFactory baseConnectorFactory = new BaseConnectorFactory(
                baseConnectorFactorySettings,
                connectorVersionProvider
        );

        EasyMock.expect(baseConnectorFactorySettings.getId()).andReturn(Fixtures.ID).times(1);
        EasyMock.expect(baseConnectorFactorySettings.getTitle()).andReturn(Fixtures.TITLE).times(1);
        EasyMock.expect(baseConnectorFactorySettings.getDescription()).andReturn(Fixtures.DESCRIPTION).times(1);
        EasyMock.expect(baseConnectorFactorySettings.getMaintainer()).andReturn(Fixtures.MAINTAINER).times(1);
        EasyMock.expect(baseConnectorFactorySettings.getCurator()).andReturn(Fixtures.CURATOR).times(1);
        EasyMock.expect(baseConnectorFactorySettings.getConnectorEndpoint()).andReturn(Fixtures.CONNECTOR_ENDPOINT).times(1);
        EasyMock.expect(baseConnectorFactorySettings.getSecurityProfile()).andReturn(Fixtures.SECURITY_PROFILE).times(1);

        EasyMock.expect(connectorVersionProvider.getVersion()).andReturn(Fixtures.CONNECTOR_VERSION).times(1);

        EasyMock.replay(baseConnectorFactorySettings, connectorVersionProvider);

        // invoke
        BaseConnector connector = baseConnectorFactory.createBaseConnector();

        // verify
        Assertions.assertThat(Fixtures.TITLE).isEqualTo(connector.getTitle().get(0).getValue());
        Assertions.assertThat(Fixtures.DESCRIPTION).isEqualTo(connector.getDescription().get(0).getValue());
        Assertions.assertThat(Fixtures.CONNECTOR_ENDPOINT).isEqualTo(connector.getHasDefaultEndpoint().getAccessURL());
        Assertions.assertThat(Fixtures.MAINTAINER).isEqualTo(connector.getMaintainer());
        Assertions.assertThat(Fixtures.CURATOR).isEqualTo(connector.getCurator());

        Assertions.assertThat(Fixtures.CONNECTOR_VERSION).isEqualTo(connector.getVersion());
    }
}