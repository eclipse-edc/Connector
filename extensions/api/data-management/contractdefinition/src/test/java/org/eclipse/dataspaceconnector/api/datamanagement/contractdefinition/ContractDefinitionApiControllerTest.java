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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractDefinitionApiControllerTest {

    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private final ContractDefinitionService service = mock(ContractDefinitionService.class);
    private ContractDefinitionApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractDefinitionApiController(monitor, service, transformerRegistry);
    }

    @Test
    void getAll() {
        var contractDefinition = createContractDefinition();
        when(service.query(any())).thenReturn(List.of(contractDefinition));
        var dto = ContractDefinitionDto.Builder.newInstance().id(contractDefinition.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractDefinitionDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allContractDefinitions = controller.getAllContractDefinitions(querySpec);

        assertThat(allContractDefinitions).hasSize(1).first().matches(d -> d.getId().equals(contractDefinition.getId()));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(contractDefinition, ContractDefinitionDto.class);
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        var contractDefinition = createContractDefinition();
        when(service.query(any())).thenReturn(List.of(contractDefinition));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionDto.class)))
                .thenReturn(Result.failure("failure"));

        var allContractDefinitions = controller.getAllContractDefinitions(QuerySpecDto.Builder.newInstance().build());

        assertThat(allContractDefinitions).hasSize(0);
        verify(transformerRegistry).transform(contractDefinition, ContractDefinitionDto.class);
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getAllContractDefinitions(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getContractDef_found() {
        var contractDefinition = createContractDefinition();
        when(service.findById("definitionId")).thenReturn(contractDefinition);
        var dto = ContractDefinitionDto.Builder.newInstance().id(contractDefinition.getId()).build();
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionDto.class))).thenReturn(Result.success(dto));

        var retrieved = controller.getContractDefinition("definitionId");

        assertThat(retrieved).isNotNull();
    }

    @Test
    void getContractDef_notFound() {
        when(service.findById("definitionId")).thenReturn(null);

        assertThatThrownBy(() -> controller.getContractDefinition("nonExistingId")).isInstanceOf(ObjectNotFoundException.class);
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void getContractDef_notFoundIfTransformationFails() {
        var contractDefinition = createContractDefinition();
        when(service.findById("definitionId")).thenReturn(contractDefinition);
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionDto.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.getContractDefinition("nonExistingId")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void createContractDefinition_success() {
        var dto = ContractDefinitionDto.Builder.newInstance().build();
        var contractDefinition = createContractDefinition();
        when(transformerRegistry.transform(isA(ContractDefinitionDto.class), eq(ContractDefinition.class))).thenReturn(Result.success(contractDefinition));
        when(service.create(any())).thenReturn(ServiceResult.success(contractDefinition));

        controller.createContractDefinition(dto);

        verify(service).create(isA(ContractDefinition.class));
        verify(transformerRegistry).transform(isA(ContractDefinitionDto.class), eq(ContractDefinition.class));
    }

    @Test
    void createContractDefinition_alreadyExists() {
        var dto = ContractDefinitionDto.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(ContractDefinitionDto.class), eq(ContractDefinition.class))).thenReturn(Result.success(createContractDefinition()));
        when(service.create(any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createContractDefinition(dto)).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void createContractDefinition_transformationFails() {
        var dto = ContractDefinitionDto.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(ContractDefinitionDto.class), eq(ContractDefinition.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.createContractDefinition(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete() {
        var contractDefinition = createContractDefinition();
        when(service.delete("definitionId")).thenReturn(ServiceResult.success(contractDefinition));

        controller.deleteContractDefinition("definitionId");

        verify(service).delete("definitionId");
    }

    @Test
    void delete_notFound() {
        when(service.delete("definitionId")).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.deleteContractDefinition("definitionId")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void delete_notPossible() {
        when(service.delete("definitionId")).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.deleteContractDefinition("definitionId")).isInstanceOf(ObjectExistsException.class);
    }

    private ContractDefinition createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();
    }
}
