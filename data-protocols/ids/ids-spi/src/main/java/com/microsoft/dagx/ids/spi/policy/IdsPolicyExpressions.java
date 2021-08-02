/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

import com.microsoft.dagx.policy.model.LiteralExpression;

/**
 * Policy expressions defined by IDS.
 */
public interface IdsPolicyExpressions {

    String PAY_AMOUNT = "idsc:PAY_AMOUNT";

    String ABS_SPATIAL_POSITION = "ids:absoluteSpatialPosition";

    LiteralExpression ABS_SPATIAL_POSITION_EXPRESSION = new LiteralExpression(ABS_SPATIAL_POSITION);

    LiteralExpression PAY_AMOUNT_EXPRESSION = new LiteralExpression(PAY_AMOUNT);
}
