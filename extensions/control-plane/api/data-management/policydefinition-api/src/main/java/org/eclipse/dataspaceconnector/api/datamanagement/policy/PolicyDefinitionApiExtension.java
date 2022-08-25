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

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.transform.PolicyDefinitionRequestDtoToPolicyDefinitionTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.transform.PolicyDefinitionToPolicyDefinitionResponseDtoTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(PolicyDefinitionService.class)
public class PolicyDefinitionApiExtension implements ServiceExtension {

    @Inject
    private DtoTransformerRegistry transformerRegistry;
    @Inject
    private WebService webService;
    @Inject
    private DataManagementApiConfiguration configuration;
    @Inject
    private PolicyDefinitionService service;

    @Override
    public String name() {
        return "Data Management API: Policy";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new PolicyDefinitionRequestDtoToPolicyDefinitionTransformer());
        transformerRegistry.register(new PolicyDefinitionToPolicyDefinitionResponseDtoTransformer());

        var monitor = context.getMonitor();

        webService.registerResource(configuration.getContextAlias(), new PolicyDefinitionApiController(monitor, service, transformerRegistry));
    }
}
