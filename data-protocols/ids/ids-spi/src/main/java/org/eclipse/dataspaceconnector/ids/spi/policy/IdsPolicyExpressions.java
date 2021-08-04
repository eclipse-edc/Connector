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

package org.eclipse.dataspaceconnector.ids.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;

/**
 * Policy expressions defined by IDS.
 */
public interface IdsPolicyExpressions {

    String PAY_AMOUNT = "idsc:PAY_AMOUNT";

    String ABS_SPATIAL_POSITION = "ids:absoluteSpatialPosition";

    LiteralExpression ABS_SPATIAL_POSITION_EXPRESSION = new LiteralExpression(ABS_SPATIAL_POSITION);

    LiteralExpression PAY_AMOUNT_EXPRESSION = new LiteralExpression(PAY_AMOUNT);
}
