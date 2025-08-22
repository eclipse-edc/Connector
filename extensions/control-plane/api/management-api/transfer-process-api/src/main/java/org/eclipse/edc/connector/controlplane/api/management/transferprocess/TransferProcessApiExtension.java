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
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.transferprocess;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.transform.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.transform.JsonObjectFromTransferStateTransformer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.transform.JsonObjectToSuspendTransferTransformer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.transform.JsonObjectToTerminateTransferTransformer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.transform.JsonObjectToTransferRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3.TransferProcessApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.v4.TransferProcessApiV4Controller;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.validation.TerminateTransferValidator;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.validation.TransferRequestValidator;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
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

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.TerminateTransfer.TERMINATE_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = TransferProcessApiExtension.NAME)
public class TransferProcessApiExtension implements ServiceExtension {

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

        validatorRegistry.register(TRANSFER_REQUEST_TYPE, TransferRequestValidator.instance(context.getMonitor()));
        validatorRegistry.register(TERMINATE_TRANSFER_TYPE, TerminateTransferValidator.instance());

        webService.registerResource(ApiContext.MANAGEMENT, new TransferProcessApiV3Controller(context.getMonitor(), service, managementApiTransformerRegistry, validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, TransferProcessApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        webService.registerResource(ApiContext.MANAGEMENT, new TransferProcessApiV4Controller(context.getMonitor(), service, managementApiTransformerRegistry, validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, TransferProcessApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));
    }
}
