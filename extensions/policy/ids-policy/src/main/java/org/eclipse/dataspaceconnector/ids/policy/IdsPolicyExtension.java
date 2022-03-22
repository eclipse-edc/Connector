/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.policy;

import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static org.eclipse.dataspaceconnector.spi.policy.PolicyEngine.ALL_SCOPES;

/**
 * Registers test policy functions.
 */
public class IdsPolicyExtension implements ServiceExtension {

    public static String ABS_SPATIAL_POSITION = "ids:absoluteSpatialPosition";
    public static String PARTNER_LEVEL = "ids:partnerLevel";
    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return "IDS Policy";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        policyEngine.registerFunction(ALL_SCOPES, Permission.class, ABS_SPATIAL_POSITION, new AbsSpatialPositionConstraintFunction());
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, PARTNER_LEVEL, new PartnerLevelConstraintFunction());
    }

}
