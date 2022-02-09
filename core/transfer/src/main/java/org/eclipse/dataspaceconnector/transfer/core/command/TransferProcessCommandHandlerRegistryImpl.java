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
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */
package org.eclipse.dataspaceconnector.transfer.core.command;

import org.eclipse.dataspaceconnector.spi.command.CommandHandler;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommandHandlerRegistry;

import java.util.HashMap;
import java.util.Map;

public class TransferProcessCommandHandlerRegistryImpl implements TransferProcessCommandHandlerRegistry {
    private final Map<Class<? extends TransferProcessCommand>, CommandHandler<?>> registrations;

    public TransferProcessCommandHandlerRegistryImpl() {
        registrations = new HashMap<>();
    }
    
    @Override
    public <T extends TransferProcessCommand> void register(CommandHandler<T> handler) {
        registrations.put(handler.getType(), handler);
    }
    
    @Override
    public <T extends TransferProcessCommand> CommandHandler<T> get(Class<T> commandClass) {
        return (CommandHandler<T>) registrations.get(commandClass);
    }
}
