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

package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.CancelTransferCommandHandler;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.CompleteTransferCommandHandler;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.DeprovisionRequestHandler;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.FailTransferCommandHandler;

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
