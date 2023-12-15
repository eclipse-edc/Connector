/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - initiate provider process
 *
 */

package org.eclipse.edc.connector.service.transferprocess;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNAUTHORIZED;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessProtocolServiceImplTest {

    private final TransferProcessStore store = mock();
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final ContractNegotiationStore negotiationStore = mock();
    private final ContractValidationService validationService = mock();
    private final DataAddressValidatorRegistry dataAddressValidator = mock();
    private final TransferProcessListener listener = mock();

    private final IdentityService identityService = mock();
    private TransferProcessProtocolService service;

    @BeforeEach
    void setUp() {
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        service = new TransferProcessProtocolServiceImpl(store, transactionContext, negotiationStore, validationService, identityService,
                dataAddressValidator, observable, mock(), mock(), mock());
    }

    @Test
    void notifyRequested_validAgreement_shouldInitiateTransfer() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferRequestMessage.Builder.newInstance()
                .processId("transferProcessId")
                .contractId("agreementId")
                .protocol("protocol")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.success());

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded().satisfies(tp -> {
            assertThat(tp.getCorrelationId()).isEqualTo("transferProcessId");
            assertThat(tp.getConnectorAddress()).isEqualTo("http://any");
            assertThat(tp.getAssetId()).isEqualTo("assetId");
        });
        verify(listener).preCreated(any());
        verify(store).save(argThat(t -> t.getState() == INITIAL.code()));
        verify(listener).initiated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyRequested_doNothingIfProcessAlreadyExist() {
        var message = TransferRequestMessage.Builder.newInstance()
                .processId("correlationId")
                .contractId("agreementId")
                .protocol("protocol")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.success());
        when(store.findForCorrelationId(any())).thenReturn(transferProcess(REQUESTED, "transferProcessId"));

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded().extracting(TransferProcess::getId).isEqualTo("transferProcessId");
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidAgreement_shouldNotInitiateTransfer() {
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .callbackAddress("http://any")
                .contractId("agreementId")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.failure("error"));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.success());

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidDestination_shouldNotInitiateTransfer() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .contractId("agreementId")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.failure(violation("invalid data address", "path")));


        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_missingDestination_shouldInitiateTransfer() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferRequestMessage.Builder.newInstance()
                .processId("transferProcessId")
                .protocol("protocol")
                .contractId("agreementId")
                .callbackAddress("http://any")
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded().satisfies(tp -> {
            assertThat(tp.getCorrelationId()).isEqualTo("transferProcessId");
            assertThat(tp.getConnectorAddress()).isEqualTo("http://any");
            assertThat(tp.getAssetId()).isEqualTo("assetId");
            assertThat(tp.getDataDestination().getType()).isEqualTo(HTTP_PROXY);
        });
        verify(listener).preCreated(any());
        verify(store).save(argThat(t -> t.getState() == INITIAL.code()));
        verify(listener).initiated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyStarted_shouldTransitionToStarted() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess(REQUESTED, "transferProcessId")));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());

        var result = service.notifyStarted(message, tokenRepresentation);

        var captor = ArgumentCaptor.forClass(TransferProcessStartedData.class);

        assertThat(result).isSucceeded();
        verify(listener).preStarted(any());
        verify(store).save(argThat(t -> t.getState() == STARTED.code()));
        verify(listener).started(any(), captor.capture());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));

        assertThat(captor.getValue().getDataAddress()).usingRecursiveComparison().isEqualTo(message.getDataAddress());
    }

    @Test
    void notifyStarted_shouldReturnConflict_whenStatusIsNotValid() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var transferProcess = transferProcess(COMPLETED, UUID.randomUUID().toString());
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());

        var result = service.notifyStarted(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        // state didn't change
        verify(store, times(1)).save(argThat(tp -> tp.getState() == COMPLETED.code()));
        verifyNoInteractions(listener);
    }

    @Test
    void notifyStarted_shouldReturnNotFound_whenCounterPartyUnauthorized() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess(REQUESTED, "transferProcessId")));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.failure("error"));

        var result = service.notifyStarted(message, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);

        verify(store, times(1)).save(any());

    }

    @Test
    void notifyCompleted_shouldTransitionToCompleted() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess(STARTED, "transferProcessId")));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());

        var result = service.notifyCompleted(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(listener).preCompleted(any());
        verify(store).save(argThat(t -> t.getState() == COMPLETED.code()));
        verify(listener).completed(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyCompleted_shouldReturnConflict_whenStatusIsNotValid() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var transferProcess = transferProcess(REQUESTED, UUID.randomUUID().toString());
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());

        var result = service.notifyCompleted(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        // state didn't change
        verify(store, times(1)).save(argThat(tp -> tp.getState() == REQUESTED.code()));
        verifyNoInteractions(listener);
    }

    @Test
    void notifyCompleted_shouldReturnNotFound_whenCounterPartyUnauthorized() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();

        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess(STARTED, "transferProcessId")));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.failure("error"));

        var result = service.notifyCompleted(message, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);

        verify(store, times(1)).save(any());

    }

    @Test
    void notifyTerminated_shouldTransitionToTerminated() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess(STARTED, "transferProcessId")));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());
        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(listener).preTerminated(any());
        verify(store).save(argThat(t -> t.getState() == TERMINATED.code()));
        verify(listener).terminated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyTerminated_shouldReturnConflict_whenStatusIsNotValid() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var transferProcess = transferProcess(TERMINATED, UUID.randomUUID().toString());
        var agreement = contractAgreement();
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());

        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        // state didn't change
        verify(store, times(1)).save(argThat(tp -> tp.getState() == TERMINATED.code()));
        verifyNoInteractions(listener);
    }

    @Test
    void notifyTerminated_shouldReturnNotFound_whenCounterPartyUnauthorized() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var agreement = contractAgreement();
        var transferProcess = transferProcess(TERMINATED, UUID.randomUUID().toString());
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.failure("error"));

        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);

        verify(store, times(1)).save(any());

    }

    @Test
    void findById_shouldReturnTransferProcess_whenValidCounterParty() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var processId = "transferProcessId";
        var transferProcess = transferProcess(INITIAL, processId);
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findById(processId)).thenReturn(transferProcess);
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.success());

        var result = service.findById(processId, tokenRepresentation);

        assertThat(result)
                .isSucceeded()
                .isEqualTo(transferProcess);
    }

    @Test
    void findById_shouldReturnNotFound_whenNegotiationNotFound() {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findById(any())).thenReturn(null);

        var result = service.findById("invalidId", tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void findById_shouldReturnNotFound_whenCounterPartyUnauthorized() {
        var processId = "transferProcessId";
        var transferProcess = transferProcess(INITIAL, processId);
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();
        var agreement = contractAgreement();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findById(processId)).thenReturn(transferProcess);
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(claimToken, agreement)).thenReturn(Result.failure("error"));

        var result = service.findById(processId, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }

    @ParameterizedTest
    @ArgumentsSource(NotFoundArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTransferProcessNotFound(MethodCall<M> methodCall, M message) {
        var claimToken = claimToken();
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.success(claimToken));
        when(store.findByCorrelationIdAndLease(any())).thenReturn(StoreResult.notFound("not found"));

        var result = methodCall.call(service, message, tokenRepresentation);

        assertThat(result).matches(ServiceResult::failed);
        verify(store, never()).save(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @ParameterizedTest
    @ArgumentsSource(NotFoundArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTokenValidationFails(MethodCall<M> methodCall, M message) {
        var tokenRepresentation = tokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), isA(VerificationContext.class))).thenReturn(Result.failure("unauthorized"));

        var result = methodCall.call(service, message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    private TransferProcess transferProcess(TransferProcessStates state, String id) {
        return TransferProcess.Builder.newInstance()
                .state(state.code())
                .id(id)
                .dataRequest(dataRequest())
                .build();
    }

    private ClaimToken claimToken() {
        return ClaimToken.Builder.newInstance()
                .claim("key", "value")
                .build();
    }

    private TokenRepresentation tokenRepresentation() {
        return TokenRepresentation.Builder.newInstance()
                .token(UUID.randomUUID().toString())
                .build();
    }

    private ContractAgreement contractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .providerId("provider")
                .consumerId("consumer")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private DataRequest dataRequest() {
        return DataRequest.Builder.newInstance()
                .contractId("contractId")
                .destinationType("type")
                .build();
    }

    @FunctionalInterface
    private interface MethodCall<M extends RemoteMessage> {
        ServiceResult<?> call(TransferProcessProtocolService service, M message, TokenRepresentation token);
    }

    private static class NotFoundArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            MethodCall<TransferStartMessage> started = TransferProcessProtocolService::notifyStarted;
            MethodCall<TransferCompletionMessage> completed = TransferProcessProtocolService::notifyCompleted;
            MethodCall<TransferTerminationMessage> terminated = TransferProcessProtocolService::notifyTerminated;
            return Stream.of(
                    Arguments.of(started, TransferStartMessage.Builder.newInstance().protocol("protocol")
                            .counterPartyAddress("http://any").processId("correlationId").build()),
                    Arguments.of(completed, TransferCompletionMessage.Builder.newInstance().protocol("protocol")
                            .counterPartyAddress("http://any").processId("correlationId").build()),
                    Arguments.of(terminated, TransferTerminationMessage.Builder.newInstance().protocol("protocol")
                            .counterPartyAddress("http://any").processId("correlationId").code("TestCode")
                            .reason("TestReason").build())
            );
        }
    }
}
