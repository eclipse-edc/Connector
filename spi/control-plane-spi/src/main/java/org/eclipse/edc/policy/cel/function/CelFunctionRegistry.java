/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.List;

/**
 * Registry for custom functions that are made available to CEL expressions.
 * <p>
 * Functions must be registered during extension initialization: the CEL environment is built lazily from a single
 * snapshot of this registry when the first expression is compiled, and cannot be extended afterwards.
 */
@ExtensionPoint
public interface CelFunctionRegistry {

    /**
     * Registers a function overload.
     *
     * @param function the function to register
     * @throws org.eclipse.edc.spi.EdcException if the registry has already been sealed
     */
    void registerFunction(CelFunction function);

    /**
     * Returns all registered functions.
     *
     * @return the registered functions
     */
    List<CelFunction> functions();

    /**
     * Returns the registered functions and closes the registry for further registrations, so that a function
     * registered too late fails loudly instead of being silently absent from expressions.
     *
     * @return the registered functions
     */
    List<CelFunction> seal();
}
