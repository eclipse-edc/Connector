/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement;

import org.eclipse.dataspaceconnector.api.auth.AuthenticationRequestFilter;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.api.exception.mappers.EdcApiExceptionMapper;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(DataManagementApiConfiguration.class)
public class DataManagementApiExtension implements ServiceExtension {

    public static final String WEB_DATA_CONFIG = "web.http.data";
    private static final String DEFAULT_DATAMANAGEMENT_ALIAS = "default";

    @Inject
    private WebService webService;
    @Inject
    private AuthenticationService service;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var contextAlias = DEFAULT_DATAMANAGEMENT_ALIAS;

        // if no dedicated web.http.data.[port|path] config exists, we'll expose it under the default mapping
        if (context.getConfig().hasKey(WEB_DATA_CONFIG)) {
            contextAlias = "data";
        }

        context.registerService(DataManagementApiConfiguration.class, new DataManagementApiConfiguration(contextAlias));
        webService.registerResource(DEFAULT_DATAMANAGEMENT_ALIAS, new AuthenticationRequestFilter(service));
        webService.registerResource(DEFAULT_DATAMANAGEMENT_ALIAS, new EdcApiExceptionMapper());
    }
}
