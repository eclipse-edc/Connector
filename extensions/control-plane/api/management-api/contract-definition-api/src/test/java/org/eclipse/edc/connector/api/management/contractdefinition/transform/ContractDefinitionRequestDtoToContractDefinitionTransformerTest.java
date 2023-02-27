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

package org.eclipse.edc.connector.api.management.contractdefinition.transform;

import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionCreateDto;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractDefinitionRequestDtoToContractDefinitionTransformerTest {

    private final ContractDefinitionRequestDtoToContractDefinitionTransformer transformer = new ContractDefinitionRequestDtoToContractDefinitionTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        when(context.transform(isA(CriterionDto.class), eq(Criterion.class))).thenReturn(new Criterion("left", "=", "right"));

        var contractDefinitionDto = ContractDefinitionCreateDto.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .criteria(List.of(CriterionDto.Builder.newInstance().operandLeft("left").operator("=").operandRight("right").build()))
                .validity(TimeUnit.MINUTES.toSeconds(10))
                .build();

        var contractDefinition = transformer.transform(contractDefinitionDto, context);

        assertThat(contractDefinition).isNotNull();
        assertThat(contractDefinition.getId()).isEqualTo(contractDefinitionDto.getId());
        assertThat(contractDefinition.getAccessPolicyId()).isEqualTo(contractDefinitionDto.getAccessPolicyId());
        assertThat(contractDefinition.getContractPolicyId()).isEqualTo(contractDefinitionDto.getContractPolicyId());
        assertThat(contractDefinition.getSelectorExpression().getCriteria()).usingRecursiveComparison().isEqualTo(contractDefinitionDto.getCriteria());
        assertThat(contractDefinition.getValidity()).isEqualTo(contractDefinitionDto.getValidity());
        assertThat(contractDefinition.getCreatedAt()).isNotZero(); //should be set automatically
        verify(context, times(1)).transform(isA(CriterionDto.class), eq(Criterion.class));
    }

}
