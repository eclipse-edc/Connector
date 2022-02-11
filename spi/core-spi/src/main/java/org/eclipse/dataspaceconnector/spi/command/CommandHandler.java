/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */
package org.eclipse.dataspaceconnector.spi.command;

/**
 * CommandHandlers receive a {@link Command} object and act on it. If possible, command handlers should
 * not perform lengthy operations as this could block the command queue.
 *
 * @param <T> The concrete type of {@link Command}
 */
public interface CommandHandler<T extends Command> {
    void handle(T command);

    Class<T> getType();
}
