/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.cosmos.policy.store;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;

import java.util.UUID;

public class TestFunctions {

    public static PolicyDefinition generatePolicy() {
        return generatePolicy(UUID.randomUUID().toString());
    }

    public static PolicyDefinition generatePolicy(String id) {
        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .uid(id)
                .build();
    }

    public static PolicyDocument generateDocument(String partitionKey) {
        return new PolicyDocument(generatePolicy(), partitionKey);
    }
}
