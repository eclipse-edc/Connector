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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractDefinitionDtoToContractDefinitionTransformerTest {

    private final ContractDefinitionDtoToContractDefinitionTransformer transformer = new ContractDefinitionDtoToContractDefinitionTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var contractDefinitionDto = ContractDefinitionDto.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .criteria(List.of(new Criterion("left", "=", "right")))
                .build();

        var contractDefinition = transformer.transform(contractDefinitionDto, context);

        assertThat(contractDefinition.getId()).isEqualTo(contractDefinitionDto.getId());
        assertThat(contractDefinition.getAccessPolicy().getUid()).isEqualTo(contractDefinitionDto.getAccessPolicyId());
        assertThat(contractDefinition.getContractPolicy().getUid()).isEqualTo(contractDefinitionDto.getContractPolicyId());
        assertThat(contractDefinition.getSelectorExpression().getCriteria()).containsExactlyElementsOf(contractDefinitionDto.getCriteria());
    }

}