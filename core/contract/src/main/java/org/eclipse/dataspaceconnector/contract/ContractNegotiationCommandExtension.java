/*
 *  Copyright (c) 2021-2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.contract.negotiation.command.handlers.CancelNegotiationCommandHandler;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.system.CoreExtension;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Adds a {@link CommandHandlerRegistry} to the context and registers the
 * handlers the core provides.
 */
@CoreExtension
@Provides({CommandHandlerRegistry.class})
public class ContractNegotiationCommandExtension implements ServiceExtension {

    @Inject
    private ContractNegotiationStore store;

    @Override
    public void initialize(ServiceExtensionContext context) {
        CommandHandlerRegistry registry = context.getService(CommandHandlerRegistry.class);
        registerDefaultCommandHandlers(registry);
    }

    private void registerDefaultCommandHandlers(CommandHandlerRegistry registry) {
        registry.register(new CancelNegotiationCommandHandler(store));
    }

}
