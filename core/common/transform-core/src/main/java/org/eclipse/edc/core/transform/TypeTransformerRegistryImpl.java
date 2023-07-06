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

package org.eclipse.edc.core.transform;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

public class TypeTransformerRegistryImpl implements TypeTransformerRegistry {
    private final Map<String, Class<?>> aliases = new HashMap<>();
    private final List<TypeTransformer<?, ?>> transformers = new ArrayList<>();

    @Override
    public void register(TypeTransformer<?, ?> transformer) {
        this.transformers.add(transformer);
    }

    @Override
    public @NotNull <INPUT, OUTPUT> TypeTransformer<INPUT, OUTPUT> transformerFor(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        return transformers.stream()
                .filter(t -> t.getInputType().isInstance(input) && t.getOutputType().equals(outputType))
                .findAny()
                .map(it -> (TypeTransformer<INPUT, OUTPUT>) it)
                .orElseThrow(() -> new EdcException(format("No Transformer registered that can handle %s -> %s", input.getClass(), outputType)));
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


    @Override
    public Class<?> typeAlias(String type) {
        return aliases.get(type);
    }

    @Override
    public Class<?> typeAlias(String type, Class<?> defaultType) {
        return aliases.getOrDefault(type, defaultType);
    }

    @Override
    public void registerTypeAlias(String alias, Class<?> type) {
        aliases.put(alias, type);
    }
}
