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
package org.eclipse.dataspaceconnector.core.base;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Abstract class for processing commands from a {@link CommandQueue} using a {@link CommandRunner}.
 * The CommandQueue, CommandRunner and Monitor have to be supplied by implementing classes.
 *
 * @param <C> the type of command a sub-class can process.
 */
public abstract class CommandQueueProcessor<C extends Command> {
    
    protected CommandQueue<C> commandQueue;
    
    protected CommandRunner<C> commandRunner;
    
    protected Monitor monitor;
    
    /**
     * Fetches a batch of commands from the {@link CommandQueue} and processes them using the
     * {@link CommandRunner}.
     *
     * @return the number of successfully processed commands.
     */
    protected int processCommandQueue() {
        var batchSize = 5;
        var commands = commandQueue.dequeue(batchSize);
        AtomicInteger successCount = new AtomicInteger(); //needs to be an atomic because lambda.
        
        commands.forEach(command -> {
            var commandResult = commandRunner.runCommand(command);
            if (commandResult.failed()) {
                //re-queue if possible
                if (command.canRetry()) {
                    monitor.warning(format("Could not process command [%s], will retry. error: %s", command.getClass(), commandResult.getFailureMessages()));
                    commandQueue.enqueue(command);
                } else {
                    monitor.severe(format("Command [%s] has exceeded its retry limit, will discard now", command.getClass()));
                }
            } else {
                monitor.debug(format("Successfully processed command [%s]", command.getClass()));
                successCount.getAndIncrement();
            }
        });
        return successCount.get();
    }
    
}
