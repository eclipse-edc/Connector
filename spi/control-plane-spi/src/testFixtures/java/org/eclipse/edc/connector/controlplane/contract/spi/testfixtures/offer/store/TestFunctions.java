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
 *       SAP SE - SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.offer.store;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestFunctions {

    private TestFunctions() {
    }

    public static ContractDefinition createContractDefinition(String id) {
        return createContractDefinition(id, "access", "contract");
    }

    public static ContractDefinition createContractDefinition(String id, String accessPolicyId, String contractPolicyId) {
        return createContractDefinitionBuilder(id, accessPolicyId, contractPolicyId, Map.of()).build();
    }

    public static ContractDefinition.Builder createContractDefinitionBuilder(String id) {
        return createContractDefinitionBuilder(id, "access", "contract", Map.of());
    }

    public static ContractDefinition.Builder createContractDefinitionBuilder(String id, String accessPolicyId, String contractPolicyId, Map<String, Object> privateProperties) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .privateProperties(privateProperties)
                .participantContextId("participantContextId")
                .createdAt(1234);
    }

    public static ContractDefinition createContractDefinition(String id, String accessPolicyId, String contractPolicyId, Map<String, Object> privateProperties) {
        return createContractDefinitionBuilder(id, accessPolicyId, contractPolicyId, privateProperties).build();
    }

    public static List<ContractDefinition> createContractDefinitions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createContractDefinition("id" + i, "policy" + i, "contract" + i))
                .collect(Collectors.toList());
    }
}
