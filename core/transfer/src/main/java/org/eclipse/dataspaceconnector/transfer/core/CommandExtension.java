/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

import org.eclipse.dataspaceconnector.core.CoreExtension;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.transfer.core.command.CommandHandlerRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.CancelTransferCommandHandler;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.DeprovisionRequestHandler;

/**
 * Registers command handlers that the core provides
 */
@CoreExtension
@Provides({ CommandHandlerRegistry.class })
public class CommandExtension implements ServiceExtension {

    @Inject
    private TransferProcessStore store;

    @Override
    public void initialize(ServiceExtensionContext context) {

        CommandHandlerRegistryImpl registry = new CommandHandlerRegistryImpl();
        context.registerService(CommandHandlerRegistry.class, registry);

        registerDefaultCommands(registry);
    }

    private void registerDefaultCommands(CommandHandlerRegistryImpl registry) {
        registry.register(new CancelTransferCommandHandler(store));
        registry.register(new DeprovisionRequestHandler(store));
    }

}