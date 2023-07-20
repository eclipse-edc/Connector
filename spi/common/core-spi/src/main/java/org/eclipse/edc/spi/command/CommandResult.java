/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.command;

import org.eclipse.edc.spi.result.AbstractResult;

import java.util.List;

public class CommandResult extends AbstractResult<Void, CommandFailure, CommandResult> {

    public static CommandResult success() {
        return new CommandResult(null, null);
    }

    public static CommandResult notFound(String message) {
        return new CommandResult(null, new CommandFailure(List.of(message), CommandFailure.Reason.NOT_FOUND));
    }

    public static CommandResult notExecutable(String message) {
        return new CommandResult(null, new CommandFailure(List.of(message), CommandFailure.Reason.NOT_EXECUTABLE));
    }

    public static CommandResult conflict(String message) {
        return new CommandResult(null, new CommandFailure(List.of(message), CommandFailure.Reason.CONFLICT));
    }

    protected CommandResult(Void content, CommandFailure failure) {
        super(content, failure);
    }

    public CommandFailure.Reason reason() {
        return getFailure().getReason();
    }
}
