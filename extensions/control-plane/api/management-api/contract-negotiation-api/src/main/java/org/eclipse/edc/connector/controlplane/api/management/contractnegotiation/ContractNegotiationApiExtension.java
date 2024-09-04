/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectFromNegotiationStateTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractOfferTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToTerminateNegotiationCommandTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v2.ContractNegotiationApiV2Controller;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation.ContractRequestValidator;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation.TerminateNegotiationValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;

@Extension(value = ContractNegotiationApiExtension.NAME)
public class ContractNegotiationApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Negotiation";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private ContractNegotiationService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var factory = Json.createBuilderFactory(Map.of());
        var monitor = context.getMonitor();

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        managementApiTransformerRegistry.register(new JsonObjectToContractRequestTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToContractOfferTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToTerminateNegotiationCommandTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromNegotiationStateTransformer(factory));

        validatorRegistry.register(CONTRACT_REQUEST_TYPE, ContractRequestValidator.instance());
        validatorRegistry.register(TERMINATE_NEGOTIATION_TYPE, TerminateNegotiationValidator.instance());

        webService.registerResource(ApiContext.MANAGEMENT, new ContractNegotiationApiV2Controller(service, managementApiTransformerRegistry, monitor, validatorRegistry));
        webService.registerResource(ApiContext.MANAGEMENT, new ContractNegotiationApiV3Controller(service, managementApiTransformerRegistry, monitor, validatorRegistry));
    }
}
