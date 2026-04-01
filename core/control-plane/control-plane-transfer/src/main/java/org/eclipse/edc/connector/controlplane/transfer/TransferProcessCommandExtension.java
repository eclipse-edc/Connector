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

import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.CompleteTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.InitiateTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.NotifyPreparedCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.NotifyStartedCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.ResumeTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.SuspendTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.command.handlers.TerminateTransferCommandHandler;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;

import java.time.Clock;

/**
 * Registers command handlers that the core provides
 */
public class TransferProcessCommandExtension implements ServiceExtension {

    @Inject
    private TransferProcessStore store;
    @Inject
    private TransferProcessObservable observable;
    @Inject
    private DataAddressStore dataAddressStore;
    @Inject
    private PolicyArchive policyArchive;
    @Inject
    private Clock clock;
    @Inject
    private Telemetry telemetry;
    @Inject
    private CommandHandlerRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.register(new InitiateTransferCommandHandler(policyArchive, store, dataAddressStore, observable, clock,
                telemetry, context.getMonitor()));
        registry.register(new TerminateTransferCommandHandler(store, observable));
        registry.register(new SuspendTransferCommandHandler(store, observable));
        registry.register(new ResumeTransferCommandHandler(store, observable));
        registry.register(new CompleteTransferCommandHandler(store, observable));
        registry.register(new NotifyPreparedCommandHandler(store, observable, dataAddressStore));
        registry.register(new NotifyStartedCommandHandler(store, observable, dataAddressStore));
    }

}
