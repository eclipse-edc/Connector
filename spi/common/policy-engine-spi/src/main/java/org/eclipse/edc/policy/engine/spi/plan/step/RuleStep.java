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

import org.eclipse.edc.policy.model.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base step class for {@link Rule}s evaluation. A rule can have multiple {@link ConstraintStep}
 * and {@link RuleFunctionStep} associated during the evaluation process.
 * A rule step is considered filtered (not evaluated) when the rule is not bound to a scope.
 */
public abstract class RuleStep<R extends Rule> {

    protected R rule;
    protected boolean isFiltered = false;

    protected List<ConstraintStep> constraintSteps = new ArrayList<>();

    protected List<RuleFunctionStep<R>> ruleFunctions = new ArrayList<>();

    public List<ConstraintStep> getConstraintSteps() {
        return constraintSteps;
    }

    public List<RuleFunctionStep<R>> getRuleFunctions() {
        return ruleFunctions;
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<R extends Rule, T extends RuleStep<R>, B extends Builder<R, T, B>> {
        protected T ruleStep;


        public B rule(R rule) {
            ruleStep.rule = rule;
            return (B) this;
        }

        public B constraint(ConstraintStep constraint) {
            ruleStep.constraintSteps.add(constraint);
            return (B) this;
        }

        public B ruleFunction(RuleFunctionStep<R> function) {
            ruleStep.ruleFunctions.add(function);
            return (B) this;
        }

        public B filtered(boolean isFiltered) {
            ruleStep.isFiltered = isFiltered;
            return (B) this;
        }

        public T build() {
            Objects.requireNonNull(ruleStep.rule, "Rule should not be null");
            return ruleStep;
        }

    }
}