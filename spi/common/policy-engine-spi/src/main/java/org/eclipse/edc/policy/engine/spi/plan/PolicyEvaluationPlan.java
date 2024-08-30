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

package org.eclipse.edc.policy.engine.spi.plan;

import org.eclipse.edc.policy.engine.spi.plan.step.DutyStep;
import org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ProhibitionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ValidatorStep;
import org.eclipse.edc.policy.model.Policy;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link PolicyEvaluationPlan} contains information about the evaluation process of a {@link Policy}
 * withing a scope without executing it.
 */
public class PolicyEvaluationPlan {

    private final List<ValidatorStep> preValidators = new ArrayList<>();
    private final List<ValidatorStep> postValidators = new ArrayList<>();
    private final List<PermissionStep> permissionSteps = new ArrayList<>();
    private final List<ProhibitionStep> prohibitionSteps = new ArrayList<>();
    private final List<DutyStep> dutySteps = new ArrayList<>();

    public List<ValidatorStep> getPostValidators() {
        return postValidators;
    }

    public List<ValidatorStep> getPreValidators() {
        return preValidators;
    }

    public List<PermissionStep> getPermissionSteps() {
        return permissionSteps;
    }

    public List<DutyStep> getDutySteps() {
        return dutySteps;
    }

    public List<ProhibitionStep> getProhibitionSteps() {
        return prohibitionSteps;
    }

    public static class Builder {

        private final PolicyEvaluationPlan plan = new PolicyEvaluationPlan();

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder preValidator(ValidatorStep validatorStep) {
            plan.preValidators.add(validatorStep);
            return this;
        }


        public Builder permission(PermissionStep permissionStep) {
            plan.permissionSteps.add(permissionStep);
            return this;
        }

        public Builder prohibition(ProhibitionStep prohibitionStep) {
            plan.prohibitionSteps.add(prohibitionStep);
            return this;
        }

        public Builder obligation(DutyStep dutyStep) {
            plan.dutySteps.add(dutyStep);
            return this;
        }

        public Builder postValidator(ValidatorStep validatorStep) {
            plan.postValidators.add(validatorStep);
            return this;
        }

        public PolicyEvaluationPlan build() {
            return plan;
        }
    }
}
