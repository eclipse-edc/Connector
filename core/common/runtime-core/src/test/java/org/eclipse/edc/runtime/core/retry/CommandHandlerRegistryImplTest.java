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

package org.eclipse.edc.runtime.core.retry;

import org.eclipse.edc.runtime.core.command.CommandHandlerRegistryImpl;
import org.eclipse.edc.spi.command.CommandFailure;
import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.command.CommandResult;
import org.eclipse.edc.spi.command.EntityCommand;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.command.CommandFailure.Reason.NOT_EXECUTABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandHandlerRegistryImplTest {

    private final CommandHandler<?> handler = mock();
    private final CommandHandlerRegistryImpl registry = new CommandHandlerRegistryImpl();

    @Test
    void execute_shouldExecuteCommand() {
        doReturn(TestCommand.class).when(handler).getType();
        when(handler.handle(any())).thenReturn(CommandResult.success());
        registry.register(handler);
        var command = new TestCommand("id");

        var result = registry.execute(command);

        assertThat(result).isSucceeded();
    }

    @Test
    void execute_shouldReturnFailure_whenCommandHandlerNotFound() {
        var command = new TestCommand("id");

        var result = registry.execute(command);

        assertThat(result).isFailed().extracting(CommandFailure::getReason).isEqualTo(NOT_EXECUTABLE);
    }

    private static class TestCommand extends EntityCommand {

        TestCommand(String entityId) {
            super(entityId);
        }
    }
}
