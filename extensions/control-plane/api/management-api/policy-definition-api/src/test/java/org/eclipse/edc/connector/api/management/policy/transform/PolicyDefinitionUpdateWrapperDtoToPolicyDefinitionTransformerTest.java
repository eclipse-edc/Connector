/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.api.management.policy.transform;

import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateWrapperDto;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformerTest {

    private final PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformer transformer = new PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var policyDefinitionDto = PolicyDefinitionUpdateWrapperDto.Builder.newInstance()
                .updateRequest(PolicyDefinitionUpdateDto.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build()).build())
                .build();

        var policyDefinition = transformer.transform(policyDefinitionDto, context);

        assertThat(policyDefinition).isNotNull();
        assertThat(policyDefinition.getPolicy()).isEqualTo(policyDefinitionDto.getUpdateDto().getPolicy());
        assertThat(policyDefinition.getCreatedAt()).isNotEqualTo(0L); //should be set automatically
    }

}
