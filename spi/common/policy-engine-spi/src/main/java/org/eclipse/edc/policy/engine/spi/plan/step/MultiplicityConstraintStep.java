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

/**
 * Base evaluation step for {@link MultiplicityConstraint}. It carries the {@link MultiplicityConstraint}
 * and the collection of child {@link ConstraintStep}.
 */
public abstract class MultiplicityConstraintStep<T extends MultiplicityConstraint> {

    private final List<ConstraintStep> steps;
    private final T constraint;

    public MultiplicityConstraintStep(List<ConstraintStep> steps, T constraint) {
        this.constraint = constraint;
        this.steps = steps;
    }

    public List<ConstraintStep> getSteps() {
        return steps;
    }

    public T getConstraint() {
        return constraint;
    }
}
