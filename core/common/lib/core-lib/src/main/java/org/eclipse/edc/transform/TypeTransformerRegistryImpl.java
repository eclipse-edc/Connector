/*
 *  Copyright (c) 2022 - 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.transform;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static java.lang.String.format;

public class TypeTransformerRegistryImpl implements TypeTransformerRegistry {

    private final Map<Class<?>, Map<Class<?>, TypeTransformer<?, ?>>> transformers = new HashMap<>();
    private final Map<String, TypeTransformerRegistry> contextRegistries = new HashMap<>();
    private final Monitor monitor;
    private TypeTransformerRegistry parent;

    public TypeTransformerRegistryImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    private TypeTransformerRegistryImpl(TypeTransformerRegistryImpl parent) {
        this.parent = parent;
        this.monitor = parent.monitor;
    }

    @Override
    public void register(TypeTransformer<?, ?> transformer) {
        var overriddenTransformer = transformers.computeIfAbsent(transformer.getInputType(), key -> new HashMap<>())
                .put(transformer.getOutputType(), transformer);
        if (overriddenTransformer != null) {
            monitor.warning(format("Overriding transformer registered for %s -> %s", transformer.getInputType(), transformer.getOutputType()));
        }
    }

    @Override
    public @NotNull TypeTransformerRegistry forContext(String context) {
        return contextRegistries.computeIfAbsent(context, k -> new TypeTransformerRegistryImpl(this));
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <INPUT, OUTPUT> TypeTransformer<INPUT, OUTPUT> transformerFor(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        return transformers.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(input))
                .filter(entry -> entry.getValue().containsKey(outputType))
                .<Class<?>>map(Map.Entry::getKey)
                .reduce(selectMostSpecificType(input.getClass(), outputType))
                .map(transformers::get)
                .map(it -> (TypeTransformer<INPUT, OUTPUT>) it.get(outputType))
                .or(() -> Optional.ofNullable(parent).map(p -> p.transformerFor(input, outputType)))
                .orElseThrow(() -> new EdcException(format("No Transformer registered that can handle %s -> %s", input.getClass(), outputType)));
    }

    private BinaryOperator<Class<?>> selectMostSpecificType(Class<?> inputType, Class<?> outputType) {
        return (current, candidate) -> {
            if (candidate.isAssignableFrom(current)) {
                return current;
            }

            if (current.isAssignableFrom(candidate)) {
                return candidate;
            }

            throw new EdcException(format("Ambiguous transformers registered for %s -> %s", inputType, outputType));
        };
    }

    @Override
    public <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        Objects.requireNonNull(input);

        var context = new TransformerContextImpl(this);

        var result = context.transform(input, outputType);
        if (context.hasProblems()) {
            return Result.failure(context.getProblems());
        } else {
            return Result.success(result);
        }
    }

}
