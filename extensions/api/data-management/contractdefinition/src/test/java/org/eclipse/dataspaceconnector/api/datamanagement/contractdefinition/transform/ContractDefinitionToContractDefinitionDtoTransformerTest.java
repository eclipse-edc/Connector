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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractDefinitionToContractDefinitionDtoTransformerTest {

    private final ContractDefinitionToContractDefinitionDtoTransformer transformer = new ContractDefinitionToContractDefinitionDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicy(Policy.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .contractPolicy(Policy.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "=", "right").build())
                .build();

        var dto = transformer.transform(contractDefinition, context);

        assertThat(dto.getId()).isEqualTo(contractDefinition.getId());
        assertThat(dto.getAccessPolicyId()).isEqualTo(contractDefinition.getAccessPolicy().getUid());
        assertThat(dto.getContractPolicyId()).isEqualTo(contractDefinition.getContractPolicy().getUid());
        assertThat(dto.getCriteria()).containsExactlyElementsOf(contractDefinition.getSelectorExpression().getCriteria());
    }

}