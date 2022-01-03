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

package org.eclipse.dataspaceconnector.ids.policy.mock;

import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyExpressions.ABS_SPATIAL_POSITION;

/**
 * Registers test policy functions.
 */
public class IdsMockPolicyExtension implements ServiceExtension {

    @Inject
    private IdsPolicyService policyService;

    @Override
    public String name() {
        return "IDS Mock Policy";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        // handle region restriction
        policyService.registerRequestPermissionFunction(ABS_SPATIAL_POSITION, (operator, rightValue, permission, policyContext) -> rightValue != null && rightValue.equals(policyContext.getClaimToken().getClaims().get("region")));
    }

}
