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

package org.eclipse.edc.connector.api.management.transferprocess;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.transferprocess.transform.DataRequestToDataRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.JsonObjectFromDataRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.JsonObjectFromTransferProcessDtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.JsonObjectFromTransferStateTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.JsonObjectToTerminateTransferDtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.JsonObjectToTransferRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.TransferProcessToTransferProcessDtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.transform.TransferRequestDtoToTransferRequestTransformer;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension(value = TransferProcessApiExtension.NAME)
public class TransferProcessApiExtension implements ServiceExtension {
    public static final String NAME = "Management API: Transfer Process";
    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration configuration;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private TransferProcessService service;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper(JSON_LD);

        transformerRegistry.register(new DataRequestToDataRequestDtoTransformer());
        transformerRegistry.register(new TransferProcessToTransferProcessDtoTransformer());
        transformerRegistry.register(new TransferRequestDtoToTransferRequestTransformer());

        var builderFactory = Json.createBuilderFactory(emptyMap());
        transformerRegistry.register(new JsonObjectFromDataRequestDtoTransformer(builderFactory));
        transformerRegistry.register(new JsonObjectFromTransferProcessDtoTransformer(builderFactory, mapper));
        transformerRegistry.register(new JsonObjectFromTransferStateTransformer(builderFactory));

        transformerRegistry.register(new JsonObjectToTerminateTransferDtoTransformer());
        transformerRegistry.register(new JsonObjectToTransferRequestDtoTransformer());

        var newController = new TransferProcessApiController(context.getMonitor(), service, transformerRegistry);
        webService.registerResource(configuration.getContextAlias(), newController);
    }
}
