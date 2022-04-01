/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionDto;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PolicyDefinitionDtoToPolicyTransformerTest {

    private final PolicyDefinitionDtoToPolicyTransformer transformer = new PolicyDefinitionDtoToPolicyTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isInstanceOf(PolicyDefinitionDto.class);
        assertThat(transformer.getOutputType()).isInstanceOf(Policy.class);
    }

    @Test
    void transform() {

        var context = mock(TransformerContext.class);
        var policyDefinitionDto = PolicyDefinitionDto.Builder.newInstance()
                .inheritsFrom("inheritant")
                .assigner("the tester")
                .assignee("the tested")
                .target("the target")
                .extensibleProperties(Map.of("key", "value"))
                .permissions(List.of())
                .prohibitions(List.of())
                .obligations(List.of())
                .id("an Id")
                .build();

        var policy = transformer.transform(policyDefinitionDto, context);

        assertThat(policy).usingRecursiveComparison().isEqualTo(policyDefinitionDto);

    }

}
