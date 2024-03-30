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
 *       Microsoft Corporation - Initial implementation
 *       SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.offer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class ContractDefinitionTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new TypeManager().getMapper();
        var definition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .assetsSelectorCriterion(criterion("field", "=", "value"))
                .privateProperties(Map.of("key1", "value2"))
                .build();

        var serialized = mapper.writeValueAsString(definition);
        var deserialized = mapper.readValue(serialized, ContractDefinition.class);

        assertThat(deserialized).isNotNull().usingRecursiveComparison().isEqualTo(definition);
        assertThat(deserialized.getCreatedAt()).isNotZero();
    }
}
