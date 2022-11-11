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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.datamanagement.contractdefinition;

import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.datamanagement.contractdefinition.transform.ContractDefinitionRequestDtoToContractDefinitionTransformer;
import org.eclipse.edc.connector.api.datamanagement.contractdefinition.transform.ContractDefinitionToContractDefinitionResponseDtoTransformer;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = ContractDefinitionApiExtension.NAME)
public class ContractDefinitionApiExtension implements ServiceExtension {
    public static final String NAME = "Management API: Contract Definition";
    @Inject
    WebService webService;

    @Inject
    ManagementApiConfiguration config;

    @Inject
    DtoTransformerRegistry transformerRegistry;

    @Inject
    ContractDefinitionService service;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        transformerRegistry.register(new ContractDefinitionToContractDefinitionResponseDtoTransformer());
        transformerRegistry.register(new ContractDefinitionRequestDtoToContractDefinitionTransformer());

        var monitor = context.getMonitor();

        webService.registerResource(config.getContextAlias(), new ContractDefinitionApiController(monitor, service, transformerRegistry));
    }
}
