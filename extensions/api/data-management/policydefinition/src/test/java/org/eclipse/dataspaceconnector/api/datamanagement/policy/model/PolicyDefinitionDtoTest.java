/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDefinitionDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build()).build();

        var dto = PolicyDefinitionDto.Builder.newInstance()
                .type(PolicyType.SET)
                .permissions(List.of(permission))
                .obligations(List.of(Duty.Builder.newInstance().assignee("test-assigner").build()))
                .extensibleProperties(Map.of("test-key", "test-value"))
                .build();

        var str = objectMapper.writeValueAsString(dto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, PolicyDefinitionDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);
    }
}
