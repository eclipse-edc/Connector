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

import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.List;

/**
 * An evaluation step for {@link XoneConstraint}
 */
public final class XoneConstraintStep extends MultiplicityConstraintStep<XoneConstraint> implements ConstraintStep {

    public XoneConstraintStep(List<ConstraintStep> steps, XoneConstraint constraint) {
        super(steps, constraint);
    }
}
