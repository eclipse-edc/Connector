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

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform.DataRequestToDataRequestDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform.TransferProcessToTransferProcessDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform.TransferRequestDtoToDataRequestTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

@Provides(TransferProcessService.class)
public class TransferProcessApiExtension implements ServiceExtension {
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
        return "Data Management API: Transfer Process";
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