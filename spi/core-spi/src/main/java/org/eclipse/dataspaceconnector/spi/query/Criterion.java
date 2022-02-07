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

package org.eclipse.dataspaceconnector.spi.query;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static java.lang.String.format;

/**
 * This class can be used to form select expressions e.g. in SQL statements. It is a way to express
 * those statements in a generic way.
 * For example:
 * <pre>
 * "operandLeft" = "name",
 * "operator" = "=",
 * "operandRight" = "someone"
 * </pre>
 * <p>
 * can be translated to {@code [select * where name = someone]}
 */
public class Criterion {
    @JsonProperty("left")
    private Object operandLeft;
    @JsonProperty("op")
    private String operator;
    @JsonProperty("right")
    private Object operandRight;

    private Criterion() {
        //for json serialization
    }

    public Criterion(Object left, String op, Object right) {
        operandLeft = Objects.requireNonNull(left);
        operator = Objects.requireNonNull(op);
        operandRight = right; // null may be allowed, for example when the operator is unary, like NOT_NULL
    }

    public Object getOperandLeft() {
        return operandLeft;
    }

    public String getOperator() {
        return operator;
    }

    public Object getOperandRight() {
        return operandRight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operandLeft, operator, operandRight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Criterion criterion = (Criterion) o;
        return Objects.equals(operandLeft, criterion.operandLeft) && Objects.equals(operator, criterion.operator) && Objects.equals(operandRight, criterion.operandRight);
    }

    @Override
    public String toString() {
        return format("%s %s %s", getOperandLeft(), getOperator(), getOperandRight());
    }
}
