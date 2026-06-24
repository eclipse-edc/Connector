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

/**
 * CommandHandlers receive a Command object and act on it. If possible, command handlers should
 * not perform lengthy operations as this could block the command queue.
 *
 * @param <T> The concrete type of Command
 */
public interface CommandHandler<T extends EntityCommand> {

    /**
     * Returns the type of Command this handler processes.
     */
    Class<T> getType();

    /**
     * Processes the command.
     */
    CommandResult handle(T command);

}
