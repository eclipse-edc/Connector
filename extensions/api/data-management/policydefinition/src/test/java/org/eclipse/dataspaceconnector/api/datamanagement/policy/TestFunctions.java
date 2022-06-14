/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;

import java.util.List;
import java.util.Map;

public class TestFunctions {

    static PolicyDefinition createPolicy(String id) {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance()
                        .inheritsFrom("inheritant")
                        .assigner("the tester")
                        .assignee("the tested")
                        .target("the target")
                        .extensibleProperties(Map.of("key", "value"))
                        .permissions(List.of())
                        .prohibitions(List.of())
                        .duties(List.of())
                        .build())
                .uid(id)
                .build();
    }

    static AssetSelectorExpression createSelectorExpression() {
        return AssetSelectorExpression.SELECT_ALL;
    }
}
