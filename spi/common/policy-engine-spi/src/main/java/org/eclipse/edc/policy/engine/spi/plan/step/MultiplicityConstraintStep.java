/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine.spi.plan.step;

import org.eclipse.edc.policy.model.MultiplicityConstraint;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Base evaluation step for {@link MultiplicityConstraint}. It carries the {@link MultiplicityConstraint}
 * and the collection of child {@link ConstraintStep}.
 */
public abstract class MultiplicityConstraintStep<T extends MultiplicityConstraint> {
    
    public static final String EDC_MULTIPLICITY_CONSTRAINT_STEPS = EDC_NAMESPACE + "constraintSteps";

    private final List<ConstraintStep> constraintSteps;
    private final T constraint;

    public MultiplicityConstraintStep(List<ConstraintStep> constraintSteps, T constraint) {
        this.constraint = constraint;
        this.constraintSteps = constraintSteps;
    }

    public List<ConstraintStep> getConstraintSteps() {
        return constraintSteps;
    }

    public T getConstraint() {
        return constraint;
    }
}
