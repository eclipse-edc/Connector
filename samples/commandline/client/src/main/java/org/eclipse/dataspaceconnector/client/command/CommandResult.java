/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.client.command;

/**
 * The result of a command execution.
 */
public class CommandResult {
    public static final CommandResult OK_RESULT = new CommandResult();

    private boolean error;
    private String message;

    public CommandResult(boolean error, String message) {
        this.error = error;
        this.message = message;
    }

    public CommandResult(String message) {
        this.message = message;
    }

    private CommandResult() {
    }

    public boolean error() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
