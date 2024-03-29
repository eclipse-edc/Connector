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

import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;

class StringIntegerTypeTransformer implements TypeTransformer<String, Integer> {

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<Integer> getOutputType() {
        return Integer.class;
    }

    @Override
    public @Nullable Integer transform(@NotNull String object, @NotNull TransformerContext context) {
        try {
            return Integer.valueOf(object);
        } catch (Exception e) {
            context.reportProblem(format("String %s cannot be transformed to integer: %s", object, e.getMessage()));
            return null;
        }

    }
}
