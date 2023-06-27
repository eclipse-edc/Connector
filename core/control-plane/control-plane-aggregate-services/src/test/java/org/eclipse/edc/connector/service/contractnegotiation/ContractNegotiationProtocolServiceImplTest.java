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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.service.contractnegotiation;

import org.eclipse.edc.connector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractNegotiationProtocolServiceImplTest {

    private static final String CONSUMER_ID = "consumer";
    private static final String PROVIDER_ID = "provider";

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final ContractNegotiationListener listener = mock(ContractNegotiationListener.class);
    private ContractNegotiationProtocolService service;

    @BeforeEach
    void setUp() {
        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);
        service = new ContractNegotiationProtocolServiceImpl(store,
                transactionContext, validationService, observable, mock(Monitor.class), mock(Telemetry.class));
    }

    @Test
    void notifyRequested_shouldInitiateNegotiation_whenOfferIsValid() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        var validatedOffer = new ValidatedConsumerOffer(CONSUMER_ID, contractOffer);
        when(validationService.validateInitialOffer(token, contractOffer)).thenReturn(Result.success(validatedOffer));
        var message = ContractRequestMessage.Builder.newInstance()
                .connectorId(CONSUMER_ID)
                .callbackAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .processId("processId")
                .build();

        var result = service.notifyRequested(message, token);

        assertThat(result).isSucceeded();
        var calls = ArgumentCaptor.forClass(ContractNegotiation.class);
        verify(store).save(calls.capture());
        assertThat(calls.getAllValues()).anySatisfy(n -> {
            assertThat(n.getState()).isEqualTo(REQUESTED.code());
            assertThat(n.getCounterPartyId()).isEqualTo(message.getConnectorId());
            assertThat(n.getCounterPartyAddress()).isEqualTo(message.getCallbackAddress());
            assertThat(n.getProtocol()).isEqualTo(message.getProtocol());
            assertThat(n.getCorrelationId()).isEqualTo(message.getProcessId());
            assertThat(n.getContractOffers()).hasSize(1);
            assertThat(n.getLastContractOffer()).isEqualTo(contractOffer);
        });
        verify(listener).requested(any());
        verify(validationService).validateInitialOffer(token, contractOffer);
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyRequested_shouldReturnBadRequest_whenOfferNotValid() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validateInitialOffer(token, contractOffer)).thenReturn(Result.failure("error"));

        var message = ContractRequestMessage.Builder.newInstance()
                .connectorId(CONSUMER_ID)
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .processId("processId")
                .build();

        var result = service.notifyRequested(message, token);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(validationService).validateInitialOffer(token, contractOffer);
    }

    @Test
    void notifyAgreed_shouldTransitionToAgreed() {
        var negotiationConsumerRequested = createContractNegotiationRequested();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        when(store.findForCorrelationId("processId")).thenReturn(negotiationConsumerRequested);
        when(validationService.validateConfirmed(eq(token), eq(contractAgreement), any(ContractOffer.class))).thenReturn(Result.success());
        var message = ContractAgreementMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .contractAgreement(contractAgreement)
                .build();

        var result = service.notifyAgreed(message, token);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == AGREED.code() &&
                        negotiation.getContractAgreement() == contractAgreement
        ));
        verify(validationService).validateConfirmed(eq(token), eq(contractAgreement), any(ContractOffer.class));
        verify(listener).agreed(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyAgreed_shouldReturnBadRequest_whenValidationFails() {
        var negotiationConsumerRequested = createContractNegotiationRequested();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        when(store.findForCorrelationId("processId")).thenReturn(negotiationConsumerRequested);
        when(validationService.validateConfirmed(eq(token), eq(contractAgreement), any(ContractOffer.class))).thenReturn(Result.failure("failure"));
        var message = ContractAgreementMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .contractAgreement(contractAgreement)
                .build();

        var result = service.notifyAgreed(message, token);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
        verify(validationService).validateConfirmed(eq(token), eq(contractAgreement), any(ContractOffer.class));
    }

    @Test
    void notifyVerified_shouldTransitionToVerified() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(AGREED.code()).build();
        when(store.findForCorrelationId("processId")).thenReturn(negotiation);
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.success());
        var message = ContractAgreementVerificationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .build();

        var result = service.notifyVerified(message, claimToken());

        assertThat(result).isSucceeded();
        verify(store).save(argThat(n -> n.getState() == VERIFIED.code()));
        verify(listener).verified(negotiation);
        verify(validationService).validateRequest(any(), any(ContractNegotiation.class));
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyVerified_shouldReturnBadRequest_whenValidationFails() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(AGREED.code()).build();
        when(store.findForCorrelationId("processId")).thenReturn(negotiation);
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.failure("validation error"));
        var message = ContractAgreementVerificationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .build();

        var result = service.notifyVerified(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyFinalized_shouldTransitionToFinalized() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(VERIFIED.code()).build();
        when(store.findForCorrelationId("processId")).thenReturn(negotiation);
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.success());
        var message = ContractNegotiationEventMessage.Builder.newInstance()
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .build();
        var token = ClaimToken.Builder.newInstance().build();

        var result = service.notifyFinalized(message, token);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(n -> n.getState() == FINALIZED.code()));
        verify(listener).finalized(negotiation);
        verify(validationService).validateRequest(any(), any(ContractNegotiation.class));
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyFinalized_shouldReturnBadRequest_whenValidationFails() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(VERIFIED.code()).build();
        when(store.findForCorrelationId("processId")).thenReturn(negotiation);
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.failure("validation error"));
        var message = ContractNegotiationEventMessage.Builder.newInstance()
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .build();
        var token = ClaimToken.Builder.newInstance().build();

        var result = service.notifyFinalized(message, token);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyTerminated_shouldTransitionToTerminated() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(VERIFIED.code()).build();
        when(store.findForCorrelationId("processId")).thenReturn(negotiation);
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.success());
        var message = ContractNegotiationTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .processId("processId")
                .counterPartyAddress("http://any")
                .rejectionReason("any")
                .build();
        var token = ClaimToken.Builder.newInstance().build();

        var result = service.notifyTerminated(message, token);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
        verify(listener).terminated(negotiation);
        verify(validationService).validateRequest(any(), any(ContractNegotiation.class));
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyTerminated_shouldReturnBadRequest_whenValidationFails() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(VERIFIED.code()).build();
        when(store.findForCorrelationId("processId")).thenReturn(negotiation);
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.failure("validation error"));
        var message = ContractNegotiationTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .processId("processId")
                .counterPartyAddress("http://any")
                .rejectionReason("any")
                .build();
        var token = ClaimToken.Builder.newInstance().build();

        var result = service.notifyTerminated(message, token);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }
    
    @Test
    void findById_shouldReturnNegotiation_whenValidCounterParty() {
        var id = "negotiationId";
        var token = ClaimToken.Builder.newInstance().build();
        var negotiation = contractNegotiationBuilder().id(id).type(PROVIDER).state(VERIFIED.code()).build();
        
        when(store.findById(id)).thenReturn(negotiation);
        when(validationService.validateRequest(token, negotiation)).thenReturn(Result.success());
        
        var result = service.findById(id, token);
        
        assertThat(result)
                .isSucceeded()
                .isEqualTo(negotiation);
    }
    
    @Test
    void findById_shouldReturnNotFound_whenNegotiationNotFound() {
        when(store.findById(any())).thenReturn(null);
    
        var result = service.findById("invalidId", ClaimToken.Builder.newInstance().build());
        
        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }
    
    @Test
    void findById_shouldReturnNotFound_whenCounterPartyUnauthorized() {
        var id = "negotiationId";
        var token = ClaimToken.Builder.newInstance().build();
        var negotiation = contractNegotiationBuilder().id(id).type(PROVIDER).state(VERIFIED.code()).build();
    
        when(store.findById(id)).thenReturn(negotiation);
        when(validationService.validateRequest(token, negotiation)).thenReturn(Result.failure("validation error"));
    
        var result = service.findById(id, token);
        
        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }

    @ParameterizedTest
    @ArgumentsSource(NotFoundArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTransferProcessNotFound(MethodCall<M> methodCall, M message) {
        when(store.findById(any())).thenReturn(null);

        var result = methodCall.call(service, message, claimToken());

        assertThat(result).matches(ServiceResult::failed);
        verify(store, never()).save(any());
    }

    private ClaimToken claimToken() {
        return ClaimToken.Builder.newInstance()
                .claim("key", "value")
                .build();
    }

    private ContractNegotiation createContractNegotiationRequested() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(REQUESTED.code())
                .contractOffer(lastOffer)
                .build();
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId("processId")
                .counterPartyId("connectorId")
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private Policy createPolicy() {
        return Policy.Builder.newInstance().build();
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(ContractId.createContractId("1", "test-asset-id"))
                .policy(createPolicy())
                .assetId("assetId")
                .providerId(PROVIDER_ID)
                .build();
    }

    @FunctionalInterface
    private interface MethodCall<M extends RemoteMessage> {
        ServiceResult<?> call(ContractNegotiationProtocolService service, M message, ClaimToken token);
    }

    private static class NotFoundArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            MethodCall<ContractAgreementMessage> agreed = ContractNegotiationProtocolService::notifyAgreed;
            MethodCall<ContractAgreementVerificationMessage> verified = ContractNegotiationProtocolService::notifyVerified;
            MethodCall<ContractNegotiationEventMessage> finalized = ContractNegotiationProtocolService::notifyFinalized;
            MethodCall<ContractNegotiationTerminationMessage> terminated = ContractNegotiationProtocolService::notifyTerminated;
            return Stream.of(
                    Arguments.of(agreed, ContractAgreementMessage.Builder.newInstance()
                            .protocol("protocol")
                            .counterPartyAddress("http://any")
                            .processId("processId")
                            .contractAgreement(mock(ContractAgreement.class))
                            .build()),
                    Arguments.of(verified, ContractAgreementVerificationMessage.Builder.newInstance()
                            .protocol("protocol")
                            .counterPartyAddress("http://any")
                            .processId("processId")
                            .build()),
                    Arguments.of(finalized, ContractNegotiationEventMessage.Builder.newInstance()
                            .type(ContractNegotiationEventMessage.Type.FINALIZED)
                            .protocol("protocol")
                            .counterPartyAddress("http://any")
                            .processId("processId")
                            .build()),
                    Arguments.of(terminated, ContractNegotiationTerminationMessage.Builder.newInstance()
                            .protocol("protocol")
                            .processId("processId")
                            .counterPartyAddress("http://any")
                            .rejectionReason("any")
                            .build())
            );
        }
    }
}
