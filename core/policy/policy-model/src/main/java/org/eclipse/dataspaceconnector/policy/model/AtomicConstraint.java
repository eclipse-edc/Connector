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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A leaf constraint containing a left expression, right expression, and operator triple that can be evaluated.
 */
@JsonDeserialize(builder = AtomicConstraint.Builder.class)
public class AtomicConstraint extends Constraint {
    private Expression leftExpression;
    private Expression rightExpression;
    private Operator operator = Operator.EQ;

    public Expression getLeftExpression() {
        return leftExpression;
    }

    public Expression getRightExpression() {
        return rightExpression;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitAtomicConstraint(this);
    }

    @Override
    public String toString() {
        return "Constraint " + leftExpression + " " + operator.toString() + " " + rightExpression;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final AtomicConstraint constraint;

        private Builder() {
            constraint = new AtomicConstraint();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder leftExpression(Expression expression) {
            constraint.leftExpression = expression;
            return this;
        }

        public Builder rightExpression(Expression expression) {
            constraint.rightExpression = expression;
            return this;
        }

        public Builder operator(Operator operator) {
            constraint.operator = operator;
            return this;
        }

        public AtomicConstraint build() {
            return constraint;
        }
    }

}
