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
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */

package org.eclipse.dataspaceconnector.spi.command;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

/**
 * Used to execute a single {@link Command}. The necessary steps are:
 * <ol>
 *     <li>Obtain a {@link CommandHandler} instance for a given command</li>
 *     <li>run the command</li>
 *     <li>convert exception into a {@link Result}, increase error count</li>
 * </ol>
 */
public class CommandRunner<C extends Command> {
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final Monitor monitor;

    public CommandRunner(CommandHandlerRegistry commandHandlerRegistry, Monitor monitor) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.monitor = monitor;
    }

    public <T extends C> Result<Void> runCommand(T command) {

        @SuppressWarnings("unchecked") var commandClass = (Class<T>) command.getClass();

        var handler = commandHandlerRegistry.get(commandClass);
        if (handler == null) {
            command.increaseErrorCount();
            return Result.failure("No command handler found for command type " + commandClass);
        }
        try {
            handler.handle(command);
            return Result.success();
        } catch (RuntimeException ex) {
            command.increaseErrorCount();
            monitor.severe("Error when processing a command", ex);
            return Result.failure(ex.getMessage());
        }
    }

}
