/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog;

import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class CatalogApiExtension implements ServiceExtension {
    @Inject
    WebService webService;

    @Inject
    DataManagementApiConfiguration config;

    @Inject
    RemoteMessageDispatcherRegistry dispatcher;

    @Override
    public String name() {
        return "Data Management API: Catalog";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var service = new CatalogServiceImpl(dispatcher);
        webService.registerResource(config.getContextAlias(), new CatalogApiController(service));
    }
}
