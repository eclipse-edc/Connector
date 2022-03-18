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
 *
 */

package org.eclipse.dataspaceconnector.spi.transformer;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Generic registry to hold {@link TypeTransformer} objects
 */
public interface TypeTransformerRegistry<T extends TypeTransformer<?, ?>> {
    /**
     * Registers a transformer.
     */
    void register(T transformer);

    /**
     * Transforms the object and any contained types, returning its transformed representation or null if the operation cannot be completed.
     *
     * @param <INPUT>    the instance type
     * @param <OUTPUT>   the transformed object type
     * @param object     the instance to transform
     * @param outputType the transformed output type
     * @return the transform result
     */
    <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT object, @NotNull Class<OUTPUT> outputType);
}
