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
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractDefinitionToContractDefinitionResponseDtoTransformerTest {

    private final ContractDefinitionToContractDefinitionResponseDtoTransformer transformer = new ContractDefinitionToContractDefinitionResponseDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        when(context.transform(isA(Criterion.class), eq(CriterionDto.class))).thenReturn(CriterionDto.Builder.newInstance().operandLeft("left").operator("=").operandRight("right").build());
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "=", "right").build())
                .build();

        var dto = transformer.transform(contractDefinition, context);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(contractDefinition.getId());
        assertThat(dto.getAccessPolicyId()).isEqualTo(contractDefinition.getAccessPolicyId());
        assertThat(dto.getContractPolicyId()).isEqualTo(contractDefinition.getContractPolicyId());
        assertThat(dto.getCreatedAt()).isNotZero();
        assertThat(dto.getCriteria()).usingRecursiveComparison().isEqualTo(contractDefinition.getSelectorExpression().getCriteria());
        verify(context).transform(isA(Criterion.class), eq(CriterionDto.class));
    }

}
