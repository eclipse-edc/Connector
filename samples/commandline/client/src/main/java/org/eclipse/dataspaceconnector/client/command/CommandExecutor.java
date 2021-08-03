/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.client.command;

/**
 * Executes a client command.
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Executes the command.
     *
     * @param context the execution context
     */
    CommandResult execute(ExecutionContext context);

}
