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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.api.management.contractagreement;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.contractagreement.transform.ContractAgreementToContractAgreementDtoTransformer;
import org.eclipse.edc.connector.api.management.contractagreement.transform.JsonObjectFromContractAgreementDtoTransformer;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;

@Extension(value = ContractAgreementApiExtension.NAME)
public class ContractAgreementApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Agreement";
    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration config;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private ContractAgreementService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractAgreementToContractAgreementDtoTransformer());
        transformerRegistry.register(new JsonObjectFromContractAgreementDtoTransformer(Json.createBuilderFactory(Map.of())));
        var monitor = context.getMonitor();

        var controller = new ContractAgreementApiController(service, transformerRegistry, monitor, validatorRegistry);
        webService.registerResource(config.getContextAlias(), controller);
    }
}
