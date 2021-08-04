/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
