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
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyApiControllerTest {

    private final PolicyService policyService = mock(PolicyService.class);
    private PolicyApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new PolicyApiController(monitor, policyService);
    }

    @Test
    void getPolicyById() {
        when(policyService.findById("id")).thenReturn(Policy.Builder.newInstance().build());

        var policyDto = controller.getPolicy("id");

        assertThat(policyDto).isNotNull();
    }

    @Test
    void getPolicyById_notExists() {
        when(policyService.findById("id")).thenReturn(null);

        assertThatThrownBy(() -> controller.getPolicy("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getAllPolicies() {

        when(policyService.query(any())).thenReturn(List.of(Policy.Builder.newInstance().build()));
        var allPolicies = controller.getAllPolicies(1, 10, "field=value", SortOrder.ASC, "field");

        var result = allPolicies.get(0);
        var size = allPolicies.size();

        assertThat(allPolicies).hasSize(1);
        verify(policyService).query(argThat(s ->
                s.getOffset() == 1 &&
                        s.getLimit() == 10 &&
                        s.getFilterExpression().get(0).equals(new Criterion("field", "=", "value")) &&
                        s.getSortOrder().equals(SortOrder.ASC) &&
                        s.getSortField().equals("field")
        ));
    }

    @Test
    void createPolicy() {
        var policyDefinition = Policy.Builder.newInstance()
                .inheritsFrom("inheritant")
                .assigner("the tester")
                .assignee("the tested")
                .target("the target")
                .extensibleProperties(Map.of("key", "value"))
                .permissions(List.of())
                .prohibitions(List.of())
                .duties(List.of())
                .id("an Id")
                .build();

        var policy = Policy.Builder.newInstance().build();

        when(policyService.create(any())).thenReturn(ServiceResult.success(policy));

        controller.createPolicy(policyDefinition);

        verify(policyService).create(isA(Policy.class));
    }

    @Test
    void createPolicy_alreadyExists() {
        var policyDefinition = Policy.Builder.newInstance()
                .inheritsFrom("inheritant")
                .assigner("the tester")
                .assignee("the tested")
                .target("the target")
                .extensibleProperties(Map.of("key", "value"))
                .permissions(List.of())
                .prohibitions(List.of())
                .duties(List.of())
                .id("an Id")
                .build();

        var policy = Policy.Builder.newInstance().build();
        when(policyService.create(any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createPolicy(policyDefinition)).isInstanceOf(ObjectExistsException.class);
    }


    @Test
    void deletePolicy() {
        when(policyService.deleteById("id")).thenReturn(ServiceResult.success(Policy.Builder.newInstance().build()));
        controller.deletePolicy("id");
        verify(policyService).deleteById("id");
    }

    @Test
    void deletePolicy_notFound() {
        when(policyService.deleteById("id")).thenReturn(ServiceResult.notFound("Not found"));
        assertThatThrownBy(() -> controller.deletePolicy("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void deletePolicy_conflicts() {
        when(policyService.deleteById("id")).thenReturn(ServiceResult.conflict("Conflicting"));
        assertThatThrownBy(() -> controller.deletePolicy("id")).isInstanceOf(ObjectExistsException.class);
    }
}
