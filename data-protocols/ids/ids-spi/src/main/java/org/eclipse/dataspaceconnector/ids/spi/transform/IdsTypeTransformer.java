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
package org.eclipse.dataspaceconnector.ids.spi.transform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementations transform between an IDS and EDC type.
 */
public interface IdsTypeTransformer<INPUT, OUTPUT> {

    /**
     * Returns the input object type.
     */
    Class<INPUT> getInputType();

    /**
     * Returns the type the object will be transformed to.
     */
    Class<OUTPUT> getOutputType();

    /**
     * Transforms the object.
     */
    @Nullable
    OUTPUT transform(@Nullable INPUT object, @NotNull TransformerContext context);
}
