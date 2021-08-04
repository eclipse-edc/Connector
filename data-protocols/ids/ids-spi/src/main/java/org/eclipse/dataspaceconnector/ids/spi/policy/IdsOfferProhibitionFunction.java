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

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;

/**
 * Evaluates a constraint attached to a prohibition.
 */
@FunctionalInterface
public interface IdsOfferProhibitionFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator    the operation
     * @param rightValue  the right-side expression for the constraint
     * @param prohibition the prohibition associated with the constraint
     * @param context     the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Prohibition prohibition, IdsOfferPolicyContext context);

}
