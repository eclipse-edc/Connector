/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.policy;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheck;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

/**
 * {@link ContractExpiryCheck} wrapper to be used with {@link TransferProcessPolicyContext}.
 */
public class ContractExpiryFunction implements AtomicConstraintRuleFunction<Permission, TransferProcessPolicyContext> {

    private final ContractExpiryCheck check = new ContractExpiryCheck();

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, TransferProcessPolicyContext context) {
        return check.evaluate(operator, rightValue, context.now(), context.contractAgreement())
                .onFailure(failure -> failure.getMessages().forEach(context::reportProblem))
                .succeeded();
    }

}
