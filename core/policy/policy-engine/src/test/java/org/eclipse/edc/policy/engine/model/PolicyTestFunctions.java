/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.policy.engine.model;

import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;

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
