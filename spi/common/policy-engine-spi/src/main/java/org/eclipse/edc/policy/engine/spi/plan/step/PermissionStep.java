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

/**
 * An evaluation step for {@link Permission} rule;
 */
public class PermissionStep extends RuleStep<Permission> {
    
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
