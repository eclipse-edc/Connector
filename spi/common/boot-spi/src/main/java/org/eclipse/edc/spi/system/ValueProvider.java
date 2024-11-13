/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.system;

import org.jetbrains.annotations.Nullable;

/**
 * Functional interface to provide default values for injection points.
 */
@FunctionalInterface
public interface ValueProvider {
    /**
     * Attempts to resolve the default value for a particular {@code InjectionPoint} from the context.
     *
     * @param context The DI container from which to resolve the default value
     * @return the default value, or null if no default value was found
     */
    @Nullable Object get(ServiceExtensionContext context);
}
