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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement;

import org.eclipse.edc.connector.controlplane.api.management.contractagreement.v3.ContractAgreementApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.contractagreement.v4alpha.ContractAgreementApiV4AlphaController;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_V_4_ALPHA;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = ContractAgreementApiExtension.NAME)
public class ContractAgreementApiExtension implements ServiceExtension {

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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var managementApiTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        var managementApiTransformerRegistryV4Alpha = managementApiTransformerRegistry.forContext(MANAGEMENT_API_V_4_ALPHA);

        webService.registerResource(ApiContext.MANAGEMENT, new ContractAgreementApiV3Controller(service, managementApiTransformerRegistry, monitor, validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractAgreementApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        webService.registerResource(ApiContext.MANAGEMENT, new ContractAgreementApiV4AlphaController(service, managementApiTransformerRegistryV4Alpha, monitor, validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractAgreementApiV4AlphaController.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

    }
}
