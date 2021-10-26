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

import de.fraunhofer.iais.eis.Resource;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

public class ResourceCatalogFactoryTest {

    private ResourceCatalogFactory resourceCatalogFactory;

    @BeforeEach
    void beforeEach() {
        resourceCatalogFactory = new ResourceCatalogFactory();
    }

    @Test
    void doesNotThrowOnArgumentNull() {
        Assertions.assertDoesNotThrow(() -> resourceCatalogFactory.createResourceCatalogBuilder(null));
    }

    @Test
    void addsResourcesToCatalog() {
        var expected = EasyMock.mock(Resource.class);
        var expectedResources = new ArrayList<Resource>(Collections.singletonList((Resource) expected));

        var catalog = resourceCatalogFactory.createResourceCatalogBuilder(expectedResources);
        var resources = catalog.getOfferedResource();

        Assertions.assertNotNull(resources);
        Assertions.assertEquals(1, resources.size());
        Assertions.assertEquals(expected, resources.get(0));
    }
}
