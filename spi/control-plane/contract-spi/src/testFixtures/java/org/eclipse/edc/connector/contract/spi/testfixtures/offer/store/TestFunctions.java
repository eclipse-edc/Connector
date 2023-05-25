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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestFunctions {

    private TestFunctions() {
    }

    public static ContractDefinition createContractDefinition(String id) {
        return createContractDefinition(id, "access", "contract");
    }

    public static ContractDefinition createContractDefinition(String id, String accessPolicyId, String contractPolicyId) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .build();
    }

    public static List<ContractDefinition> createContractDefinitions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createContractDefinition("id" + i, "policy" + i, "contract" + i))
                .collect(Collectors.toList());
    }
}
