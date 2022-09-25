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

import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionId;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

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

    private final PolicyDefinitionService service = mock(PolicyDefinitionService.class);
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
        var responseDto = PolicyDefinitionResponseDto.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        when(transformerRegistry.transform(isA(PolicyDefinition.class), eq(PolicyDefinitionResponseDto.class)))
                .thenReturn(Result.success(responseDto));

        var policyDto = controller.getPolicy("id");

        assertThat(policyDto).isNotNull();
    }

    @Test
    void getPolicyById_notExists() {
        when(service.findById("id")).thenReturn(null);

        assertThatThrownBy(() -> controller.getPolicy("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getPolicy_notFoundIfTransformationFails() {
        var policyDefinition = TestFunctions.createPolicy("id");
        when(service.findById("definitionId")).thenReturn(policyDefinition);
        when(transformerRegistry.transform(isA(PolicyDefinition.class), eq(PolicyDefinitionResponseDto.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.getPolicy("nonExistingId")).isInstanceOf(ObjectNotFoundException.class);
    }


    @Test
    void getAllPolicies() {
        when(service.query(any())).thenReturn(ServiceResult.success(List.of(TestFunctions.createPolicy("id"))));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var responseDto = PolicyDefinitionResponseDto.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        when(transformerRegistry.transform(isA(PolicyDefinition.class), eq(PolicyDefinitionResponseDto.class)))
                .thenReturn(Result.success(responseDto));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allPolicies = controller.getAllPolicies(querySpec);

        assertThat(allPolicies).hasSize(1);
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
        verify(transformerRegistry).transform(isA(PolicyDefinition.class), eq(PolicyDefinitionResponseDto.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        var policyDefinition = TestFunctions.createPolicy("id");
        when(service.query(any())).thenReturn(ServiceResult.success(List.of(policyDefinition)));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(PolicyDefinition.class), eq(PolicyDefinitionResponseDto.class)))
                .thenReturn(Result.failure("failure"));

        var allPolicyDefinitions = controller.getAllPolicies(QuerySpecDto.Builder.newInstance().build());

        assertThat(allPolicyDefinitions).hasSize(0);
        verify(transformerRegistry).transform(policyDefinition, PolicyDefinitionResponseDto.class);
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getAllPolicies(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createPolicy() {
        var dto = PolicyDefinitionRequestDto.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        var policyDefinition = TestFunctions.createPolicy("id");
        when(transformerRegistry.transform(isA(PolicyDefinitionRequestDto.class), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
        when(service.create(any())).thenReturn(ServiceResult.success(policyDefinition));

        var policyDefinitionId = controller.createPolicy(dto);

        assertThat(policyDefinitionId).isNotNull();
        assertThat(policyDefinitionId).isInstanceOf(PolicyDefinitionId.class);
        assertThat(policyDefinitionId.getId()).isNotEmpty();

        verify(service).create(isA(PolicyDefinition.class));
    }

    @Test
    void createPolicy_returnExpectedId() {
        var policyId = UUID.randomUUID().toString();
        var dto = PolicyDefinitionRequestDto.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

        var policyDefinition = TestFunctions.createPolicy(policyId);

        when(transformerRegistry.transform(isA(PolicyDefinitionRequestDto.class), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
        when(service.create(any())).thenReturn(ServiceResult.success(policyDefinition));

        var policyDefinitionId = controller.createPolicy(dto);
        assertThat(policyDefinitionId).isNotNull();
        assertThat(policyDefinitionId.getId()).isEqualTo(policyId);
    }

    @Test
    void createPolicy_alreadyExists() {
        var policy = Policy.Builder.newInstance().build();
        var dto = PolicyDefinitionRequestDto.Builder.newInstance().policy(policy).build();
        var policyDefinition = PolicyDefinition.Builder.newInstance()
                .policy(policy)
                .id("an Id")
                .build();

        when(service.create(any())).thenReturn(ServiceResult.conflict("already exists"));
        when(transformerRegistry.transform(isA(PolicyDefinitionRequestDto.class), eq(PolicyDefinition.class)))
                .thenReturn(Result.success(policyDefinition));

        assertThatThrownBy(() -> controller.createPolicy(dto)).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void createPolicy_transformationFails() {
        var dto = PolicyDefinitionRequestDto.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(PolicyDefinitionRequestDto.class), eq(PolicyDefinition.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.createPolicy(dto)).isInstanceOf(InvalidRequestException.class);
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
