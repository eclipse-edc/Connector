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

package org.eclipse.dataspaceconnector.policy.engine.model;

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;

/**
 * Functions used for testing.
 */
public class PolicyTestFunctions {

    private PolicyTestFunctions() {
    }

    public static AtomicConstraint createLiteralAtomicConstraint(String value1, String value2) {
        LiteralExpression left = new LiteralExpression(value1);
        LiteralExpression right = new LiteralExpression(value2);
        return AtomicConstraint.Builder.newInstance().leftExpression(left).operator(Operator.EQ).rightExpression(right).build();
    }
}
