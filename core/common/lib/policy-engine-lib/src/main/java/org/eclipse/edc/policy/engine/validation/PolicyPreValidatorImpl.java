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

package org.eclipse.edc.policy.engine.validation;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.plan.step.ValidatorStep;
import org.eclipse.edc.policy.model.Policy;

import java.util.function.BiFunction;

public class PolicyPreValidatorImpl implements PolicyPreValidator {

    private final ValidatorStep validator;

    public PolicyPreValidatorImpl(BiFunction<Policy, PolicyContext, Boolean> validator) {
        this.validator = new ValidatorStep(validator);
    }

    @Override
    public Boolean apply(Policy policy, PolicyContext context) {
        return this.validator.validator().apply(policy, context);
    }

    public ValidatorStep getValidator() {
        return validator;
    }
}
