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

package org.eclipse.dataspaceconnector.contract.definition.store;

import org.eclipse.dataspaceconnector.cosmos.policy.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.UUID;

public class TestFunctions {

    public static final String ACCESS_POLICY_ID = "test-ap-id1";
    public static final String CONTRACT_POLICY_ID = "test-cp-id1";

    public static ContractDefinition generateDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractPolicyId(CONTRACT_POLICY_ID)
                .accessPolicyId(ACCESS_POLICY_ID)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("somekey", "someval").build())
                .build();
    }

    public static ContractDefinitionDocument generateDocument(String partitionKey) {
        return new ContractDefinitionDocument(generateDefinition(), partitionKey);
    }
}
