/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.ContractAgreementToContractAgreementDtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.ContractNegotiationToContractNegotiationDtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectFromContractAgreementDtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectFromContractNegotiationDtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectFromNegotiationStateTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectToContractOfferDescriptionTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectToNegotiationInitiateRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.NegotiationInitiateRequestDtoToDataRequestTransformer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.time.Clock;
import java.util.Map;

@Extension(value = ContractNegotiationApiExtension.NAME)
public class ContractNegotiationApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Negotiation";

    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration config;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private ContractNegotiationService service;

    @Inject
    private Clock clock;

    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractNegotiationToContractNegotiationDtoTransformer());
        transformerRegistry.register(new ContractAgreementToContractAgreementDtoTransformer());
        transformerRegistry.register(new NegotiationInitiateRequestDtoToDataRequestTransformer(clock));
        var factory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new JsonObjectFromContractNegotiationDtoTransformer(factory));
        transformerRegistry.register(new JsonObjectFromContractAgreementDtoTransformer(factory));
        transformerRegistry.register(new JsonObjectToNegotiationInitiateRequestDtoTransformer());
        transformerRegistry.register(new JsonObjectFromNegotiationStateTransformer(factory));
        transformerRegistry.register(new JsonObjectToContractOfferDescriptionTransformer());

        var monitor = context.getMonitor();

        webService.registerResource(config.getContextAlias(), new ContractNegotiationApiController(monitor, service, transformerRegistry));
        webService.registerResource(config.getContextAlias(), new ContractNegotiationNewApiController(service, transformerRegistry, jsonLd, monitor));
    }
}
