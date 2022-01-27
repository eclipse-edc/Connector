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
package org.eclipse.dataspaceconnector.transfer.core.command;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.command.CommandHandler;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;

import java.util.HashMap;
import java.util.Map;

public class CommandHandlerRegistryImpl implements CommandHandlerRegistry {
    private final Map<Class<? extends Command>, CommandHandler<?>> registrations;

    public CommandHandlerRegistryImpl() {
        registrations = new HashMap<>();
    }

    @Override
    public <C extends Command> void register(CommandHandler<C> handler) {
        registrations.put(handler.getType(), handler);
    }


    @Override
    public <C extends Command> CommandHandler<C> get(Class<C> commandClass) {
        return (CommandHandler<C>) registrations.get(commandClass);
    }
}
