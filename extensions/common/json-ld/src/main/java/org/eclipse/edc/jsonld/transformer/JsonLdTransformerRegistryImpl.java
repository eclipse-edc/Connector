/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.jsonld.transformer;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TransformerContextImpl;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


public class JsonLdTransformerRegistryImpl implements JsonLdTransformerRegistry {

    private final Set<JsonLdTransformer<?, ?>> transformers;

    public JsonLdTransformerRegistryImpl() {
        transformers = new HashSet<>();
    }

    public Set<JsonLdTransformer<?, ?>> getTransformers() {
        return Collections.unmodifiableSet(transformers);
    }

    @Override
    public void register(JsonLdTransformer<?, ?> transformer) {
        transformers.add(transformer);
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
    public <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT object, @NotNull Class<OUTPUT> outputType) {

        var ctx = new TransformerContextImpl(this);

        var output = transform(object, outputType, ctx);

        return ctx.hasProblems() ? Result.failure(ctx.getProblems()) : Result.success(output);
    }

    private <INPUT, OUTPUT> OUTPUT transform(INPUT object, Class<OUTPUT> outputType, TransformerContext context) {
        requireNonNull(object);

        var t = transformers.stream()
                .filter(tr -> tr.getInputType().isInstance(object) && tr.getOutputType().equals(outputType))
                .findFirst().orElse(null);

        if (t == null) {
            throw new EdcException(format("No transformer registered that can handle %s -> %s", object, outputType));
        }
        //noinspection unchecked
        return outputType.cast(((JsonLdTransformer<INPUT, OUTPUT>) t).transform(object, context));
    }

}
