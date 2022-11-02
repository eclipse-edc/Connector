/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TransformerContextImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

/**
 * Default in-mem implementation of the {@link DtoTransformerRegistry} interface.
 * Every REST API extension should check whether an {@link DtoTransformerRegistry} is already available in the {@link ServiceExtensionContext}
 * and register one otherwise:
 * <pre>
 * \@Inject(required=false)
 * private DtoTransformerRegistry registry;
 *
 * void initialize(ServiceExtensionContext context){
 *      if(registry == null){
 *          registry= new DtoTransformerRegistryImpl();
 *          context.registerService(DtoTransformerRegistry.class, registry);
 *      }
 *
 *      var yourController = new YourApiController(..., registry);
 * }
 * </pre>
 */
public class DtoTransformerRegistryImpl implements DtoTransformerRegistry {

    private final Set<DtoTransformer<?, ?>> transformers;

    public DtoTransformerRegistryImpl() {
        transformers = new HashSet<>();
    }

    public Set<DtoTransformer<?, ?>> getTransformers() {
        return Collections.unmodifiableSet(transformers);
    }

    @Override
    public void register(DtoTransformer<?, ?> transformer) {
        transformers.add(transformer);
    }

    @Override
    public <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT object, @NotNull Class<OUTPUT> outputType) {

        var ctx = new TransformerContextImpl(this);

        var output = transform(object, outputType, ctx);

        return ctx.hasProblems() ? Result.failure(ctx.getProblems()) : Result.success(output);
    }

    private <INPUT, OUTPUT> OUTPUT transform(INPUT object, Class<OUTPUT> outputType, TransformerContext context) {
        Objects.requireNonNull(object);

        var t = transformers.stream()
                .filter(tr -> tr.getInputType().isInstance(object) && tr.getOutputType().equals(outputType))
                .findFirst().orElse(null);

        if (t == null) {
            throw new EdcException(format("No DtoTransformer registered that can handle %s -> %s", object, outputType));
        }
        return outputType.cast(((DtoTransformer<INPUT, OUTPUT>) t).transform(object, context));
    }

}
