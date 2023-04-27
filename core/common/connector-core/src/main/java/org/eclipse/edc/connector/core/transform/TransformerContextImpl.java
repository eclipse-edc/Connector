/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.core.transform;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TransformerContextImpl implements TransformerContext {
    private final List<String> problems = new ArrayList<>();
    private final TypeTransformerRegistry registry;

    public TransformerContextImpl(TypeTransformerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    @Override
    public List<String> getProblems() {
        return problems;
    }

    @Override
    public void reportProblem(String problem) {
        problems.add(problem);
    }

    @Override
    public <INPUT, OUTPUT> @Nullable OUTPUT transform(INPUT object, Class<OUTPUT> outputType) {
        if (object == null) {
            return null;
        }

        return registry.transformerFor(object, outputType)
                .transform(object, this);
    }

    @Override
    public Class<?> typeAlias(String type) {
        return registry.typeAlias(type);
    }

    @Override
    public Class<?> typeAlias(String type, Class<?> defaultType) {
        return registry.typeAlias(type, defaultType);
    }

}
