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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.api.datamanagement.contractagreement;

import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.datamanagement.contractagreement.model.ContractAgreementDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractAgreementApiControllerTest {

    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private final ContractAgreementService service = mock(ContractAgreementService.class);

    private ContractAgreementApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractAgreementApiController(monitor, service, transformerRegistry);
    }

    @Test
    void getAll() {
        var contractAgreement = createContractAgreement();
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractAgreement)));
        var dto = ContractAgreementDto.Builder.newInstance().id(contractAgreement.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractAgreementDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allContractAgreements = controller.getAllAgreements(querySpec);

        assertThat(allContractAgreements).hasSize(1).first().matches(d -> d.getId().equals(contractAgreement.getId()));
        verify(transformerRegistry).transform(contractAgreement, ContractAgreementDto.class);
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        var contractAgreement = createContractAgreement();
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractAgreement)));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(ContractAgreement.class), eq(ContractAgreementDto.class)))
                .thenReturn(Result.failure("failure"));

        var allContractAgreements = controller.getAllAgreements(QuerySpecDto.Builder.newInstance().build());

        assertThat(allContractAgreements).hasSize(0);
        verify(transformerRegistry).transform(contractAgreement, ContractAgreementDto.class);
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();

        assertThatThrownBy(() -> controller.getAllAgreements(querySpecDto)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getAll_withInvalidQuery_shouldThrowException() {
        var contractAgreement = createContractAgreement();
        when(service.query(any())).thenReturn(ServiceResult.badRequest("test error message"));

        var dto = ContractAgreementDto.Builder.newInstance().id(contractAgreement.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractAgreementDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().filter("invalid=foobar").build();

        assertThatThrownBy(() -> controller.getAllAgreements(querySpec)).isInstanceOf(InvalidRequestException.class);

    }

    @Test
    void queryAll() {
        var contractAgreement = createContractAgreement();
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractAgreement)));
        var dto = ContractAgreementDto.Builder.newInstance().id(contractAgreement.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractAgreementDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allContractAgreements = controller.queryAllAgreements(querySpec);

        assertThat(allContractAgreements).hasSize(1).first().matches(d -> d.getId().equals(contractAgreement.getId()));
        verify(transformerRegistry).transform(contractAgreement, ContractAgreementDto.class);
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void queryAll_filtersOutFailedTransforms() {
        var contractAgreement = createContractAgreement();
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractAgreement)));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(ContractAgreement.class), eq(ContractAgreementDto.class)))
                .thenReturn(Result.failure("failure"));

        var allContractAgreements = controller.queryAllAgreements(QuerySpecDto.Builder.newInstance().build());

        assertThat(allContractAgreements).hasSize(0);
        verify(transformerRegistry).transform(contractAgreement, ContractAgreementDto.class);
    }

    @Test
    void queryAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();

        assertThatThrownBy(() -> controller.queryAllAgreements(querySpecDto)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void queryAll_withInvalidQuery_shouldThrowException() {
        var contractAgreement = createContractAgreement();
        when(service.query(any())).thenReturn(ServiceResult.badRequest("test error message"));

        var dto = ContractAgreementDto.Builder.newInstance().id(contractAgreement.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractAgreementDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().filterExpression(List.of(CriterionDto.from("invalid", "=", "foo"))).build();

        assertThatThrownBy(() -> controller.queryAllAgreements(querySpec)).isInstanceOf(InvalidRequestException.class);

    }

    @Test
    void getContractDef_found() {
        var contractAgreement = createContractAgreement();
        when(service.findById("agreementId")).thenReturn(contractAgreement);
        var dto = ContractAgreementDto.Builder.newInstance().id(contractAgreement.getId()).build();
        when(transformerRegistry.transform(isA(ContractAgreement.class), eq(ContractAgreementDto.class))).thenReturn(Result.success(dto));

        var retrieved = controller.getContractAgreement("agreementId");

        assertThat(retrieved).isNotNull();
    }

    @Test
    void getContractDef_notFound() {
        when(service.findById("agreementId")).thenReturn(null);

        assertThatThrownBy(() -> controller.getContractAgreement("nonExistingId")).isInstanceOf(ObjectNotFoundException.class);
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void getContractDef_notFoundIfTransformationFails() {
        var contractAgreement = createContractAgreement();
        when(service.findById("agreementId")).thenReturn(contractAgreement);
        when(transformerRegistry.transform(isA(ContractAgreement.class), eq(ContractAgreementDto.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.getContractAgreement("nonExistingId")).isInstanceOf(ObjectNotFoundException.class);
    }

    private ContractAgreement createContractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .consumerAgentId(UUID.randomUUID().toString())
                .providerAgentId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

}
