package com.microsoft.dagx.policy.model;

/**
 * An expression that can be evaluated.
 */
public abstract class Expression {

    public interface Visitor<R> {

        R visitLiteralExpression(LiteralExpression expression);

    }

    public abstract <R> R accept(Expression.Visitor<R> visitor);

}
