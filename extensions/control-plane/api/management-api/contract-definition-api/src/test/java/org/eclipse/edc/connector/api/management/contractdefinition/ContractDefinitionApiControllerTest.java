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

package org.eclipse.edc.connector.api.management.contractdefinition;

import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionCreateDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionUpdateDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionUpdateDtoWrapper;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Stream;

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
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractDefinition)));
        var dto = ContractDefinitionResponseDto.Builder.newInstance().id(contractDefinition.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractDefinitionResponseDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allContractDefinitions = controller.getAllContractDefinitions(querySpec);

        assertThat(allContractDefinitions).hasSize(1).first().matches(d -> d.getId().equals(contractDefinition.getId()));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(contractDefinition, ContractDefinitionResponseDto.class);
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractDefinition)));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionResponseDto.class)))
                .thenReturn(Result.failure("failure"));

        var allContractDefinitions = controller.getAllContractDefinitions(QuerySpecDto.Builder.newInstance().build());

        assertThat(allContractDefinitions).isEmpty();
        verify(transformerRegistry).transform(contractDefinition, ContractDefinitionResponseDto.class);
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getAllContractDefinitions(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void queryAll() {
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractDefinition)));
        var dto = ContractDefinitionResponseDto.Builder.newInstance().id(contractDefinition.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractDefinitionResponseDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allContractDefinitions = controller.queryAllContractDefinitions(querySpec);

        assertThat(allContractDefinitions).hasSize(1).first().matches(d -> d.getId().equals(contractDefinition.getId()));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(contractDefinition, ContractDefinitionResponseDto.class);
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void queryAll_filtersOutFailedTransforms() {
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractDefinition)));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionResponseDto.class)))
                .thenReturn(Result.failure("failure"));

        var allContractDefinitions = controller.queryAllContractDefinitions(QuerySpecDto.Builder.newInstance().build());

        assertThat(allContractDefinitions).hasSize(0);
        verify(transformerRegistry).transform(contractDefinition, ContractDefinitionResponseDto.class);
    }

    @Test
    void queryAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.queryAllContractDefinitions(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getContractDef_found() {
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(service.findById("definitionId")).thenReturn(contractDefinition);
        var dto = ContractDefinitionResponseDto.Builder.newInstance().id(contractDefinition.getId()).build();
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionResponseDto.class))).thenReturn(Result.success(dto));

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
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(service.findById("definitionId")).thenReturn(contractDefinition);
        when(transformerRegistry.transform(isA(ContractDefinition.class), eq(ContractDefinitionResponseDto.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.getContractDefinition("nonExistingId")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void createContractDefinition_success() {
        var dto = ContractDefinitionCreateDto.Builder.newInstance().build();
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(transformerRegistry.transform(isA(ContractDefinitionCreateDto.class), eq(ContractDefinition.class))).thenReturn(Result.success(contractDefinition));
        when(service.create(any())).thenReturn(ServiceResult.success(contractDefinition));

        var contractDefinitionId = controller.createContractDefinition(dto);

        assertThat(contractDefinitionId).isNotNull();
        assertThat(contractDefinitionId).isInstanceOf(IdResponseDto.class);
        assertThat(contractDefinitionId.getId()).isNotEmpty();
        assertThat(contractDefinitionId.getCreatedAt()).isNotEqualTo(0L);

        verify(service).create(contractDefinition);
        verify(transformerRegistry).transform(isA(ContractDefinitionCreateDto.class), eq(ContractDefinition.class));
    }

    @Test
    void createContractDefinition_returnExpectedId() {
        var definedContractDefinitionId = UUID.randomUUID().toString();
        var dto = ContractDefinitionCreateDto.Builder.newInstance().build();
        var contractDefinition = createContractDefinition(definedContractDefinitionId);

        when(transformerRegistry.transform(isA(ContractDefinitionCreateDto.class), eq(ContractDefinition.class))).thenReturn(Result.success(contractDefinition));
        when(service.create(any())).thenReturn(ServiceResult.success(contractDefinition));

        var contractDefinitionId = controller.createContractDefinition(dto);
        assertThat(contractDefinitionId).isNotNull();
        assertThat(contractDefinitionId.getId()).isEqualTo(definedContractDefinitionId);
    }

    @Test
    void createContractDefinition_alreadyExists() {
        var dto = ContractDefinitionCreateDto.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(ContractDefinitionCreateDto.class), eq(ContractDefinition.class))).thenReturn(Result.success(createContractDefinition(UUID.randomUUID().toString())));
        when(service.create(any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createContractDefinition(dto)).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void createContractDefinition_transformationFails() {
        var dto = ContractDefinitionCreateDto.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(ContractDefinitionCreateDto.class), eq(ContractDefinition.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.createContractDefinition(dto)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void delete() {
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
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

        assertThatThrownBy(() -> controller.deleteContractDefinition("definitionId")).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void update_whenExists() {
        var dto = ContractDefinitionUpdateDto.Builder.newInstance().build();
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(transformerRegistry.transform(isA(ContractDefinitionUpdateDtoWrapper.class), eq(ContractDefinition.class))).thenReturn(Result.success(contractDefinition));
        when(service.update(eq(contractDefinition))).thenReturn(ServiceResult.success(null));

        controller.updateContractDefinition(contractDefinition.getId(), dto);


        verify(service).update(contractDefinition);
        verify(transformerRegistry).transform(isA(ContractDefinitionUpdateDtoWrapper.class), eq(ContractDefinition.class));
    }


    @Test
    void update_whenNotExists_shouldThrowException() {
        var dto = ContractDefinitionUpdateDto.Builder.newInstance().build();
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(transformerRegistry.transform(isA(ContractDefinitionUpdateDtoWrapper.class), eq(ContractDefinition.class))).thenReturn(Result.success(contractDefinition));
        when(service.update(eq(contractDefinition))).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.updateContractDefinition(contractDefinition.getId(), dto)).isInstanceOf(ObjectNotFoundException.class);

    }

    @Test
    void update_whenTransformationFails_shouldThrowException() {
        var dto = ContractDefinitionUpdateDto.Builder.newInstance().build();
        var contractDefinition = createContractDefinition(UUID.randomUUID().toString());
        when(transformerRegistry.transform(isA(ContractDefinitionUpdateDtoWrapper.class), eq(ContractDefinition.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.updateContractDefinition(contractDefinition.getId(), dto)).isInstanceOf(InvalidRequestException.class);
    }

    private ContractDefinition createContractDefinition(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .validity(100)
                .build();
    }
}
