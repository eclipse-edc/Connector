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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.policy.model.Prohibition;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * An evaluation step for {@link Prohibition} rule;
 */
public class ProhibitionStep extends RuleStep<Prohibition> {

    public static final String EDC_PROHIBITION_STEP_TYPE = EDC_NAMESPACE + "ProhibitionStep";
    
    public static class Builder extends RuleStep.Builder<Prohibition, ProhibitionStep, Builder> {

        private Builder() {
            ruleStep = new ProhibitionStep();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

    }
}
