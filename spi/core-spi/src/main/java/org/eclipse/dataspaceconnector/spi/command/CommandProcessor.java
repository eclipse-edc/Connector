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
package org.eclipse.dataspaceconnector.spi.command;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import static java.lang.String.format;

/**
 * Processes commands from a {@link CommandQueue} using a {@link CommandRunner}.
 *
 * @param <C> the type of command that can be processed.
 */
public class CommandProcessor<C extends Command> {

    private CommandQueue<C> commandQueue;
    private CommandRunner<C> commandRunner;
    private Monitor monitor;

    public CommandProcessor(CommandQueue<C> commandQueue, CommandRunner<C> commandRunner, Monitor monitor) {
        this.commandQueue = commandQueue;
        this.commandRunner = commandRunner;
        this.monitor = monitor;
    }

    /**
     * Processes the given command using a {@link CommandRunner}. If processing the command fails,
     * it is enqueued in the {@link CommandQueue} again.
     *
     * @param command the Command to process.
     * @return true, if the command has successfully been processed; false otherwise.
     */
    public boolean processCommandQueue(C command) {
        var commandResult = commandRunner.runCommand(command);
        if (commandResult.failed()) {
            if (command.canRetry()) {
                monitor.warning(format("Could not process command [%s], will retry. Error: %s", command.getClass(), commandResult.getFailureMessages()));
                commandQueue.enqueue(command);
            } else {
                monitor.severe(format("Could not process command [%s], it has exceeded its retry limit, will discard now. Error: %s", command.getClass(), commandResult.getFailureMessages()));
            }
            return false;
        } else {
            monitor.debug(format("Successfully processed command [%s]", command.getClass()));
            return true;
        }
    }

}
