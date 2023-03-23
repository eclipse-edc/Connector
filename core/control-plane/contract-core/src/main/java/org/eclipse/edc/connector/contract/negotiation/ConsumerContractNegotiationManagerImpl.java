/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - extended method implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - refactor
 *
 */

package org.eclipse.edc.connector.contract.negotiation;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRejection;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.StateProcessorImpl;

import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_AGREEING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_REQUESTING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_VERIFYING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of the {@link ConsumerContractNegotiationManager}.
 */
public class ConsumerContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ConsumerContractNegotiationManager {

    private static final String TYPE = "Consumer";
    private StateMachineManager stateMachineManager;

    private ConsumerContractNegotiationManagerImpl() {
    }

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("consumer-contract-negotiation", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(INITIAL, this::processInitial))
                .processor(processNegotiationsInState(CONSUMER_REQUESTING, this::processRequesting))
                .processor(processNegotiationsInState(CONSUMER_AGREEING, this::processConsumerApproving))
                .processor(processNegotiationsInState(PROVIDER_AGREED, this::processProviderAgreed))
                .processor(processNegotiationsInState(CONSUMER_VERIFYING, this::processConsumerVerifying))
                .processor(processNegotiationsInState(TERMINATING, this::processTerminating))
                .processor(onCommands(this::processCommand))
                .build();

        stateMachineManager.start();
    }

    public void stop() {
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    /**
     * Initiates a new {@link ContractNegotiation}. The ContractNegotiation is created and persisted, which moves it to
     * state REQUESTING.
     *
     * @param contractOffer Container object containing all relevant request parameters.
     * @return a {@link StatusResult}: OK
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> initiate(ContractOfferRequest contractOffer) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol(contractOffer.getProtocol())
                .counterPartyId(contractOffer.getConnectorId())
                .counterPartyAddress(contractOffer.getConnectorAddress())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(CONSUMER)
                .build();

        negotiation.addContractOffer(contractOffer.getContractOffer());
        transitToInitial(negotiation);

        return StatusResult.success(negotiation);
    }

    /**
     * Tells this manager that a previously sent contract offer has been agreed by the provider. Validates the
     * contract agreement sent by the provider against the last contract offer and transitions the corresponding
     * {@link ContractNegotiation} to state AGREED or TERMINATING.
     *
     * @param token         Claim token of the consumer that send the contract request.
     * @param negotiationId Id of the ContractNegotiation.
     * @param agreement     Agreement sent by provider.
     * @param policy        the policy
     * @return a {@link StatusResult}: FATAL_ERROR, if no match found for Id or no last offer found for negotiation; OK otherwise
     */
    @WithSpan
    @Override
    // TODO: should be renamed to agreed
    public StatusResult<ContractNegotiation> confirmed(ClaimToken token, String negotiationId, ContractAgreement agreement, Policy policy) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR, format("ContractNegotiation with id %s not found", negotiationId));
        }

        var latestOffer = negotiation.getLastContractOffer();
        if (latestOffer == null) {
            monitor.severe("[Consumer] No offer found for validation. Process id: " + negotiation.getId());
            return StatusResult.failure(FATAL_ERROR);
        }

        var result = validationService.validateConfirmed(agreement, latestOffer);
        if (result.failed()) {
            var message = "Contract agreement received. Validation failed: " + result.getFailureDetail();
            monitor.debug("[Consumer] " + message);
            transitToTerminating(negotiation, message);
            return StatusResult.success(negotiation);
        }

        monitor.debug("[Consumer] Contract agreement received. Validation successful.");
        transitToProviderAgreed(negotiation, agreement);

        return StatusResult.success(negotiation);
    }

    @Override
    public StatusResult<ContractNegotiation> finalized(String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR, format("ContractNegotiation with id %s not found", negotiationId));
        }

        transitToFinalized(negotiation);
        return StatusResult.success(negotiation);
    }

    /**
     * Tells this manager that a {@link ContractNegotiation} has been declined by the counter-party. Transitions the
     * corresponding ContractNegotiation to state DECLINED.
     *
     * @param token         Claim token of the consumer that sent the rejection.
     * @param negotiationId Id of the ContractNegotiation.
     * @return a {@link StatusResult}: OK, if successfully transitioned to declined; FATAL_ERROR, if no match found for id.
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> declined(ClaimToken token, String negotiationId) {
        var negotiation = findContractNegotiationById(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR);
        }

        monitor.debug("[Consumer] Contract rejection received. Abort negotiation process.");
        transitToTerminated(negotiation);
        return StatusResult.success(negotiation);
    }

    @Override
    public void enqueueCommand(ContractNegotiationCommand command) {
        commandQueue.enqueue(command);
    }

    @Override
    protected String getType() {
        return TYPE;
    }

    private ContractNegotiation findContractNegotiationById(String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            negotiation = negotiationStore.findForCorrelationId(negotiationId);
        }

        return negotiation;
    }

    /**
     * Processes {@link ContractNegotiation} in state INITIAL. Transition ContractNegotiation to REQUESTING.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processInitial(ContractNegotiation negotiation) {
        transitToRequesting(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state REQUESTING. Tries to send the current offer to the respective
     * provider. If this succeeds, the ContractNegotiation is transitioned to state REQUESTED. Else, it is transitioned
     * to REQUESTING for a retry.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processRequesting(ContractNegotiation negotiation) {
        var offer = negotiation.getLastContractOffer();
        var request = ContractOfferRequest.Builder.newInstance() // TODO: should be renamed to ContractRequestMessage
                .contractOffer(offer)
                .connectorAddress(negotiation.getCounterPartyAddress())
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .correlationId(negotiation.getId())
                .type(ContractOfferRequest.Type.INITIAL)
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, request))
                .entityRetrieve(negotiationStore::find)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitToRequested(n))
                .onFailure((n, throwable) -> transitToRequesting(n))
                .onRetryExhausted((n, throwable) -> transitToTerminating(n, format("Failed to send %s to provider: %s", request.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[Consumer] Send ContractRequestMessage message");
    }

    /**
     * Processes {@link ContractNegotiation} in state CONSUMER_APPROVING. Tries to send a dummy contract agreement to
     * the respective provider in order to approve the last offer sent by the provider. If this succeeds, the
     * ContractNegotiation is transitioned to state CONSUMER_APPROVED. Else, it is transitioned to CONSUMER_APPROVING
     * for a retry.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processConsumerApproving(ContractNegotiation negotiation) {
        var lastOffer = negotiation.getLastContractOffer();

        var contractId = ContractId.parse(lastOffer.getId());
        if (!contractId.isValid()) {
            monitor.severe("ConsumerContractNegotiationManagerImpl.approveContractOffers(): Offer Id not correctly formatted.");
            return false;
        }
        var definitionId = contractId.definitionPart();

        var policy = lastOffer.getPolicy();
        var agreement = ContractAgreement.Builder.newInstance()
                .id(ContractId.createContractId(definitionId))
                .contractStartDate(clock.instant().getEpochSecond())
                .contractEndDate(lastOffer.getContractEnd().toEpochSecond())
                .contractSigningDate(clock.instant().getEpochSecond())
                .providerAgentId(String.valueOf(lastOffer.getProvider()))
                .consumerAgentId(String.valueOf(lastOffer.getConsumer()))
                .policy(policy)
                .assetId(lastOffer.getAsset().getId())
                .build();

        var request = ContractAgreementRequest.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractAgreement(agreement)
                .correlationId(negotiation.getId())
                .policy(policy)
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, request))
                .entityRetrieve(negotiationStore::find)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitToApproved(n))
                .onFailure((n, throwable) -> transitToApproving(n))
                .onRetryExhausted((n, throwable) -> transitToTerminating(n, format("Failed to send %s to provider: %s", request.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[consumer] send agreement");
    }

    /**
     * Processes {@link ContractNegotiation} in state PROVIDER_AGREED. For the deprecated ids-protocol, it's transitioned
     * to PROVIDER_FINALIZED, otherwise to CONSUMER_VERIFYING to make the verification process start.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processProviderAgreed(ContractNegotiation negotiation) {
        if ("ids-multipart".equals(negotiation.getProtocol())) {
            transitToFinalized(negotiation);
            return true;
        }

        transitToVerifying(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state CONSUMER_VERIFYING. Verifies the Agreement and send the
     * {@link ContractAgreementVerificationMessage} to the provider and transition the negotiation to the CONSUMER_VERIFIED
     * state.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processConsumerVerifying(ContractNegotiation negotiation) {
        var message = ContractAgreementVerificationMessage.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .correlationId(negotiation.getId())
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, message))
                .entityRetrieve(negotiationStore::find)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitToVerified(n))
                .onFailure((n, throwable) -> transitToVerifying(n))
                .onRetryExhausted((n, throwable) -> transitToTerminating(n, format("Failed to send %s to provider: %s", message.getClass().getSimpleName(), throwable.getMessage())))
                .execute(format("[consumer] send %s", message.getClass().getSimpleName()));
    }

    /**
     * Processes {@link ContractNegotiation} in state TERMINATING. Tries to send a contract rejection to the respective
     * provider. If this succeeds, the ContractNegotiation is transitioned to state TERMINATED. Else, it is transitioned
     * to TERMINATING for a retry.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processTerminating(ContractNegotiation negotiation) {
        var rejection = ContractRejection.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .correlationId(negotiation.getId())
                .rejectionReason(negotiation.getErrorDetail())
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, rejection))
                .entityRetrieve(negotiationStore::find)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitToTerminated(n))
                .onFailure((n, throwable) -> transitToTerminating(n))
                .onRetryExhausted((n, throwable) -> transitToTerminated(n, format("Failed to send %s to provider: %s", rejection.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[Consumer] send rejection");
    }

    private StateProcessorImpl<ContractNegotiation> processNegotiationsInState(ContractNegotiationStates state, Function<ContractNegotiation, Boolean> function) {
        return new StateProcessorImpl<>(() -> negotiationStore.nextForState(state.code(), batchSize), telemetry.contextPropagationMiddleware(function));
    }

    private StateProcessorImpl<ContractNegotiationCommand> onCommands(Function<ContractNegotiationCommand, Boolean> process) {
        return new StateProcessorImpl<>(() -> commandQueue.dequeue(5), process);
    }

    private boolean processCommand(ContractNegotiationCommand command) {
        return commandProcessor.processCommandQueue(command);
    }

    /**
     * Builder for ConsumerContractNegotiationManagerImpl.
     */
    public static class Builder extends AbstractContractNegotiationManager.Builder<ConsumerContractNegotiationManagerImpl> {


        private Builder() {
            super(new ConsumerContractNegotiationManagerImpl());
        }

        public static ConsumerContractNegotiationManagerImpl.Builder newInstance() {
            return new Builder();
        }

    }
}
