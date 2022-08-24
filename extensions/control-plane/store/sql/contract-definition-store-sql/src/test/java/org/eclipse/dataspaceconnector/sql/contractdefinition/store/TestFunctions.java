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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestFunctions {

    public static ContractDefinition getContractDefinition(String id, String accessPolicyId, String contractPolicyId) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().build())
                .build();
    }

    public static List<ContractDefinition> getContractDefinitions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> getContractDefinition("id" + i, "policy" + i, "contract" + i))
                .collect(Collectors.toList());
    }
}
