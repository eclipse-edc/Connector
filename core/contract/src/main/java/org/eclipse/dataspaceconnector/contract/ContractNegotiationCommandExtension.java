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

import org.eclipse.dataspaceconnector.contract.negotiation.command.ContractNegotiationCommandHandlerRegistryImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.command.handlers.CancelNegotiationCommandHandler;
import org.eclipse.dataspaceconnector.core.CoreExtension;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommandHandlerRegistry;

/**
 * Adds a {@link ContractNegotiationCommandHandlerRegistry} to the context and registers the
 * handlers the core provides.
 */
@CoreExtension
@Provides({ContractNegotiationCommandHandlerRegistry.class})
public class ContractNegotiationCommandExtension implements ServiceExtension {
    
    @Inject
    private ContractNegotiationStore store;
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        ContractNegotiationCommandHandlerRegistry registry = new ContractNegotiationCommandHandlerRegistryImpl();
        context.registerService(ContractNegotiationCommandHandlerRegistry.class, registry);
        
        registerDefaultCommandHandlers(registry);
    }
    
    private void registerDefaultCommandHandlers(ContractNegotiationCommandHandlerRegistry registry) {
        registry.register(new CancelNegotiationCommandHandler(store));
    }
    
}
