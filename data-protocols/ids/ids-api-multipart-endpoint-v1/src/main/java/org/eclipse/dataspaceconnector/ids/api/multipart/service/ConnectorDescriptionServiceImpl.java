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

import de.fraunhofer.iais.eis.Connector;
import org.eclipse.dataspaceconnector.ids.api.multipart.factory.BaseConnectorFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.factory.ResourceCatalogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

public class ConnectorDescriptionServiceImpl implements ConnectorDescriptionService {
    private final BaseConnectorFactory baseConnectorFactory;
    private final ResourceCatalogFactory resourceCatalogFactory;

    public ConnectorDescriptionServiceImpl(
            @NotNull BaseConnectorFactory baseConnectorFactory,
            @NotNull ResourceCatalogFactory resourceCatalogFactory) {
        this.baseConnectorFactory = Objects.requireNonNull(baseConnectorFactory);
        this.resourceCatalogFactory = Objects.requireNonNull(resourceCatalogFactory);
    }

    @NotNull
    public Connector createSelfDescription() {
        var resourceCatalog = resourceCatalogFactory.createResourceCatalogBuilder(Collections.emptyList());

        return baseConnectorFactory.createBaseConnector(resourceCatalog);
    }
}
