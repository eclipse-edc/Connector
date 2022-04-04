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

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements a {@link IdsTransformerRegistry} that recursively dispatches to transformers for type conversion.
 */
public class IdsTransformerRegistryImpl implements IdsTransformerRegistry {
    private final Map<TransformKey, IdsTypeTransformer<?, ?>> transformers = new HashMap<>();

    @Override
    public void register(IdsTypeTransformer<?, ?> transformer) {
        Objects.requireNonNull(transformer);
        transformers.put(new TransformKey(transformer.getInputType(), transformer.getOutputType()), transformer);
    }

    @Override
    public <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT object, @NotNull Class<OUTPUT> outputType) {
        var context = new TransformerContextImpl(this);
        var output = transform(object, outputType, context);
        return context.hasProblems() ? Result.failure(context.problems) : Result.success(output);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <INPUT, OUTPUT> @Nullable OUTPUT transform(INPUT object, Class<OUTPUT> outputType, TransformerContext context) {
        Objects.requireNonNull(object);

        IdsTypeTransformer idsTypeTransformer = findEligibleTransformer(object, outputType);
        if (idsTypeTransformer == null) {
            throw new EdcException("Transformer not found for pair:" + new TransformKey(object.getClass(), outputType)); // this is a programming error
        }
        return outputType.cast(idsTypeTransformer.transform(object, context));
    }

    @SuppressWarnings({ "unchecked" })
    private <INPUT, OUTPUT> IdsTypeTransformer<INPUT, OUTPUT> findEligibleTransformer(INPUT object, Class<OUTPUT> outputType) {
        IdsTypeTransformer<INPUT, OUTPUT> idsTypeTransformer;

        Class<?> inputClass = object.getClass();
        do {
            idsTypeTransformer = (IdsTypeTransformer<INPUT, OUTPUT>) transformers.get(new TransformKey(inputClass, outputType));
            if (idsTypeTransformer == null) {
                for (Class<?> anInterface : inputClass.getInterfaces()) {
                    idsTypeTransformer = (IdsTypeTransformer<INPUT, OUTPUT>) transformers.get(new TransformKey(anInterface, outputType));
                    if (idsTypeTransformer != null) {
                        break;
                    }
                }
            }

            inputClass = inputClass.getSuperclass();
        } while (inputClass != null && idsTypeTransformer == null);

        return idsTypeTransformer;
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
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
        private final IdsTransformerRegistryImpl registry;

        public TransformerContextImpl(IdsTransformerRegistryImpl registry) {
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
