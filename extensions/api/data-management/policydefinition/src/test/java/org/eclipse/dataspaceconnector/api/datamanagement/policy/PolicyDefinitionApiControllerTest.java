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

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyDefinitionApiControllerTest {

    private final PolicyService service = mock(PolicyService.class);
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private PolicyDefinitionApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new PolicyDefinitionApiController(monitor, service, transformerRegistry);
    }

    @Test
    void getPolicyById() {
        when(service.findById("id")).thenReturn(TestFunctions.createPolicy("id"));

        var policyDto = controller.getPolicy("id");

        assertThat(policyDto).isNotNull();
    }

    @Test
    void getPolicyById_notExists() {
        when(service.findById("id")).thenReturn(null);

        assertThatThrownBy(() -> controller.getPolicy("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getAllPolicies() {
        when(service.query(any())).thenReturn(List.of(TestFunctions.createPolicy("id")));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allPolicies = controller.getAllPolicies(querySpec);

        assertThat(allPolicies).hasSize(1);
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getAllPolicies(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPolicy() {
        var policyDefinition = TestFunctions.createPolicy("id");
        when(service.create(any())).thenReturn(ServiceResult.success(policyDefinition));

        controller.createPolicy(policyDefinition);

        verify(service).create(isA(PolicyDefinition.class));
    }

    @Test
    void createPolicy_alreadyExists() {
        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance()
                        .inheritsFrom("inheritant")
                        .assigner("the tester")
                        .assignee("the tested")
                        .target("the target")
                        .extensibleProperties(Map.of("key", "value"))
                        .permissions(List.of())
                        .prohibitions(List.of())
                        .duties(List.of())
                        .build())
                .uid("an Id")
                .build();

        var policy = Policy.Builder.newInstance().build();
        when(service.create(any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createPolicy(policyDefinition)).isInstanceOf(ObjectExistsException.class);
    }


    @Test
    void deletePolicy() {
        when(service.deleteById("id")).thenReturn(ServiceResult.success(TestFunctions.createPolicy("id")));
        controller.deletePolicy("id");
        verify(service).deleteById("id");
    }

    @Test
    void deletePolicy_notFound() {
        when(service.deleteById("id")).thenReturn(ServiceResult.notFound("Not found"));
        assertThatThrownBy(() -> controller.deletePolicy("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void deletePolicy_conflicts() {
        when(service.deleteById("id")).thenReturn(ServiceResult.conflict("Conflicting"));
        assertThatThrownBy(() -> controller.deletePolicy("id")).isInstanceOf(ObjectExistsException.class);
    }
}
