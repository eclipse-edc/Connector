/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionInputDto;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractDefinitionInputDtoToContractDefinitionTransformerTest {

    private final ContractDefinitionInputDtoToContractDefinitionTransformer transformer = new ContractDefinitionInputDtoToContractDefinitionTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var contractDefinitionDto = ContractDefinitionInputDto.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .criteria(List.of(new Criterion("left", "=", "right")))
                .build();

        var contractDefinition = transformer.transform(contractDefinitionDto, context);

        assertThat(contractDefinition.getId()).isEqualTo(contractDefinitionDto.getId());
        assertThat(contractDefinition.getAccessPolicyId()).isEqualTo(contractDefinitionDto.getAccessPolicyId());
        assertThat(contractDefinition.getContractPolicyId()).isEqualTo(contractDefinitionDto.getContractPolicyId());
        assertThat(contractDefinition.getSelectorExpression().getCriteria()).containsExactlyElementsOf(contractDefinitionDto.getCriteria());
        assertThat(contractDefinition.getCreatedAt()).isNotEqualTo(0L); //should be set automatically
    }

}