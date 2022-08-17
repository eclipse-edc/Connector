/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.command;

import java.util.UUID;

/**
 * Base class for all command objects. Contains basic information such as a command ID
 * and an error count, which indicates how many times a particular command has already errored out. This is useful
 * if the command should be discarded after a few retries.
 * <p>
 * Please take note of the following guidelines:
 * <ul>
 * <li>Commands are simple POJOs, that must be (JSON-)serializable and can therefore not have references to other services.</li>
 * <li>Commands must contain all the information that a {@link CommandHandler} requires to do its job.</li>
 * <li>Commands do not have results. Any results that an operation may produce are to be handled by the {@link CommandHandler}</li>
 * </ul>
 */
public abstract class Command {
    private final String commandId;
    private int errorCount = 0;

    /**
     * Creates a new Command assigning a random UUID as commandId
     */
    protected Command() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Creates a new Command assigning a specified String as command ID
     */
    protected Command(String commandId) {
        this.commandId = commandId;
    }

    public String getCommandId() {
        return commandId;
    }

    public void increaseErrorCount() {
        errorCount++;
    }


    /**
     * Indicates whether {@link Command#getMaxRetry()} has been reached.
     */
    public boolean canRetry() {
        return errorCount < getMaxRetry();
    }

    /**
     * Indicates the maximum amount of times a Command should be retried. Defaults to 5.
     */
    protected int getMaxRetry() {
        return 5;
    }
}
