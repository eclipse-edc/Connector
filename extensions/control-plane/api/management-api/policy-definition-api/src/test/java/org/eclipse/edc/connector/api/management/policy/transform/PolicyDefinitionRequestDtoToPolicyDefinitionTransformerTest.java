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

package org.eclipse.edc.connector.api.management.policy.transform;

import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionRequestDto;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolicyDefinitionRequestDtoToPolicyDefinitionTransformerTest {

    private final PolicyDefinitionRequestDtoToPolicyDefinitionTransformer transformer = new PolicyDefinitionRequestDtoToPolicyDefinitionTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var policyDefinitionDto = PolicyDefinitionRequestDto.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();

        var policyDefinition = transformer.transform(policyDefinitionDto, context);

        assertThat(policyDefinition).isNotNull();
        assertThat(policyDefinition.getId()).isEqualTo(policyDefinitionDto.getId());
        assertThat(policyDefinition.getPolicy()).isEqualTo(policyDefinitionDto.getPolicy());
        assertThat(policyDefinition.getCreatedAt()).isNotEqualTo(0L); //should be set automatically
    }

    @Test
    void transform_nullInput() {
        var context = mock(TransformerContext.class);

        var definition = transformer.transform(null, context);

        assertThat(definition).isNull();
        verify(context).reportProblem("input policy definition is null");
    }

}
