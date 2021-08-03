/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.policy.model;

/**
 * An expression that can be evaluated.
 */
public abstract class Expression {

    public interface Visitor<R> {

        R visitLiteralExpression(LiteralExpression expression);

    }

    public abstract <R> R accept(Expression.Visitor<R> visitor);

}
