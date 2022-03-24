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

import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class TokenClaimValidationExtension implements ServiceExtension {

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return "Token Claim Validation Extension";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        TokenClaimFunctionFactory factory = new TokenClaimFunctionFactory(policyEngine, context.getMonitor());
        factory.register(context);
    }
}
