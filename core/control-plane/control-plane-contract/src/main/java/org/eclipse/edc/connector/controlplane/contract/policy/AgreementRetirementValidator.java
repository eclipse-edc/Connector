/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.policy;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;

/**
 * Pre-validator that fails policy evaluation when the {@link org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement}
 * bound to the evaluation context has been retired. Registered for every scope that carries a contract agreement (e.g.
 * transfer process and policy monitor), it prevents new transfers from being started on a retired agreement and causes
 * the policy monitor to terminate the ongoing ones.
 */
public class AgreementRetirementValidator<C extends AgreementPolicyContext> implements PolicyValidatorRule<C> {

    @Override
    public Boolean apply(Policy policy, C context) {
        var agreement = context.contractAgreement();
        if (agreement != null && agreement.isRetired()) {
            context.reportProblem("Contract Agreement with ID=%s has been retired".formatted(agreement.getId()));
            return false;
        }
        return true;
    }
}
