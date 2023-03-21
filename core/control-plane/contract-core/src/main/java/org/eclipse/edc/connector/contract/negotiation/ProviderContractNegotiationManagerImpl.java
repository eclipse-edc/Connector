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
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
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
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_VERIFIED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_AGREEING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_FINALIZING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ProviderContractNegotiationManager {

    private static final String TYPE = "Provider";
    private StateMachineManager stateMachineManager;

    private ProviderContractNegotiationManagerImpl() {
    }

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("provider-contract-negotiation", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(PROVIDER_OFFERING, this::processProviderOffering))
                .processor(processNegotiationsInState(PROVIDER_AGREEING, this::processProviderAgreeing))
                .processor(processNegotiationsInState(CONSUMER_VERIFIED, this::processConsumerVerified))
                .processor(processNegotiationsInState(PROVIDER_FINALIZING, this::processProviderFinalizing))
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
     * Tells this manager that a {@link ContractNegotiation} has been terminated by the counter-party. Transitions the
     * corresponding ContractNegotiation to state TERMINATED.
     *
     * @param token Claim token of the consumer that sent the rejection.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @return a {@link StatusResult}: OK, if successfully transitioned to TERMINATED; FATAL_ERROR, if no match found for correlationId.
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> declined(ClaimToken token, String correlationId) {
        var negotiation = findContractNegotiationById(correlationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR);
        }

        var result = validationService.validateRequest(token, negotiation);
        if (!result.succeeded()) {
            return StatusResult.failure(FATAL_ERROR, "Invalid client credentials");
        }

        monitor.debug("[Provider] Contract rejection received. Abort negotiation process.");

        // Remove agreement if present
        if (negotiation.getContractAgreement() != null) {
            negotiation.setContractAgreement(null);
        }
        transitionToTerminated(negotiation);

        return StatusResult.success(negotiation);
    }

    @Override
    public void enqueueCommand(ContractNegotiationCommand command) {
        commandQueue.enqueue(command);
    }

    /**
     * Initiates a new {@link ContractNegotiation}. The ContractNegotiation is created and persisted, which moves it to
     * state REQUESTED. It is then validated and transitioned to CONFIRMING, PROVIDER_OFFERING or TERMINATING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param request Container object containing all relevant request parameters.
     * @return a {@link StatusResult}: OK
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> requested(ClaimToken token, ContractOfferRequest request) {
        var offer = request.getContractOffer();

        var result = validationService.validateInitialOffer(token, offer);
        if (result.failed()) {
            monitor.debug("[Provider] Contract offer rejected as invalid: " + result.getFailureDetail());
            return StatusResult.failure(FATAL_ERROR);
        }

        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .counterPartyId(result.getContent().getConsumer().toString())
                .counterPartyAddress(request.getConnectorAddress())
                .protocol(request.getProtocol())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(PROVIDER)
                .build();

        transitionToConsumerRequested(negotiation);

        negotiation.addContractOffer(offer);

        monitor.debug("[Provider] Contract offer received. Will be approved.");
        transitionToProviderAgreeing(negotiation);

        return StatusResult.success(negotiation);
    }

    @Override
    public StatusResult<ContractNegotiation> verified(ClaimToken token, String negotiationId) {
        var negotiation = negotiationStore.findById(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR, format("ContractNegotiation with id %s not found", negotiationId));
        }

        var result = validationService.validateRequest(token, negotiation);
        if (!result.succeeded()) {
            return StatusResult.failure(FATAL_ERROR, "Invalid client credentials");
        }

        transitionToConsumerVerified(negotiation);
        return StatusResult.success(negotiation);
    }

    @Override
    protected String getType() {
        return TYPE;
    }

    private ContractNegotiation findContractNegotiationById(String negotiationId) {
        var negotiation = negotiationStore.findById(negotiationId);
        if (negotiation == null) {
            negotiation = negotiationStore.findForCorrelationId(negotiationId);
        }

        return negotiation;
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
     * Processes {@link ContractNegotiation} in state PROVIDER_OFFERING. Tries to send the current offer to the
     * respective consumer. If this succeeds, the ContractNegotiation is transitioned to state PROVIDER_OFFERED. Else,
     * it is transitioned to PROVIDER_OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processProviderOffering(ContractNegotiation negotiation) {
        var currentOffer = negotiation.getLastContractOffer();

        var contractOfferRequest = ContractOfferRequest.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractOffer(currentOffer)
                .correlationId(negotiation.getCorrelationId())
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, contractOfferRequest))
                .entityRetrieve(negotiationStore::findById)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitionToProviderOffered(n))
                .onFailure((n, throwable) -> transitionToProviderOffering(n))
                .onRetryExhausted((n, throwable) -> transitionToTerminating(n, format("Failed to send %s to consumer: %s", contractOfferRequest.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[Provider] send counter offer");
    }

    /**
     * Processes {@link ContractNegotiation} in state TERMINATING. Tries to send a contract rejection to the respective
     * consumer. If this succeeds, the ContractNegotiation is transitioned to state TERMINATED. Else, it is transitioned
     * to TERMINATING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processTerminating(ContractNegotiation negotiation) {
        var rejection = ContractRejection.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .correlationId(negotiation.getCorrelationId())
                .rejectionReason(negotiation.getErrorDetail())
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, rejection))
                .entityRetrieve(negotiationStore::findById)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitionToTerminated(n))
                .onFailure((n, throwable) -> transitionToTerminating(n))
                .onRetryExhausted((n, throwable) -> transitionToTerminated(n, format("Failed to send %s to consumer: %s", rejection.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[Provider] send rejection");
    }

    /**
     * Processes {@link ContractNegotiation} in state CONFIRMING. Tries to send a contract agreement to the respective
     * consumer. If this succeeds, the ContractNegotiation is transitioned to state CONFIRMED. Else, it is transitioned
     * to CONFIRMING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processProviderAgreeing(ContractNegotiation negotiation) {
        var retrievedAgreement = negotiation.getContractAgreement();

        ContractAgreement agreement;
        Policy policy;
        if (retrievedAgreement == null) {
            var lastOffer = negotiation.getLastContractOffer();

            var contractId = ContractId.parse(lastOffer.getId());
            if (!contractId.isValid()) {
                monitor.severe("ProviderContractNegotiationManagerImpl.checkConfirming(): Offer Id not correctly formatted.");
                return false;
            }
            var definitionId = contractId.definitionPart();

            policy = lastOffer.getPolicy();

            agreement = ContractAgreement.Builder.newInstance()
                    .id(ContractId.createContractId(definitionId))
                    .contractStartDate(clock.instant().getEpochSecond())
                    .contractEndDate(lastOffer.getContractEnd().toEpochSecond())
                    .contractSigningDate(clock.instant().getEpochSecond())
                    .providerAgentId(String.valueOf(lastOffer.getProvider()))
                    .consumerAgentId(String.valueOf(lastOffer.getConsumer()))
                    .policy(policy)
                    .assetId(lastOffer.getAsset().getId())
                    .build();
        } else {
            agreement = retrievedAgreement;
            policy = agreement.getPolicy();
        }

        var request = ContractAgreementRequest.Builder.newInstance() // TODO: should be renamed to ContractAgreementMessage
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractAgreement(agreement)
                .correlationId(negotiation.getCorrelationId())
                .policy(policy)
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, request))
                .entityRetrieve(negotiationStore::findById)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitionToProviderAgreed(n, agreement))
                .onFailure((n, throwable) -> transitionToProviderAgreeing(n))
                .onRetryExhausted((n, throwable) -> transitionToTerminating(n, format("Failed to send %s to consumer: %s", request.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[Provider] send agreement");
    }

    /**
     * Processes {@link ContractNegotiation} in state PROVIDER_AGREED. For the deprecated ids-protocol, it's transitioned
     * to PROVIDER_FINALIZED, otherwise to CONSUMER_VERIFYING to make the verification process start.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processConsumerVerified(ContractNegotiation negotiation) {
        transitionToProviderFinalizing(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state PROVIDER_OFFERING. Tries to send the current offer to the
     * respective consumer. If this succeeds, the ContractNegotiation is transitioned to state PROVIDER_OFFERED. Else,
     * it is transitioned to PROVIDER_OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processProviderFinalizing(ContractNegotiation negotiation) {
        var message = ContractNegotiationEventMessage.Builder.newInstance()
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .protocol(negotiation.getProtocol())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .correlationId(negotiation.getCorrelationId())
                .build();

        return entityRetryProcessFactory.doAsyncProcess(negotiation, () -> dispatcherRegistry.send(Object.class, message))
                .entityRetrieve(negotiationStore::findById)
                .onDelay(this::breakLease)
                .onSuccess((n, result) -> transitionToProviderFinalized(n))
                .onFailure((n, throwable) -> transitionToProviderFinalizing(n))
                .onRetryExhausted((n, throwable) -> transitionToTerminating(n, format("Failed to send %s to consumer: %s", message.getClass().getSimpleName(), throwable.getMessage())))
                .execute("[Provider] send finalization");
    }

    /**
     * Builder for ProviderContractNegotiationManagerImpl.
     */
    public static class Builder extends AbstractContractNegotiationManager.Builder<ProviderContractNegotiationManagerImpl> {


        private Builder() {
            super(new ProviderContractNegotiationManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }
    }
}
