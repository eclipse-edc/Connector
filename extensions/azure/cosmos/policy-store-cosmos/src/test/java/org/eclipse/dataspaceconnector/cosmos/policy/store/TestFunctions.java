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

import java.util.UUID;

public class TestFunctions {

    public static Policy generatePolicy() {
        return generatePolicy(UUID.randomUUID().toString());
    }

    public static Policy generatePolicy(String id) {
        return Policy.Builder.newInstance()
                .id(id)
                .build();
    }

    public static PolicyDocument generateDocument(String partitionKey) {
        return new PolicyDocument(generatePolicy(), partitionKey);
    }
}
