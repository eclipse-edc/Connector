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

package org.eclipse.edc.transform.spi;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

public class TypeTransformerRegistryImpl<T extends TypeTransformer<?, ?>> implements TypeTransformerRegistry<T> {

    private final List<T> transformers = new ArrayList<>();

    @Override
    public void register(T transformer) {
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

        var transformer = transformerFor(input, outputType);

        var result = transformer.transform(input, context);
        if (context.hasProblems()) {
            return Result.failure(context.getProblems());
        } else {
            return Result.success(result);
        }
    }
}
