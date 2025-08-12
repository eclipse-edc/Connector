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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactored
 *
 */

package org.eclipse.edc.spi.command;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Links together a Command and its handler class
 */
@ExtensionPoint
public interface CommandHandlerRegistry {

    /**
     * Registers the handler.
     */
    <C extends EntityCommand> void register(CommandHandler<C> handlerClass);

    /**
     * Execute a command.
     *
     * @param command the command to be executed
     * @return successful result if command is executed correctly, failed result otherwise
     * @param <C> the command type
     */
    <C extends EntityCommand> CommandResult execute(C command);
}
