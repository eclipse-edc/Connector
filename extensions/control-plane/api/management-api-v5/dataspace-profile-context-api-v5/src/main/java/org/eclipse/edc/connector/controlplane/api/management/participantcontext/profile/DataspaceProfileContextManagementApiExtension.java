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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile.v5.DataspaceProfileApiV5Controller;
import org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile.v5.DataspaceProfileContextApiV5Controller;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectFromDataspaceProfileContextTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectFromDataspaceProfileTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectToAssociateDataspaceProfileContextTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.to.JsonObjectToDataspaceProfileTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
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
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = DataspaceProfileContextManagementApiExtension.NAME)
public class DataspaceProfileContextManagementApiExtension implements ServiceExtension {

    public static final String NAME = "ParticipantContext config management API Extension";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ParticipantProfileService profileResolver;

    @Inject
    private Monitor monitor;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private DataspaceProfileService dataspaceProfileService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var factory = Json.createBuilderFactory(Map.of());
        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        managementApiTransformerRegistry.register(new JsonObjectFromDataspaceProfileContextTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectToAssociateDataspaceProfileContextTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromDataspaceProfileTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectToDataspaceProfileTransformer());

        var jsonLdInterceptor = new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version());

        webService.registerResource(ApiContext.MANAGEMENT, new DataspaceProfileContextApiV5Controller(authorizationService, profileResolver, managementApiTransformerRegistry, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, DataspaceProfileContextApiV5Controller.class, jsonLdInterceptor);

        webService.registerResource(ApiContext.MANAGEMENT, new DataspaceProfileApiV5Controller(dataspaceProfileService, managementApiTransformerRegistry, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, DataspaceProfileApiV5Controller.class, jsonLdInterceptor);

    }
}
