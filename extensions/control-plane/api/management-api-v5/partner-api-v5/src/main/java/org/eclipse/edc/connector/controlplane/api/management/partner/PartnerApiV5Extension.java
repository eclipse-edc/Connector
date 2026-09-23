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

package org.eclipse.edc.connector.controlplane.api.management.partner;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.connector.controlplane.api.management.partner.v5.PartnerApiV5Controller;
import org.eclipse.edc.connector.controlplane.api.management.partner.v5.PartnerGroupApiV5Controller;
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerGroupService;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerService;
import org.eclipse.edc.connector.controlplane.transform.edc.partner.from.JsonObjectFromPartnerGroupTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.partner.from.JsonObjectFromPartnerTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.partner.to.JsonObjectToPartnerGroupTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.partner.to.JsonObjectToPartnerTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = PartnerApiV5Extension.NAME)
public class PartnerApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Partners and Partner Groups";

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private PartnerService partnerService;
    @Inject
    private PartnerGroupService partnerGroupService;
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
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        managementApiTransformerRegistry.register(new JsonObjectToPartnerTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromPartnerTransformer(jsonBuilderFactory));
        managementApiTransformerRegistry.register(new JsonObjectToPartnerGroupTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromPartnerGroupTransformer(jsonBuilderFactory));

        authorizationService.addLookupFunction(Partner.class, this::findPartner);
        authorizationService.addLookupFunction(PartnerGroup.class, this::findPartnerGroup);

        var jsonLdInterceptor = new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4);

        webService.registerResource(ApiContext.MANAGEMENT, new PartnerApiV5Controller(partnerService, managementApiTransformerRegistry, monitor, authorizationService));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, PartnerApiV5Controller.class, jsonLdInterceptor);

        webService.registerResource(ApiContext.MANAGEMENT, new PartnerGroupApiV5Controller(partnerGroupService, managementApiTransformerRegistry, monitor, authorizationService));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, PartnerGroupApiV5Controller.class, jsonLdInterceptor);
    }

    private ParticipantResource findPartner(String participantContextId, String id) {
        return partnerService.findById(participantContextId, id).orElse(f -> null);
    }

    private ParticipantResource findPartnerGroup(String participantContextId, String id) {
        return partnerGroupService.findById(participantContextId, id).orElse(f -> null);
    }
}
