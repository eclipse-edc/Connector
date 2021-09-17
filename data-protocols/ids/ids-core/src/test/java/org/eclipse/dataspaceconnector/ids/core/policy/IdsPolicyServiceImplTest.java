/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.policy;

import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyActions;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyActions.COMPENSATE_ACTION;
import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyActions.USE_ACTION;
import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyExpressions.ABS_SPATIAL_POSITION;
import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyExpressions.ABS_SPATIAL_POSITION_EXPRESSION;
import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyExpressions.PAY_AMOUNT;
import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyExpressions.PAY_AMOUNT_EXPRESSION;
import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdsPolicyServiceImplTest {

    private IdsPolicyServiceImpl policyService;

    @Test
    void verifyUseButPay() {

        // function that verifies payment
        policyService.registerRequestDutyFunction(PAY_AMOUNT, (operator, value, duty, context) -> {
            if (duty.getParentPermission() != null && !IdsPolicyActions.USE.equals(duty.getParentPermission().getAction().getType())) {
                return true;
            }
            return context.getConsumerConnectorId().equals("connectorThatPaid");
        });

        var amountToPay = new LiteralExpression("25.00");
        var payConstraint = AtomicConstraint.Builder.newInstance().leftExpression(PAY_AMOUNT_EXPRESSION).rightExpression(amountToPay).build();

        var payDuty = Duty.Builder.newInstance().action(COMPENSATE_ACTION).constraint(payConstraint).build();
        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).duty(payDuty).build();

        var payPolicy = Policy.Builder.newInstance().permission(usePermission).build();

        ClaimToken claim = ClaimToken.Builder.newInstance().build();

        // verify pay/unpaid use
        var result = policyService.evaluateRequest("connectorThatPaid", "1", claim, payPolicy);
        assertTrue(result.valid());

        result = policyService.evaluateRequest("connectorThatDidNotPay", "1", claim, payPolicy);
        assertFalse(result.valid());

        // verify that an artifact which does not require payment is accessible

        var freePolicy = Policy.Builder.newInstance().permission(Permission.Builder.newInstance().action(USE_ACTION).build()).build();

        result = policyService.evaluateRequest("connectorThatDidNotPay", "1", claim, freePolicy);
        assertTrue(result.valid());
    }

    @Test
    void verifySpatialLocation() {

        // function that verifies the EU region
        policyService.registerRequestPermissionFunction(ABS_SPATIAL_POSITION, (operator, value, permission, context) -> context.getClaimToken().getClaims().get("region").equals(value));

        var euConstraint = new LiteralExpression("eu");
        var spatialConstraint = AtomicConstraint.Builder.newInstance().leftExpression(ABS_SPATIAL_POSITION_EXPRESSION).operator(IN).rightExpression(euConstraint).build();

        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).constraint(spatialConstraint).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();

        ClaimToken euClaim = ClaimToken.Builder.newInstance().claim("region", "eu").build();
        var result = policyService.evaluateRequest("euConnector", "1", euClaim, policy);
        assertTrue(result.valid());

        ClaimToken outsideClaim = ClaimToken.Builder.newInstance().claim("region", "unknown").build();
        result = policyService.evaluateRequest("unknownConnector", "1", outsideClaim, policy);
        assertFalse(result.valid());

    }


    @BeforeEach
    void setUp() {
        policyService = new IdsPolicyServiceImpl();

    }
}
