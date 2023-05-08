/*
 *  Copyright (c) 2022 - 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - Added initiate-negotiation endpoint tests
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation;

import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractNegotiationApiControllerTest {
    private final ContractNegotiationService service = mock(ContractNegotiationService.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private ContractNegotiationApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractNegotiationApiController(monitor, service, transformerRegistry);
    }

    @Test
    void getAll() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractNegotiation)));
        var dto = ContractNegotiationDto.Builder.newInstance().id(contractNegotiation.getId()).build();
        when(transformerRegistry.transform(any(), eq(ContractNegotiationDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var negotiations = controller.getNegotiations(querySpec);

        assertThat(negotiations).hasSize(1).first().matches(d -> d.getId().equals(contractNegotiation.getId()));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(contractNegotiation, ContractNegotiationDto.class);
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(contractNegotiation)));
        when(transformerRegistry.transform(isA(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.failure("failure"));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));

        var negotiations = controller.getNegotiations(QuerySpecDto.Builder.newInstance().build());

        assertThat(negotiations).hasSize(0);
        verify(transformerRegistry).transform(contractNegotiation, ContractNegotiationDto.class);
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getNegotiations(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getContractNegotiation_found() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(service.findbyId("negotiationId")).thenReturn(contractNegotiation);
        var dto = ContractNegotiationDto.Builder.newInstance().id(contractNegotiation.getId()).build();
        when(transformerRegistry.transform(isA(ContractNegotiation.class), eq(ContractNegotiationDto.class))).thenReturn(Result.success(dto));

        var retrieved = controller.getNegotiation("negotiationId");

        assertThat(retrieved).isNotNull();
    }

    @Test
    void getContractNegotiation_notFound() {
        when(service.findbyId("negotiationId")).thenReturn(null);

        assertThatThrownBy(() -> controller.getNegotiation("nonExistingId"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Object of type ContractNegotiation with ID=nonExistingId was not found");
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void getContractNegotiation_notFoundIfTransformationFails() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(service.findbyId("negotiationId")).thenReturn(contractNegotiation);
        when(transformerRegistry.transform(isA(ContractNegotiation.class), eq(ContractNegotiationDto.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.getNegotiation("nonExistingId"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Object of type ContractNegotiation with ID=nonExistingId was not found");
    }

    @Test
    void getContractNegotiationState_found() {
        when(service.getState("negotiationId")).thenReturn("REQUESTED");

        var state = controller.getNegotiationState("negotiationId");

        assertThat(state.getState()).isEqualTo(REQUESTED.name());
    }

    @Test
    void getContractNegotiationState_notFound() {
        when(service.getState("negotiationId")).thenReturn(null);

        assertThatThrownBy(() -> controller.getNegotiationState("nonExistingId"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Object of type ContractNegotiation with ID=nonExistingId was not found");
    }

    @Test
    void getAgreementForNegotiation() {
        when(service.getForNegotiation("negotiationId")).thenReturn(createContractAgreement("negotiationId"));
        var dto = ContractAgreementDto.Builder.newInstance().id("agreementId").build();
        when(transformerRegistry.transform(isA(ContractAgreement.class), eq(ContractAgreementDto.class))).thenReturn(Result.success(dto));

        var agreement = controller.getAgreementForNegotiation("negotiationId");

        assertThat(agreement).isNotNull()
                .extracting(ContractAgreementDto::getId)
                .isEqualTo("agreementId");
    }

    @Test
    void getAgreementForNegotiation_negotiationNotExist() {
        when(service.getForNegotiation(any())).thenReturn(null);

        assertThatThrownBy(() -> controller.getAgreementForNegotiation("negotiationId"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Object of type ContractNegotiation with ID=negotiationId was not found");
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void initiateNegotiation() {
        when(service.initiateNegotiation(isA(ContractRequest.class))).thenReturn(createContractNegotiation("negotiationId"));
        var contractOfferRequest = createContractOfferRequest();
        when(transformerRegistry.transform(isA(NegotiationInitiateRequestDto.class), eq(ContractRequest.class))).thenReturn(Result.success(contractOfferRequest));
        var request = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("callbackAddress")
                .protocol("protocol")
                .offer(TestFunctions.createOffer("offerId"))
                .callbackAddresses(List.of(CallbackAddressDto.Builder.newInstance()
                        .uri("local://test")
                        .build()))
                .build();

        var negotiationId = controller.initiateContractNegotiation(request);

        assertThat(negotiationId.getId()).isEqualTo("negotiationId");
        assertThat(negotiationId).isInstanceOf(IdResponseDto.class);
        assertThat(negotiationId.getId()).isNotEmpty();
        assertThat(negotiationId.getCreatedAt()).isNotZero();
    }

    @Test
    void initiateNegotiation_illegalArgumentIfTransformationFails() {
        when(service.initiateNegotiation(isA(ContractRequest.class))).thenReturn(createContractNegotiation("negotiationId"));
        var request = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("callbackAddress")
                .protocol("protocol")
                .offer(TestFunctions.createOffer("offerId"))
                .build();
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.initiateContractNegotiation(request)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cancel() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(service.cancel("negotiationId")).thenReturn(ServiceResult.success(contractNegotiation));

        controller.cancelNegotiation("negotiationId");

        verify(service).cancel("negotiationId");
    }

    @Test
    void cancel_notFound() {
        when(service.cancel("negotiationId")).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.cancelNegotiation("negotiationId"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Object of type ContractNegotiation with ID=negotiationId was not found");
    }

    @Test
    void cancel_notPossible() {
        when(service.cancel("negotiationId")).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.cancelNegotiation("negotiationId")).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void decline() {
        var contractNegotiation = createContractNegotiation("negotiationId");
        when(service.decline("negotiationId")).thenReturn(ServiceResult.success(contractNegotiation));

        controller.declineNegotiation("negotiationId");

        verify(service).decline("negotiationId");
    }

    @Test
    void decline_notFound() {
        when(service.decline("negotiationId")).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.declineNegotiation("negotiationId"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Object of type ContractNegotiation with ID=negotiationId was not found");
    }

    @Test
    void decline_notPossible() {
        when(service.decline("negotiationId")).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.declineNegotiation("negotiationId")).isInstanceOf(ObjectConflictException.class);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidNegotiationParameters.class)
    void initiateNegotiation_invalidRequestBody(String connectorAddress, String connectorId, String protocol, String offerId) {
        when(transformerRegistry.transform(isA(NegotiationInitiateRequestDto.class), eq(ContractRequest.class))).thenReturn(Result.failure("error"));

        var rq = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .connectorId(connectorId)
                .protocol(protocol)
                .offer(TestFunctions.createOffer(offerId))
                .build();

        assertThatThrownBy(() -> controller.initiateContractNegotiation(rq)).isInstanceOf(InvalidRequestException.class);
    }

    private ContractNegotiation createContractNegotiation(String negotiationId) {
        return createContractNegotiationBuilder(negotiationId)
                .build();
    }

    private ContractAgreement createContractAgreement(String negotiationId) {
        return ContractAgreement.Builder.newInstance()
                .id(negotiationId)
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private ContractRequest createContractOfferRequest() {
        var requestData = ContractRequestData.Builder.newInstance()
                .protocol("protocol")
                .connectorId("connectorId")
                .callbackAddress("callbackAddress")
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .policy(Policy.Builder.newInstance().build())
                        .assetId("test-asset")
                        .contractStart(ZonedDateTime.now())
                        .contractEnd(ZonedDateTime.now().plusMonths(1))
                        .build())
                .build();
        return ContractRequest.Builder.newInstance()
                .requestData(requestData)
                .build();
    }

    private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol");
    }

    private static class InvalidNegotiationParameters implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(Arguments.of(null, "consumer", "ids-multipart", "test-offer"),
                    Arguments.of("", "consumer", "ids-multipart", "test-offer"),
                    Arguments.of("  ", "consumer", "ids-multipart", "test-offer"),
                    Arguments.of("http://some-connector", null, "ids-multipart", "test-offer"),
                    Arguments.of("http://some-connector", "", "ids-multipart", "test-offer"),
                    Arguments.of("http://some-connector", "  ", "ids-multipart", "test-offer"),
                    Arguments.of("http://some-connector", "consumer", null, "test-offer"),
                    Arguments.of("http://some-connector", "consumer", "", "test-offer"),
                    Arguments.of("http://some-connector", "consumer", "   ", "test-offer"),
                    Arguments.of("http://some-connector", "consumer", "ids-multipart", null),
                    Arguments.of("http://some-connector", "consumer", "ids-multipart", ""),
                    Arguments.of("http://some-connector", "consumer", "ids-multipart", "  ")
            );
        }
    }
}
