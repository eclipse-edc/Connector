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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactored
 *
 */

package org.eclipse.edc.runtime.core.command;

import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.command.CommandResult;
import org.eclipse.edc.spi.command.EntityCommand;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Implementation of the {@link CommandHandlerRegistry} interface.
 */
public class CommandHandlerRegistryImpl implements CommandHandlerRegistry {

    private final Map<Class<? extends EntityCommand>, CommandHandler<?>> registrations;

    public CommandHandlerRegistryImpl() {
        this.registrations = new HashMap<>();
    }

    @Override
    public <C extends EntityCommand> void register(CommandHandler<C> handler) {
        registrations.put(handler.getType(), handler);
    }

    @Override
    public <C extends EntityCommand> CommandResult execute(C command) {
        var commandHandler = (CommandHandler<C>) registrations.get(command.getClass());

        if (commandHandler == null) {
            return CommandResult.notExecutable(format("Command type %s cannot be executed", command.getClass()));
        }

        return commandHandler.handle(command);
    }
}
