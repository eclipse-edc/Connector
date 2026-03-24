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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement;

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.contractagreement.v5.ContractAgreementApiV5Controller;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.jsonld.spi.JsonLd;
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

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_V_4;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = ContractAgreementApiV5Extension.NAME)
public class ContractAgreementApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Agreement";
    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private ContractAgreementService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var managementApiTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        var managementApiTransformerRegistryV4 = managementApiTransformerRegistry.forContext(MANAGEMENT_API_V_4);

        authorizationService.addLookupFunction(ContractAgreement.class, this::findContractAgreement);

        webService.registerResource(ApiContext.MANAGEMENT, new ContractAgreementApiV5Controller(service, authorizationService, managementApiTransformerRegistryV4, monitor, validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractAgreementApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));

    }

    private ParticipantResource findContractAgreement(String ownerId, String agreementId) {
        return service
                .search(QuerySpec.Builder.newInstance()
                        .filter(filterByParticipantContextId(ownerId))
                        .filter(new Criterion("id", "=", agreementId))
                        .build()
                )
                .map(it -> it.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }
}
