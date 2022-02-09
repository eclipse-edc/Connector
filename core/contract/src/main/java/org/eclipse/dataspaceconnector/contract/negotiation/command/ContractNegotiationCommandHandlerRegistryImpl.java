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
package org.eclipse.dataspaceconnector.contract.negotiation.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dataspaceconnector.spi.command.CommandHandler;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommandHandlerRegistry;

public class ContractNegotiationCommandHandlerRegistryImpl implements ContractNegotiationCommandHandlerRegistry {
    private final Map<Class<? extends ContractNegotiationCommand>, CommandHandler<?>> registrations;
    
    public ContractNegotiationCommandHandlerRegistryImpl() {
        this.registrations = new HashMap<>();
    }
    
    @Override
    public <T extends ContractNegotiationCommand> void register(CommandHandler<T> handler) {
        registrations.put(handler.getType(), handler);
    }
    
    @Override
    public <T extends ContractNegotiationCommand> CommandHandler<T> get(Class<T> commandClass) {
        return (CommandHandler<T>) registrations.get(commandClass);
    }
}
