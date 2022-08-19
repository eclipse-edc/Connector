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

package org.eclipse.dataspaceconnector.api.datamanagement.policy.transform;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolicyDefinitionToPolicyDefinitionResponseDtoTransformerTest {

    private final PolicyDefinitionToPolicyDefinitionResponseDtoTransformer transformer = new PolicyDefinitionToPolicyDefinitionResponseDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var contractDefinition = PolicyDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .createdAt(10L)
                .build();

        var dto = transformer.transform(contractDefinition, context);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(contractDefinition.getId());
        assertThat(dto.getPolicy()).isEqualTo(contractDefinition.getPolicy());
        assertThat(dto.getCreatedAt()).isEqualTo(10L);
    }

    @Test
    void transform_nullInput() {
        var context = mock(TransformerContext.class);

        var definition = transformer.transform(null, context);

        assertThat(definition).isNull();
        verify(context).reportProblem("input contract definition is null");
    }

}