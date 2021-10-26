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

package org.eclipse.dataspaceconnector.ids.api.multipart.service;

import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.ResourceCatalog;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.api.multipart.factory.BaseConnectorFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.factory.ResourceCatalogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectorDescriptionServiceImplTest {

    // subject
    private ConnectorDescriptionServiceImpl connectorDescriptionService;

    // mocks
    private BaseConnectorFactory baseConnectorFactory;
    private ResourceCatalogFactory resourceCatalogFactory;


    @BeforeEach
    void setUp() {
        baseConnectorFactory = EasyMock.mock(BaseConnectorFactory.class);
        resourceCatalogFactory = EasyMock.mock(ResourceCatalogFactory.class);

        connectorDescriptionService = new ConnectorDescriptionServiceImpl(baseConnectorFactory, resourceCatalogFactory);
    }

    @Test
    void createSelfDescription() {
        // prepare
        ResourceCatalog resourceCatalog = EasyMock.mock(ResourceCatalog.class);
        BaseConnector baseConnector = EasyMock.mock(BaseConnector.class);
        EasyMock.expect(resourceCatalogFactory.createResourceCatalogBuilder(EasyMock.anyObject())).andReturn(resourceCatalog);
        EasyMock.expect(baseConnectorFactory.createBaseConnector(resourceCatalog)).andReturn(baseConnector);

        EasyMock.replay(resourceCatalogFactory, baseConnectorFactory);

        // invoke
        Connector connector = connectorDescriptionService.createSelfDescription();

        // verify
        Assertions.assertEquals(baseConnector, connector);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(resourceCatalogFactory, baseConnectorFactory);
    }

}