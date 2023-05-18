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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.contractdefinition.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionRequestDtoTest {

    private final ObjectMapper objectMapper = new TypeManager().getMapper();

    @Test
    void verifySerialization() throws JsonProcessingException {
        var criterion = CriterionDto.Builder.newInstance().operandLeft("name").operator("beginsWith").operandRight("test").build();
        var dto = ContractDefinitionRequestDto.Builder.newInstance()
                .contractPolicyId("test-contract-policyid")
                .accessPolicyId("test-access-policyid")
                .id("test-id")
                .criteria(List.of(criterion))
                .build();

        var str = objectMapper.writeValueAsString(dto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, ContractDefinitionRequestDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);
    }
}
