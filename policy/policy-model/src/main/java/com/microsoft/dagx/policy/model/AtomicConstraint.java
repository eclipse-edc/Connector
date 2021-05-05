/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

/**
 * A leaf constraint containing a left expression, right expression, and operator triple that can be evaluated.
 */
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

    public static class Builder {
        private AtomicConstraint constraint;

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

        private Builder() {
            constraint = new AtomicConstraint();
        }
    }

}
