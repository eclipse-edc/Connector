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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.v5.TransferProcessApiV5Controller;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.from.JsonObjectFromTransferStateTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to.JsonObjectToSuspendTransferTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to.JsonObjectToTerminateTransferTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to.JsonObjectToTransferRequestTransformer;
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

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = TransferProcessApiV5Extension.NAME)
public class TransferProcessApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Transfer Process";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private TransferProcessService service;

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
        var builderFactory = Json.createBuilderFactory(emptyMap());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        managementApiTransformerRegistry.register(new JsonObjectFromTransferProcessTransformer(builderFactory));
        managementApiTransformerRegistry.register(new JsonObjectFromTransferStateTransformer(builderFactory));

        managementApiTransformerRegistry.register(new JsonObjectToTerminateTransferTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToSuspendTransferTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToTransferRequestTransformer());

        authorizationService.addLookupFunction(TransferProcess.class, this::findTransferProcess);

        webService.registerResource(ApiContext.MANAGEMENT, new TransferProcessApiV5Controller(context.getMonitor(), authorizationService, participantContextService, service, managementApiTransformerRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, TransferProcessApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));
    }

    private ParticipantResource findTransferProcess(String ownerId, String assetId) {
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
