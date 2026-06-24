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

package org.eclipse.edc.spi.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * This class can be used to form select expressions e.g. in SQL statements. It is a way to express those statements in
 * a generic way. For example:
 * <pre>
 * "operandLeft" = "name",
 * "operator" = "=",
 * "operandRight" = "someone"
 * </pre>
 * <p>
 * can be translated to {@code [select * where name = someone]}
 */
public class Criterion {

    public static final String CRITERION_OPERAND_LEFT = EDC_NAMESPACE + "operandLeft";
    public static final String CRITERION_OPERAND_RIGHT = EDC_NAMESPACE + "operandRight";
    public static final String CRITERION_OPERATOR = EDC_NAMESPACE + "operator";
    public static final String CRITERION_TYPE = EDC_NAMESPACE + "Criterion";
    private Object operandLeft;
    private String operator;
    private Object operandRight;

    private Criterion() {
        //for json serialization
    }

    public static Criterion criterion(Object operandLeft, String operator, Object operandRight) {
        return new Criterion(operandLeft, operator, operandRight);
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

    public Criterion withLeftOperand(String operandLeft) {
        return new Criterion(operandLeft, operator, getOperandRight());
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
        var criterion = (Criterion) o;
        return Objects.equals(operandLeft, criterion.operandLeft) && Objects.equals(operator, criterion.operator) && Objects.equals(operandRight, criterion.operandRight);
    }

    @Override
    public String toString() {
        return format("%s %s %s", getOperandLeft(), getOperator(), getOperandRight());
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final Criterion criterion;

        private Builder() {
            this.criterion = new Criterion();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder operandLeft(Object operandLeft) {
            criterion.operandLeft = operandLeft;
            return this;
        }

        public Builder operator(String operator) {
            criterion.operator = operator;
            return this;
        }

        public Builder operandRight(Object operandRight) {
            criterion.operandRight = operandRight;
            return this;
        }

        public Criterion build() {
            Objects.requireNonNull(this.criterion.operandLeft);
            Objects.requireNonNull(this.criterion.operator);
            return criterion;
        }
    }
}
