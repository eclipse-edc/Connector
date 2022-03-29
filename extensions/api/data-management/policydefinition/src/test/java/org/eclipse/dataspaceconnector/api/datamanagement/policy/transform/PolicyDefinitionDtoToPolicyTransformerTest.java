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

import org.assertj.core.api.SoftAssertions;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionDto;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
        ArrayList<Permission> permissionsTest = new ArrayList<>();
        ArrayList<Prohibition> prohibitionsTest = new ArrayList<>();
        ArrayList<Duty> obligationsTest = new ArrayList<>();

        var context = mock(TransformerContext.class);
        var policyDefinitionDto = PolicyDefinitionDto.Builder.newInstance()
                .inheritsFrom("inheritant")
                .assigner("the tester")
                .assignee("the tested")
                .target("the target")
                .extensibleProperties(Map.of("key", "value"))
                .permissions(permissionsTest)
                .prohibitions(prohibitionsTest)
                .obligations(obligationsTest)
                .build();

        var policy = transformer.transform(policyDefinitionDto, context);

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(policy.getInheritsFrom()).isEqualTo(policyDefinitionDto.getInheritsFrom());
        softAssertions.assertThat(policy.getAssigner()).isEqualTo(policyDefinitionDto.getAssigner());
        softAssertions.assertThat(policy.getAssignee()).isEqualTo(policyDefinitionDto.getAssignee());
        softAssertions.assertThat(policy.getTarget()).isEqualTo(policyDefinitionDto.getTarget());
        softAssertions.assertThat(policy.getExtensibleProperties()).containsExactlyEntriesOf(policyDefinitionDto.getExtensibleProperties());
        softAssertions.assertThat(policy.getPermissions()).hasSameElementsAs(policyDefinitionDto.getPermissions());
        softAssertions.assertThat(policy.getProhibitions()).hasSameElementsAs(policyDefinitionDto.getProhibitions());
        softAssertions.assertThat(policy.getObligations()).hasSameElementsAs(policyDefinitionDto.getObligations());
        softAssertions.assertAll();
    }

}
