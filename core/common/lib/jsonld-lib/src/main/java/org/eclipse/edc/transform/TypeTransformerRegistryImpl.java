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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class TypeTransformerRegistryImpl implements TypeTransformerRegistry {
    private final Map<String, Class<?>> aliases = new HashMap<>();
    private final Map<Class<?>, Map<Class<?>, TypeTransformer<?, ?>>> transformers = new HashMap<>();
    private final Map<String, TypeTransformerRegistry> contextRegistries = new HashMap<>();
    private TypeTransformerRegistry parent;

    public TypeTransformerRegistryImpl() {
    }

    private TypeTransformerRegistryImpl(TypeTransformerRegistry parent) {
        this.parent = parent;
    }

    @Override
    public void register(TypeTransformer<?, ?> transformer) {
        transformers.computeIfAbsent(transformer.getInputType(), key -> new HashMap<>())
                    .put(transformer.getOutputType(), transformer);
    }

    @Override
    public @NotNull TypeTransformerRegistry forContext(String context) {
        return contextRegistries.computeIfAbsent(context, k -> new TypeTransformerRegistryImpl(this));
    }

    @Override
    public @NotNull <INPUT, OUTPUT> TypeTransformer<INPUT, OUTPUT> transformerFor(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        return findTransformer(input, outputType)
                .map(it -> (TypeTransformer<INPUT, OUTPUT>) it)
                .or(() -> Optional.ofNullable(parent).map(p -> p.transformerFor(input, outputType)))
                .orElseThrow(() -> new EdcException(format("No Transformer registered that can handle %s -> %s", input.getClass(), outputType)));
    }

    private Optional<TypeTransformer<?, ?>> findTransformer(Object input, Class<?> outputType) {
        var inputTypes = transformers.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(input))
                .filter(entry -> entry.getValue().containsKey(outputType))
                .map(Map.Entry::getKey)
                .toList();

        var mostSpecificInputTypes = inputTypes.stream()
                .filter(candidate -> inputTypes.stream()
                        .noneMatch(other -> !candidate.equals(other) && candidate.isAssignableFrom(other)))
                .toList();

        if (mostSpecificInputTypes.size() > 1) {
            throw new EdcException(format("Ambiguous transformers registered for %s -> %s", input.getClass(), outputType));
        }

        return mostSpecificInputTypes.stream()
                .findFirst()
            .map(inputType -> transformers.get(inputType).get(outputType));
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
