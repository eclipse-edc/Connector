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

import org.eclipse.edc.policy.model.Duty;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * An evaluation step for {@link Duty} rule;
 */
public class DutyStep extends RuleStep<Duty> {

    public static final String EDC_DUTY_STEP_TYPE = EDC_NAMESPACE + "DutyStep";
    
    public static class Builder extends RuleStep.Builder<Duty, DutyStep, Builder> {

        private Builder() {
            ruleStep = new DutyStep();
        }

        public static Builder newInstance() {
            return new Builder();
        }

    }
}
