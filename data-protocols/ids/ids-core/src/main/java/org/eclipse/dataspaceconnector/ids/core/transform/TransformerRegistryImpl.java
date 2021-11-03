/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.ids.core.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements a {@link TransformerRegistry} that recursively dispatches to transformers for type conversion.
 */
public class TransformerRegistryImpl implements TransformerRegistry {
    private final Map<TransformKey, IdsTypeTransformer<?, ?>> transformers = new HashMap<>();

    @Override
    public void register(IdsTypeTransformer<?, ?> transformer) {
        Objects.requireNonNull(transformer);
        transformers.put(new TransformKey(transformer.getInputType(), transformer.getOutputType()), transformer);
    }

    @Override
    public <INPUT, OUTPUT> TransformResult<OUTPUT> transform(INPUT object, Class<OUTPUT> outputType) {
        var context = new TransformerContextImpl(this);
        var output = transform(object, outputType, context);
        return context.hasProblems() ? new TransformResult<>(context.problems) : new TransformResult<>(output);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <INPUT, OUTPUT> @Nullable OUTPUT transform(INPUT object, Class<OUTPUT> outputType, TransformerContext context) {
        Objects.requireNonNull(object);
        var key = new TransformKey(object.getClass(), outputType);
        var idsTypeTransformer = (IdsTypeTransformer) transformers.get(key);
        if (idsTypeTransformer == null) {
            throw new EdcException("Transformer not found for pair:" + key); // this is a programming error
        }
        return outputType.cast(idsTypeTransformer.transform(object, context));
    }

    private static class TransformKey {
        private final Class<?> input;
        private final Class<?> output;

        public TransformKey(Class<?> input, Class<?> output) {
            Objects.requireNonNull(input);
            Objects.requireNonNull(output);
            this.input = input;
            this.output = output;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransformKey transformKey = (TransformKey) o;
            return input.equals(transformKey.input) && output.equals(transformKey.output);
        }

        @Override
        public int hashCode() {
            return Objects.hash(input, output);
        }

        @Override
        public String toString() {
            return "TKey{" +
                    "input=" + input +
                    ", output=" + output +
                    '}';
        }
    }

    private static class TransformerContextImpl implements TransformerContext {
        private final List<String> problems = new ArrayList<>();
        private final TransformerRegistryImpl registry;

        public TransformerContextImpl(TransformerRegistryImpl registry) {
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
            return registry.transform(object, outputType, this);
        }
    }
}
