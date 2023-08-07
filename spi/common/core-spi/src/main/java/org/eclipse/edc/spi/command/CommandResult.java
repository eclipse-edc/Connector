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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandResult extends AbstractResult<Void, CommandFailure, CommandResult> {

    protected CommandResult(Void content, CommandFailure failure) {
        super(content, failure);
    }

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

    public CommandFailure.Reason reason() {
        return getFailure().getReason();
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected <R1 extends AbstractResult<C1, CommandFailure, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable CommandFailure failure) {
        return (R1) new CommandResult(null, failure);
    }
}
