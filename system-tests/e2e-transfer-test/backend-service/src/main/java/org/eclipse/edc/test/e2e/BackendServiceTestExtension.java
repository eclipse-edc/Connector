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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.mapper.EdcApiExceptionMapper;
import org.eclipse.edc.web.spi.WebService;

public class BackendServiceTestExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return "[TEST] Backend service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var exposedHttpPort = context.getConfig().getInteger("web.http.port");
        webService.registerResource(new ProviderBackendApiController());
        webService.registerResource(new ConsumerBackendServiceController(context.getMonitor()));
        webService.registerResource(new BackendServiceHttpProvisionerController(context.getMonitor(), httpClient, typeManager, exposedHttpPort));
        webService.registerResource(new Oauth2TokenController(context.getMonitor()));
        webService.registerResource(new EdcApiExceptionMapper());
    }
}
