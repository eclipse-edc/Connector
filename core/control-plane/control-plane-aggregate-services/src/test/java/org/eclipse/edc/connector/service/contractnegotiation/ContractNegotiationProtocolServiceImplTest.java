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
import org.eclipse.edc.connector.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
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
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.OFFERED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.service.contractnegotiation.ContractNegotiationProtocolServiceImplTest.TestFunctions.contractOffer;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNAUTHORIZED;
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

    private final ContractNegotiationStore store = mock();
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final ContractValidationService validationService = mock();
    private final ContractNegotiationListener listener = mock();
    private final IdentityService identityService = mock();
    private ContractNegotiationProtocolService service;

    @BeforeEach
    void setUp() {
        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);
        service = new ContractNegotiationProtocolServiceImpl(store, transactionContext, validationService, identityService, observable,
                mock(), mock());
    }

    @Test
    void notifyRequested_shouldInitiateNegotiation_whenNegotiationDoesNotExist() {
        var claimToken = ClaimToken.Builder.newInstance().build();
        var tokenRepresentation = tokenRepresentation();
        var contractOffer = contractOffer();
        var validatedOffer = new ValidatedConsumerOffer(CONSUMER_ID, contractOffer);
        var message = ContractRequestMessage.Builder.newInstance()
                .callbackAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .processId("processId")
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease(any())).thenReturn(StoreResult.notFound("not found"));
        when(validationService.validateInitialOffer(claimToken, contractOffer)).thenReturn(Result.success(validatedOffer));

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        var calls = ArgumentCaptor.forClass(ContractNegotiation.class);
        verify(store).save(calls.capture());
        assertThat(calls.getAllValues()).anySatisfy(n -> {
            assertThat(n.getState()).isEqualTo(REQUESTED.code());
            assertThat(n.getCounterPartyAddress()).isEqualTo(message.getCallbackAddress());
            assertThat(n.getProtocol()).isEqualTo(message.getProtocol());
            assertThat(n.getCorrelationId()).isEqualTo(message.getProcessId());
            assertThat(n.getContractOffers()).hasSize(1);
            assertThat(n.getLastContractOffer()).isEqualTo(contractOffer);
        });
        verify(listener).requested(any());
        verify(validationService).validateInitialOffer(claimToken, contractOffer);
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyRequested_shouldTransitionToRequested_whenNegotiationFound() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var contractOffer = contractOffer();
        var validatedOffer = new ValidatedConsumerOffer(CONSUMER_ID, contractOffer);
        var negotiation = createContractNegotiationOffered();
        var message = ContractRequestMessage.Builder.newInstance()
                .callbackAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .processId("processId")
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease(any())).thenReturn(StoreResult.success(negotiation));
        when(validationService.validateInitialOffer(claimToken, contractOffer)).thenReturn(Result.success(validatedOffer));


        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        var calls = ArgumentCaptor.forClass(ContractNegotiation.class);
        verify(store).save(calls.capture());
        assertThat(calls.getAllValues()).anySatisfy(n -> {
            assertThat(n.getState()).isEqualTo(REQUESTED.code());
            assertThat(n.getCounterPartyAddress()).isEqualTo(message.getCallbackAddress());
            assertThat(n.getProtocol()).isEqualTo(message.getProtocol());
            assertThat(n.getCorrelationId()).isEqualTo(message.getProcessId());
            assertThat(n.getContractOffers()).hasSize(2);
            assertThat(n.getLastContractOffer()).isEqualTo(contractOffer);
        });
        verify(listener).requested(any());
        verify(store).findByCorrelationIdAndLease("processId");
        verify(validationService).validateInitialOffer(claimToken, contractOffer);
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyOffered_shouldTransitionToOffered_whenNegotiationFound() {
        var processId = "processId";
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var contractOffer = contractOffer();
        var message = ContractOfferMessage.Builder.newInstance()
                .callbackAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .processId(processId)
                .build();
        var negotiation = createContractNegotiationRequested();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease(processId)).thenReturn(StoreResult.success(negotiation));
        when(validationService.validateRequest(claimToken, negotiation)).thenReturn(Result.success());

        var result = service.notifyOffered(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        var updatedNegotiation = result.getContent();
        assertThat(updatedNegotiation.getContractOffers()).hasSize(2);
        assertThat(updatedNegotiation.getLastContractOffer()).isEqualTo(contractOffer);

        verify(listener).offered(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyAccepted_shouldTransitionToAccepted() {
        var contractNegotiation = createContractNegotiationOffered();
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = ContractNegotiationEventMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .type(ContractNegotiationEventMessage.Type.ACCEPTED)
                .policy(Policy.Builder.newInstance().build())
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("processId")).thenReturn(StoreResult.success(contractNegotiation));
        when(validationService.validateRequest(eq(claimToken), any(ContractNegotiation.class))).thenReturn(Result.success());

        var result = service.notifyAccepted(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(negotiation -> negotiation.getState() == ACCEPTED.code()));
        verify(validationService).validateRequest(claimToken, contractNegotiation);
        verify(listener).accepted(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyAgreed_shouldTransitionToAgreed() {
        var negotiationConsumerRequested = createContractNegotiationRequested();
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        var contractAgreement = mock(ContractAgreement.class);

        var message = ContractAgreementMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .contractAgreement(contractAgreement)
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("processId")).thenReturn(StoreResult.success(negotiationConsumerRequested));
        when(validationService.validateConfirmed(eq(claimToken), eq(contractAgreement), any(ContractOffer.class))).thenReturn(Result.success());

        var result = service.notifyAgreed(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == AGREED.code() &&
                        negotiation.getContractAgreement() == contractAgreement
        ));
        verify(validationService).validateConfirmed(eq(claimToken), eq(contractAgreement), any(ContractOffer.class));
        verify(listener).agreed(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyVerified_shouldTransitionToVerified() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(AGREED.code()).build();
        var message = ContractAgreementVerificationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("processId")).thenReturn(StoreResult.success(negotiation));
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.success());

        var result = service.notifyVerified(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(n -> n.getState() == VERIFIED.code()));
        verify(listener).verified(negotiation);
        verify(validationService).validateRequest(any(), any(ContractNegotiation.class));
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyFinalized_shouldTransitionToFinalized() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(VERIFIED.code()).build();
        var message = ContractNegotiationEventMessage.Builder.newInstance()
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("processId")
                .build();
        var claimToken = ClaimToken.Builder.newInstance().build();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("processId")).thenReturn(StoreResult.success(negotiation));
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.success());

        var result = service.notifyFinalized(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(n -> n.getState() == FINALIZED.code()));
        verify(listener).finalized(negotiation);
        verify(validationService).validateRequest(any(), any(ContractNegotiation.class));
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyTerminated_shouldTransitionToTerminated() {
        var negotiation = contractNegotiationBuilder().id("negotiationId").type(PROVIDER).state(VERIFIED.code()).build();
        var message = ContractNegotiationTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .processId("processId")
                .counterPartyAddress("http://any")
                .rejectionReason("any")
                .build();
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("processId")).thenReturn(StoreResult.success(negotiation));
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.success());

        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
        verify(listener).terminated(negotiation);
        verify(validationService).validateRequest(any(), any(ContractNegotiation.class));
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void findById_shouldReturnNegotiation_whenValidCounterParty() {
        var id = "negotiationId";
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var negotiation = contractNegotiationBuilder().id(id).type(PROVIDER).state(VERIFIED.code()).build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findById(id)).thenReturn(negotiation);
        when(validationService.validateRequest(claimToken, negotiation)).thenReturn(Result.success());

        var result = service.findById(id, tokenRepresentation);

        assertThat(result)
                .isSucceeded()
                .isEqualTo(negotiation);
    }

    @Test
    void findById_shouldReturnNotFound_whenNegotiationNotFound() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findById(any())).thenReturn(null);

        var result = service.findById("invalidId", tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void findById_shouldReturnBadRequest_whenCounterPartyUnauthorized() {
        var id = "negotiationId";
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));

        var negotiation = contractNegotiationBuilder().id(id).type(PROVIDER).state(VERIFIED.code()).build();

        when(store.findById(id)).thenReturn(negotiation);
        when(validationService.validateRequest(claimToken, negotiation)).thenReturn(Result.failure("validation error"));

        var result = service.findById(id, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(BAD_REQUEST);
    }

    @ParameterizedTest
    @ArgumentsSource(NotifyArguments.class)
    <M extends RemoteMessage> void notify_shouldReturnNotFound_whenNotFound(MethodCall<M> methodCall, M message) {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease(any())).thenReturn(StoreResult.notFound("not found"));

        // currently ContractRequestMessage cannot happen on an already existing negotiation
        if (!(message instanceof ContractRequestMessage)) {
            var result = methodCall.call(service, message, tokenRepresentation);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
            verify(store, never()).save(any());
            verifyNoInteractions(listener);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(NotifyArguments.class)
    <M extends RemoteMessage> void notify_shouldReturnBadRequest_whenValidationFails(MethodCall<M> methodCall, M message) {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease(any())).thenReturn(StoreResult.success(createContractNegotiationOffered()));
        when(validationService.validateRequest(any(), any(ContractNegotiation.class))).thenReturn(Result.failure("validation error"));
        when(validationService.validateInitialOffer(any(), any(ContractOffer.class))).thenReturn(Result.failure("error"));
        when(validationService.validateConfirmed(any(), any(), any(ContractOffer.class))).thenReturn(Result.failure("failure"));

        var result = methodCall.call(service, message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @ParameterizedTest
    @ArgumentsSource(NotifyArguments.class)
    <M extends RemoteMessage> void notify_shouldReturnBadRequest_whenTokenValidationFails(MethodCall<M> methodCall, M message) {
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.failure("unauthorized"));

        var result = methodCall.call(service, message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
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

    private ContractNegotiation createContractNegotiationOffered() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(OFFERED.code())
                .type(PROVIDER)
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

    private TokenRepresentation tokenRepresentation() {
        return TokenRepresentation.Builder.newInstance()
                .token(UUID.randomUUID().toString())
                .build();
    }

    @FunctionalInterface
    private interface MethodCall<M extends RemoteMessage> {
        ServiceResult<?> call(ContractNegotiationProtocolService service, M message, TokenRepresentation token);
    }

    private static class NotifyArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            MethodCall<ContractRequestMessage> requested = ContractNegotiationProtocolService::notifyRequested;
            MethodCall<ContractOfferMessage> offered = ContractNegotiationProtocolService::notifyOffered;
            MethodCall<ContractAgreementMessage> agreed = ContractNegotiationProtocolService::notifyAgreed;
            MethodCall<ContractNegotiationEventMessage> accepted = ContractNegotiationProtocolService::notifyAccepted;
            MethodCall<ContractAgreementVerificationMessage> verified = ContractNegotiationProtocolService::notifyVerified;
            MethodCall<ContractNegotiationEventMessage> finalized = ContractNegotiationProtocolService::notifyFinalized;
            MethodCall<ContractNegotiationTerminationMessage> terminated = ContractNegotiationProtocolService::notifyTerminated;
            return Stream.of(
                    Arguments.of(requested, ContractRequestMessage.Builder.newInstance()
                            .counterPartyAddress("callbackAddress")
                            .protocol("protocol")
                            .contractOffer(contractOffer())
                            .processId("processId")
                            .build()),
                    Arguments.of(offered, ContractOfferMessage.Builder.newInstance()
                            .callbackAddress("callbackAddress")
                            .protocol("protocol")
                            .contractOffer(contractOffer())
                            .processId("processId")
                            .build()),
                    Arguments.of(agreed, ContractAgreementMessage.Builder.newInstance()
                            .protocol("protocol")
                            .counterPartyAddress("http://any")
                            .processId("processId")
                            .contractAgreement(mock(ContractAgreement.class))
                            .build()),
                    Arguments.of(accepted, ContractNegotiationEventMessage.Builder.newInstance()
                            .type(ContractNegotiationEventMessage.Type.ACCEPTED)
                            .protocol("protocol")
                            .counterPartyAddress("http://any")
                            .processId("processId")
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

    interface TestFunctions {
        static ContractOffer contractOffer() {
            return ContractOffer.Builder.newInstance()
                    .id(ContractOfferId.create("1", "test-asset-id").toString())
                    .policy(createPolicy())
                    .assetId("assetId")
                    .build();
        }

        private static Policy createPolicy() {
            return Policy.Builder.newInstance().build();
        }
    }
}
