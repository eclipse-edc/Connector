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

package org.eclipse.edc.connector.transfer;

import org.eclipse.edc.connector.transfer.command.handlers.CancelTransferCommandHandler;
import org.eclipse.edc.connector.transfer.command.handlers.CompleteTransferCommandHandler;
import org.eclipse.edc.connector.transfer.command.handlers.DeprovisionRequestHandler;
import org.eclipse.edc.connector.transfer.command.handlers.FailTransferCommandHandler;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Registers command handlers that the core provides
 */
@CoreExtension
public class TransferProcessCommandExtension implements ServiceExtension {

    @Inject
    private TransferProcessStore store;

    @Inject
    private TransferProcessObservable observable;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var registry = context.getService(CommandHandlerRegistry.class);

        registry.register(new CancelTransferCommandHandler(store, observable));
        registry.register(new DeprovisionRequestHandler(store));
        registry.register(new CompleteTransferCommandHandler(store, observable));
        registry.register(new FailTransferCommandHandler(store, observable));
    }

}
