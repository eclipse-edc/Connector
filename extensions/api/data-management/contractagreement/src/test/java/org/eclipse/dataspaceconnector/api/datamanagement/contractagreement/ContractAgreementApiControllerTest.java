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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement;

import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.service.ContractAgreementService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

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
        when(service.query(any())).thenReturn(List.of(contractAgreement));
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
        when(service.query(any())).thenReturn(List.of(contractAgreement));
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

        assertThatThrownBy(() -> controller.getAllAgreements(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(IllegalArgumentException.class);
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