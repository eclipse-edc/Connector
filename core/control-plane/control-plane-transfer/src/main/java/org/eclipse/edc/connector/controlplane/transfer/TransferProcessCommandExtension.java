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

package org.eclipse.edc.connector.controlplane.transfer;

import org.eclipse.edc.connector.controlplane.transfer.command.handlers.CompleteProvisionCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.CompleteTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.DeprovisionRequestCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.ResumeTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.SuspendTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.TerminateTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Registers command handlers that the core provides
 */
public class TransferProcessCommandExtension implements ServiceExtension {

    @Inject
    private TransferProcessStore store;

    @Inject
    private TransferProcessObservable observable;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var registry = context.getService(CommandHandlerRegistry.class);

        registry.register(new TerminateTransferCommandHandler(store));
        registry.register(new SuspendTransferCommandHandler(store));
        registry.register(new ResumeTransferCommandHandler(store));
        registry.register(new DeprovisionRequestCommandHandler(store));
        registry.register(new CompleteTransferCommandHandler(store));
        registry.register(new CompleteProvisionCommandHandler(store, observable));
    }

}
