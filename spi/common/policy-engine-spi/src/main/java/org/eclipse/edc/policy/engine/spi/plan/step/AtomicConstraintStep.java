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

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Rule;

/**
 * An evaluation step for {@link AtomicConstraint}.
 * <p>
 * The {@link AtomicConstraintStep} should be considered filtered when the left expression is not bound to a
 * scope or an evaluation function {@link AtomicConstraintFunction}
 */
public record AtomicConstraintStep(AtomicConstraint constraint, boolean isFiltered, Rule rule,
                                   AtomicConstraintFunction<? extends Rule> function) implements ConstraintStep {

}
