/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.policy.model;

import java.util.List;

/**
 * Returns types that can be registered with an ObjectMapper for deserialization.
 */
public final class PolicyRegistrationTypes {

    /**
     * The types that can be registered with an ObjectMapper.
     */
    public static final List<Class<?>> TYPES = List.of(
            Action.class,
            AndConstraint.class,
            AtomicConstraint.class,
            Duty.class,
            LiteralExpression.class,
            OrConstraint.class,
            Permission.class,
            Policy.class,
            PolicyType.class,
            Prohibition.class,
            XoneConstraint.class);

    private PolicyRegistrationTypes() {
    }
}
