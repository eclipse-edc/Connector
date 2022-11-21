/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.contract.spi.testfixtures.offer.store;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.eclipse.edc.spi.asset.AssetSelectorExpression.SELECT_ALL;

public class TestFunctions {

    public static final long CONTRACT_DURATION = TimeUnit.DAYS.toSeconds(1);

    private TestFunctions() {
    }

    public static ContractDefinition createContractDefinition(String id, String accessPolicyId, String contractPolicyId, AssetSelectorExpression selectorExpression) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .selectorExpression(selectorExpression)
                .contractValidityDuration(CONTRACT_DURATION)
                .build();
    }

    public static ContractDefinition createContractDefinition(String id, String accessPolicyId, String contractPolicyId) {
        return createContractDefinition(id, accessPolicyId, contractPolicyId, AssetSelectorExpression.Builder.newInstance().build());
    }

    public static ContractDefinition createContractDefinition(String id) {
        return createContractDefinition(id, "access", "contract", SELECT_ALL);
    }

    public static List<ContractDefinition> createContractDefinitions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createContractDefinition("id" + i, "policy" + i, "contract" + i))
                .collect(Collectors.toList());
    }
}
