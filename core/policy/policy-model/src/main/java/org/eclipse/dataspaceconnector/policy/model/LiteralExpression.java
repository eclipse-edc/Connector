/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.policy.model;

import org.jetbrains.annotations.NotNull;

/**
 * A literal value used as an expression.
 */
public class LiteralExpression extends Expression {
    private final Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value + "'";
    }

    @NotNull
    public String asString() {
        return value == null ? "null" : value.toString();
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitLiteralExpression(this);
    }


    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LiteralExpression) {
            return ((LiteralExpression) obj).value.equals(value);
        }

        return false;
    }
}
