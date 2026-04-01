/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v5.ContractNegotiationApiV5Controller;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.transform.edc.contractagreement.from.JsonObjectFromContractAgreementTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from.JsonObjectFromNegotiationStateTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to.JsonObjectToContractOfferTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to.JsonObjectToContractRequestTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to.JsonObjectToTerminateNegotiationCommandTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to.JsonObjectToTerminateNegotiationTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = ContractNegotiationApiV5Extension.NAME)
public class ContractNegotiationApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Negotiation";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private ContractNegotiationService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private ParticipantContextService participantContextService;

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
        managementApiTransformerRegistry.register(new JsonObjectToTerminateNegotiationTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromNegotiationStateTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromContractAgreementTransformer(factory));

        authorizationService.addLookupFunction(ContractNegotiation.class, this::findContractNegotiation);

        webService.registerResource(ApiContext.MANAGEMENT, new ContractNegotiationApiV5Controller(service, participantContextService, authorizationService, managementApiTransformerRegistry, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractNegotiationApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));
    }

    private ParticipantResource findContractNegotiation(String ownerId, String assetId) {
        return service
                .search(QuerySpec.Builder.newInstance()
                        .filter(filterByParticipantContextId(ownerId))
                        .filter(new Criterion("id", "=", assetId))
                        .build()
                )
                .map(it -> it.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }
}
