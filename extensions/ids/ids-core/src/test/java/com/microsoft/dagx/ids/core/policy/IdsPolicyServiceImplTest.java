/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.policy;

import com.microsoft.dagx.ids.spi.policy.IdsPolicyActions;
import com.microsoft.dagx.policy.model.AtomicConstraint;
import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.LiteralExpression;
import com.microsoft.dagx.policy.model.Permission;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.iam.ClaimToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.dagx.ids.spi.policy.IdsPolicyActions.COMPENSATE_ACTION;
import static com.microsoft.dagx.ids.spi.policy.IdsPolicyActions.USE_ACTION;
import static com.microsoft.dagx.ids.spi.policy.IdsPolicyExpressions.ABS_SPATIAL_POSITION;
import static com.microsoft.dagx.ids.spi.policy.IdsPolicyExpressions.ABS_SPATIAL_POSITION_EXPRESSION;
import static com.microsoft.dagx.ids.spi.policy.IdsPolicyExpressions.PAY_AMOUNT;
import static com.microsoft.dagx.ids.spi.policy.IdsPolicyExpressions.PAY_AMOUNT_EXPRESSION;
import static com.microsoft.dagx.policy.model.Operator.IN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class IdsPolicyServiceImplTest {

    private IdsPolicyServiceImpl policyService;

    @Test
    void verifyUseButPay() {

        // function that verifies payment
        policyService.registerRequestDutyFunction(PAY_AMOUNT, (operator, value, duty, context) -> {
            if (duty.getParentPermission() != null && !IdsPolicyActions.USE.equals(duty.getParentPermission().getAction().getType())) {
                return true;
            }
            return context.getClientConnectorId().equals("connectorThatPaid");
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
