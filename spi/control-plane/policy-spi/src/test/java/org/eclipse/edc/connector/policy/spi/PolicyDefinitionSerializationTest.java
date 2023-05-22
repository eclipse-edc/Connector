/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.policy.spi;

import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDefinitionSerializationTest {

    private final TypeManager typeManager = new TypeManager();


    @Test
    void verifySerialization() {
        var policyDef = PolicyDefinition.Builder.newInstance()
                .id("test-policy-id")
                .createdAt(12345L)
                .policy(createPolicy())
                .build();

        var json = typeManager.writeValueAsString(policyDef);
        assertThat(json).isNotNull();
        assertThat(json).contains("createdAt");
        assertThat(json).contains("sampleInheritsFrom");

        var deserialized = typeManager.readValue(json, PolicyDefinition.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(policyDef);
        assertThat(deserialized.getCreatedAt()).isEqualTo(12345L);
    }

    private Policy createPolicy() {
        var permission = Permission.Builder.newInstance().build();

        var prohibition = Prohibition.Builder.newInstance().build();

        var duty = Duty.Builder.newInstance().build();

        var p = Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duties(List.of(duty))
                .inheritsFrom("sampleInheritsFrom")
                .assigner("sampleAssigner")
                .assignee("sampleAssignee")
                .target("sampleTarget")
                .type(PolicyType.SET);
        return p.build();
    }
}
