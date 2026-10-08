/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

class IntegerStringTypeTransformer implements TypeTransformer<Integer, String> {

    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public Class<String> getOutputType() {
        return String.class;
    }

    @Override
    public @Nullable String transform(@NotNull Integer object, @NotNull TransformerContext context) {
        try {
            return String.valueOf(object);
        } catch (Exception e) {
            context.reportProblem(format("Integer %s cannot be transformed to String: %s", object, e.getMessage()));
            return null;
        }

    }
}
