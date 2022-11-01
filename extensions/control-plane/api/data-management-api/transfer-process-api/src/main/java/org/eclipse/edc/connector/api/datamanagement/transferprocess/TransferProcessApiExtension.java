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

package org.eclipse.edc.connector.api.datamanagement.transferprocess;

import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.service.TransferProcessServiceImpl;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.transform.DataRequestToDataRequestDtoTransformer;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.transform.TransferProcessToTransferProcessDtoTransformer;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.transform.TransferRequestDtoToDataRequestTransformer;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;

@Provides(TransferProcessService.class)
@Extension(value = TransferProcessApiExtension.NAME)
public class TransferProcessApiExtension implements ServiceExtension {
    public static final String NAME = "Data Management API: Transfer Process";
    @Inject
    private WebService webService;

    @Inject
    private DataManagementApiConfiguration configuration;

    @Inject
    private DtoTransformerRegistry transformerRegistry;

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private TransferProcessManager manager;

    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var service = new TransferProcessServiceImpl(transferProcessStore, manager, transactionContext);
        context.registerService(TransferProcessService.class, service);

        var controller = new TransferProcessApiController(context.getMonitor(), service, transformerRegistry);
        webService.registerResource(configuration.getContextAlias(), controller);

        transformerRegistry.register(new DataRequestToDataRequestDtoTransformer());
        transformerRegistry.register(new TransferProcessToTransferProcessDtoTransformer());
        transformerRegistry.register(new TransferRequestDtoToDataRequestTransformer());
    }
}
