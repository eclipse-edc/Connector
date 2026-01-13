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

package org.eclipse.edc.connector.controlplane.services.query;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.List;
import java.util.Map;

/**
 * Factory methods to instantiate {@link QueryValidators}
 */
public final class QueryValidators {

    /**
     * Validator for {@link ContractAgreement}
     *
     * @return the validator.
     */
    public static QueryValidator contractAgreement() {
        return new QueryValidator(ContractAgreement.class);
    }

    /**
     * Validator for {@link ContractDefinition}
     *
     * @return the validator.
     */
    public static QueryValidator contractDefinition() {
        return new QueryValidator(ContractDefinition.class);
    }

    /**
     * Validator for {@link ContractNegotiation}
     *
     * @return the validator.
     */
    public static QueryValidator contractNegotiation() {
        return new QueryValidator(ContractNegotiation.class);
    }

    /**
     * Validator for {@link PolicyDefinition}
     *
     * @return the validator.
     */
    public static QueryValidator policyDefinition() {
        return new QueryValidator(PolicyDefinition.class, policySubtypeMap());
    }

    /**
     * Validator for {@link TransferProcess}
     *
     * @return the validator.
     */
    public static QueryValidator transferProcess() {
        return new QueryValidator(TransferProcess.class);
    }

    private static Map<Class<?>, List<Class<?>>> policySubtypeMap() {
        return Map.of(
                Constraint.class, List.of(MultiplicityConstraint.class, AtomicConstraint.class),
                MultiplicityConstraint.class, List.of(AndConstraint.class, OrConstraint.class, XoneConstraint.class),
                Expression.class, List.of(LiteralExpression.class)
        );
    }
}
