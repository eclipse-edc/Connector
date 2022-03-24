/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.claim.validation;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.spi.policy.PolicyEngine.ALL_SCOPES;

public class TokenClaimFunctionFactory {

    public static final String POLICY_VALIDATION_CONFIG = "edc.policy.validation";
    public static final String CLAIM = "claim";

    private final PolicyEngine policyEngine;
    private final Monitor monitor;

    public TokenClaimFunctionFactory(PolicyEngine policyEvaluator, Monitor monitor) {
        this.policyEngine = policyEvaluator;
        this.monitor = monitor;
    }

    public void register(ServiceExtensionContext context) {

        var config = context.getConfig(POLICY_VALIDATION_CONFIG);
        var partitions = config.partition();
        for (Config partition : partitions.collect(Collectors.toList())) {
            String leftOperandValue = partition.currentNode();
            String claimName = partition.getString(CLAIM);
            if (claimName == null || claimName.isEmpty()) {
                monitor.debug(String.format("Setting for policy token claim validation (%s) has no '%s' value", POLICY_VALIDATION_CONFIG, CLAIM));
                continue;
            }

            policyEngine.registerFunction(ALL_SCOPES, Permission.class, leftOperandValue, new ClaimFunction(claimName));
            monitor.info(String.format("Register claim validation for permission constraint '%s' and token claim '%s'", leftOperandValue, claimName));
        }
    }

    private static class ClaimFunction implements AtomicConstraintFunction<Permission> {

        private final String claim;

        private ClaimFunction(String claim) {
            this.claim = claim;
        }

        @Override
        public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
            switch (operator) {
                case IN:
                    return context.getParticipantAgent().getClaims().containsKey(claim) &&
                            context.getParticipantAgent().getClaims().get(claim).contains(rightValue.toString());
                case EQ:
                    return context.getParticipantAgent().getClaims().containsKey(claim) &&
                            context.getParticipantAgent().getClaims().get(claim).equals(rightValue.toString());
                case NEQ:
                    return context.getParticipantAgent().getClaims().containsKey(claim) &&
                            !context.getParticipantAgent().getClaims().get(claim).equals(rightValue.toString());
                case GT:
                case GEQ:
                case LT:
                case LEQ:
                default:
                    throw new RuntimeException("Not supported");
            }
        }
    }

}
