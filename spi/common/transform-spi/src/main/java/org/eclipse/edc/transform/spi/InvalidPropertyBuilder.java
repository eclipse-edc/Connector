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

package org.eclipse.edc.transform.spi;

import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;

/**
 * Reports a property that contains an invalid value.
 */
public class InvalidPropertyBuilder extends AbstractProblemBuilder<InvalidPropertyBuilder> {
    private final TransformerContext context;

    private String value = UNKNOWN;
    private String error;

    public InvalidPropertyBuilder(TransformerContext context) {
        this.context = context;
    }

    public InvalidPropertyBuilder value(@Nullable String value) {
        if (value == null) {
            this.value = "null";
            return this;
        }
        this.value = value;
        return this;
    }

    public InvalidPropertyBuilder error(String error) {
        this.error = error;
        return this;
    }

    @Override
    public void report() {
        context.reportProblem(format("%s '%s' was invalid%s%s",
                type == null ? "Property" : type + " property",
                property,
                value != null ? ": " + value : "",
                error != null ? ". Error was: " + error : ""));
    }

}
