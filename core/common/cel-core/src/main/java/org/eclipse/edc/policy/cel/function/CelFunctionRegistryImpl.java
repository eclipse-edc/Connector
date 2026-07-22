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

import org.eclipse.edc.spi.EdcException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CelFunctionRegistryImpl implements CelFunctionRegistry {

    private final List<CelFunction> functions = new CopyOnWriteArrayList<>();
    private final AtomicBoolean sealed = new AtomicBoolean();

    @Override
    public void registerFunction(CelFunction function) {
        if (sealed.get()) {
            throw new EdcException("Cannot register CEL function '%s': the CEL environment has already been built. "
                    .formatted(function.name()) + "Functions must be registered during extension initialization.");
        }
        functions.add(function);
    }

    @Override
    public List<CelFunction> functions() {
        return List.copyOf(functions);
    }

    @Override
    public List<CelFunction> seal() {
        sealed.set(true);
        return List.copyOf(functions);
    }
}
