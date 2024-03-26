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

package org.eclipse.edc.policy.evaluator;

import org.eclipse.edc.policy.model.Constraint;

/**
 * A problem encountered during evaluation or processing of a constraint such as an unsatisfied constraint.
 */
public class ConstraintProblem {
    private final String description;
    private final Constraint constraint;

    public ConstraintProblem(String description, Constraint constraint) {
        this.description = description;
        this.constraint = constraint;
    }

    /**
     * Returns a problem description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the constraint associated with the problem.
     */
    public Constraint getConstraint() {
        return constraint;
    }
}
