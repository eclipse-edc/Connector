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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.offer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var definition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build()).contractPolicy(Policy.Builder.newInstance().build()).selectorExpression(AssetSelectorExpression.SELECT_ALL).build();
        var serialized = mapper.writeValueAsString(definition);

        var deserialized = mapper.readValue(serialized, ContractDefinition.class);

        assertThat(deserialized).isNotNull();
    }
}
