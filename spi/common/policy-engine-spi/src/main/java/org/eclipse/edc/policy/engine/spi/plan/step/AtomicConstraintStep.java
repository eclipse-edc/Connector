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

import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Rule;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * An evaluation step for {@link AtomicConstraint}.
 * <p>
 * The {@link AtomicConstraintStep} should be considered filtered when the left expression is not bound to a
 * scope or an evaluation function {@link AtomicConstraintRuleFunction}
 */
public record AtomicConstraintStep(AtomicConstraint constraint,
                                   List<String> filteringReasons,
                                   Rule rule,
                                   String functionName) implements ConstraintStep {

    public static final String EDC_ATOMIC_CONSTRAINT_STEP_TYPE = EDC_NAMESPACE + "AtomicConstraintStep";
    public static final String EDC_ATOMIC_CONSTRAINT_STEP_IS_FILTERED = EDC_NAMESPACE + "isFiltered";
    public static final String EDC_ATOMIC_CONSTRAINT_STEP_FILTERING_REASONS = EDC_NAMESPACE + "filteringReasons";
    public static final String EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_NAME = EDC_NAMESPACE + "functionName";
    public static final String EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_PARAMS = EDC_NAMESPACE + "functionParams";

    public boolean isFiltered() {
        return !filteringReasons.isEmpty();
    }

    public List<String> functionParams() {
        return List.of(
                constraint.getLeftExpression().toString(),
                constraint.getOperator().toString(),
                constraint.getRightExpression().toString());
    }
}
