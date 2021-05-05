/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.engine.model;

import com.microsoft.dagx.policy.model.AtomicConstraint;
import com.microsoft.dagx.policy.model.LiteralExpression;
import com.microsoft.dagx.policy.model.Operator;

/**
 * Functions used for testing.
 */
public class PolicyTestFunctions {

    public static AtomicConstraint createLiteralAtomicConstraint(String value1, String value2) {
        LiteralExpression left = new LiteralExpression(value1);
        LiteralExpression right = new LiteralExpression(value2);
        return AtomicConstraint.Builder.newInstance().leftExpression(left).operator(Operator.EQ).rightExpression(right).build();
    }

    private PolicyTestFunctions() {
    }
}
