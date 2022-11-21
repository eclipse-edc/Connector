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
 *
 */

package org.eclipse.edc.connector.contract.spi.types.offer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var definition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .contractValidityDuration(TimeUnit.HOURS.toSeconds(5))
                .build();

        var serialized = mapper.writeValueAsString(definition);
        var deserialized = mapper.readValue(serialized, ContractDefinition.class);

        assertThat(deserialized).isNotNull().usingRecursiveComparison().isEqualTo(definition);
        assertThat(deserialized.getCreatedAt()).isNotZero();
    }
}
