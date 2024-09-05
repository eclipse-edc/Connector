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

import org.eclipse.edc.policy.model.Permission;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * An evaluation step for {@link Permission} rule;
 */
public class PermissionStep extends RuleStep<Permission> {

    public static final String EDC_PERMISSION_STEP_TYPE = EDC_NAMESPACE + "PermissionStep";
    public static final String EDC_PERMISSION_STEP_DUTY_STEPS = EDC_NAMESPACE + "dutySteps";

    private final List<DutyStep> dutySteps = new ArrayList<>();

    public List<DutyStep> getDutySteps() {
        return dutySteps;
    }

    public static class Builder extends RuleStep.Builder<Permission, PermissionStep, Builder> {

        private Builder() {
            ruleStep = new PermissionStep();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dutyStep(DutyStep dutyStep) {
            ruleStep.dutySteps.add(dutyStep);
            return this;
        }
    }


}
