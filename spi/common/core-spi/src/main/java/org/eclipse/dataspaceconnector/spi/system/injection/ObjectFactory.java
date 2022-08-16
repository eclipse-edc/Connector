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

package org.eclipse.dataspaceconnector.spi.system.injection;

import org.jetbrains.annotations.NotNull;

/**
 * Factory object that is used to generate instances of objects based on their class.
 * One way to implement this is to generate a new instance on demand using reflection and dependency injection.
 */
@FunctionalInterface
public interface ObjectFactory {
    /**
     * Creates a new instance of a commandHandler
     *
     * @param clazz The object's class
     * @throws RuntimeException if a new instance could not be created
     */
    @NotNull <T> T constructInstance(Class<T> clazz);
}
