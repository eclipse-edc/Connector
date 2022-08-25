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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.ContractDefinitionRequestDtoToContractDefinitionTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.ContractDefinitionToContractDefinitionResponseDtoTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.definition.service.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class ContractDefinitionApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private DataManagementApiConfiguration config;

    @Inject
    private DtoTransformerRegistry transformerRegistry;

    @Inject
    private ContractDefinitionService service;

    @Override
    public String name() {
        return "Data Management API: Contract Definition";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractDefinitionToContractDefinitionResponseDtoTransformer());
        transformerRegistry.register(new ContractDefinitionRequestDtoToContractDefinitionTransformer());

        var monitor = context.getMonitor();

        webService.registerResource(config.getContextAlias(), new ContractDefinitionApiController(monitor, service, transformerRegistry));
    }
}