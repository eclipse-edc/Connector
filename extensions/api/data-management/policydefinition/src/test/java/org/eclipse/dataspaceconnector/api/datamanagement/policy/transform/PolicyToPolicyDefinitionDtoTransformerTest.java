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

public class PolicyToPolicyDefinitionDtoTransformerTest {

    private final PolicyToPolicyDefinitionDtoTransformer transformer = new PolicyToPolicyDefinitionDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isInstanceOf(Policy.class);
        assertThat(transformer.getOutputType()).isInstanceOf(PolicyDefinitionDto.class);
    }

    @Test
    void transform() {
        ArrayList<Permission> permissionsTest = new ArrayList<>();
        ArrayList<Prohibition> prohibitionsTest = new ArrayList<>();
        ArrayList<Duty> obligationsTest = new ArrayList<>();

        var context = mock(TransformerContext.class);
        var policy = Policy.Builder.newInstance()
                .inheritsFrom("inheritant")
                .assigner("the tester")
                .assignee("the tested")
                .target("the target")
                .extensibleProperties(Map.of("key", "value"))
                .permissions(permissionsTest)
                .prohibitions(prohibitionsTest)
                .duties(obligationsTest)
                .build();

        var policyDefinitionDto = transformer.transform(policy, context);

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(policyDefinitionDto.getInheritsFrom()).isEqualTo(policy.getInheritsFrom());
        softAssertions.assertThat(policyDefinitionDto.getAssigner()).isEqualTo(policy.getAssigner());
        softAssertions.assertThat(policyDefinitionDto.getAssignee()).isEqualTo(policy.getAssignee());
        softAssertions.assertThat(policyDefinitionDto.getTarget()).isEqualTo(policy.getTarget());
        softAssertions.assertThat(policyDefinitionDto.getExtensibleProperties()).containsExactlyEntriesOf(policy.getExtensibleProperties());
        softAssertions.assertThat(policyDefinitionDto.getPermissions()).hasSameElementsAs(policy.getPermissions());
        softAssertions.assertThat(policyDefinitionDto.getProhibitions()).hasSameElementsAs(policy.getProhibitions());
        softAssertions.assertThat(policyDefinitionDto.getObligations()).hasSameElementsAs(policy.getObligations());
        softAssertions.assertAll();
    }

}
