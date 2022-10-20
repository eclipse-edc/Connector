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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRejection;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.StateProcessorImpl;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_APPROVING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_OFFERING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of the {@link ConsumerContractNegotiationManager}.
 */
public class ConsumerContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ConsumerContractNegotiationManager {

    private StateMachineManager stateMachineManager;

    private ConsumerContractNegotiationManagerImpl() {
    }

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("consumer-contract-negotiation", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(INITIAL, this::processInitial))
                .processor(processNegotiationsInState(REQUESTING, this::processRequesting))
                .processor(processNegotiationsInState(CONSUMER_OFFERING, this::processConsumerOffering))
                .processor(processNegotiationsInState(CONSUMER_APPROVING, this::processConsumerApproving))
                .processor(processNegotiationsInState(DECLINING, this::processDeclining))
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
        negotiation.transitionInitial();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.initiated(negotiation));

        monitor.debug(String.format("[Consumer] ContractNegotiation initiated. %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return StatusResult.success(negotiation);
    }

    /**
     * Tells this manager that a new contract offer has been received for a {@link ContractNegotiation}. Validates the
     * offer against the last contract offer for that ContractNegotiation and transitions the ContractNegotiation to
     * CONSUMER_APPROVING, CONSUMER_OFFERING or DECLINING.
     *
     * @param token         Claim token of the consumer that send the contract request.
     * @param negotiationId Id of the ContractNegotiation.
     * @param contractOffer The contract offer.
     * @param hash          A hash of all previous contract offers.
     * @return a {@link StatusResult}: FATAL_ERROR, if no match found for Id or no last offer found for negotiation; OK otherwise
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR);
        }

        var latestOffer = negotiation.getLastContractOffer();
        try {
            Objects.requireNonNull(latestOffer, "latestOffer");
        } catch (NullPointerException e) {
            monitor.severe("[Consumer] No offer found for validation. Process id: " + negotiation.getId());
            return StatusResult.failure(FATAL_ERROR);
        }

        Result<ContractOffer> result = validationService.validate(token, contractOffer, latestOffer);
        negotiation.addContractOffer(contractOffer); // TODO persist unchecked offer of provider?
        if (result.failed()) {
            monitor.debug("[Consumer] Contract offer received. Will be rejected: " + result.getFailureDetail());
            negotiation.setErrorDetail(result.getFailureMessages().get(0));
            negotiation.transitionDeclining();
            negotiationStore.save(negotiation);
        } else {
            // Offer has been approved.
            monitor.debug("[Consumer] Contract offer received. Will be approved.");
            negotiation.transitionApproving();
            negotiationStore.save(negotiation);
        }

        monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return StatusResult.success(negotiation);
    }

    /**
     * Tells this manager that a previously sent contract offer has been confirmed by the provider. Validates the
     * contract agreement sent by the provider against the last contract offer and transitions the corresponding
     * {@link ContractNegotiation} to state CONFIRMED or DECLINING.
     *
     * @param token         Claim token of the consumer that send the contract request.
     * @param negotiationId Id of the ContractNegotiation.
     * @param agreement     Agreement sent by provider.
     * @param policy        the policy
     * @return a {@link StatusResult}: FATAL_ERROR, if no match found for Id or no last offer found for negotiation; OK otherwise
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> confirmed(ClaimToken token, String negotiationId, ContractAgreement agreement, Policy policy) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR, format("ContractNegotiation with id %s not found", negotiationId));
        }

        var latestOffer = negotiation.getLastContractOffer();
        try {
            Objects.requireNonNull(latestOffer, "latestOffer");
        } catch (NullPointerException e) {
            monitor.severe("[Consumer] No offer found for validation. Process id: " + negotiation.getId());
            return StatusResult.failure(FATAL_ERROR);
        }

        var result = validationService.validateConfirmed(agreement, latestOffer);
        if (result.failed()) {
            // TODO Add contract offer possibility.
            var message = "Contract agreement received. Validation failed: " + result.getFailureDetail();
            monitor.debug("[Consumer] " + message);
            negotiation.setErrorDetail(message);
            negotiation.transitionDeclining();
            negotiationStore.save(negotiation);
            monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return StatusResult.success(negotiation);
        }

        // Agreement has been approved.
        negotiation.setContractAgreement(agreement); // TODO persist unchecked agreement of provider?
        monitor.debug("[Consumer] Contract agreement received. Validation successful.");
        if (negotiation.getState() != CONFIRMED.code()) {
            // TODO: otherwise will fail. But should do it, since it's already confirmed? A duplicated message received shouldn't be an issue
            negotiation.transitionConfirmed();
        }
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.confirmed(negotiation));
        monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

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
        negotiation.transitionDeclined();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.declined(negotiation));
        monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return StatusResult.success(negotiation);
    }

    @Override
    public void enqueueCommand(ContractNegotiationCommand command) {
        commandQueue.enqueue(command);
    }

    @Override
    protected String getName() {
        return CONSUMER.name();
    }

    private ContractNegotiation findContractNegotiationById(String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            negotiation = negotiationStore.findForCorrelationId(negotiationId);
        }

        return negotiation;
    }

    /**
     * Builds and sends a {@link ContractOfferRequest} for a given {@link ContractNegotiation} and
     * {@link ContractOffer}.
     *
     * @param offer   The contract offer.
     * @param process The contract negotiation.
     * @return The response to the sent message.
     */
    private CompletableFuture<Object> sendOffer(ContractOffer offer, ContractNegotiation process, ContractOfferRequest.Type type) {
        var request = ContractOfferRequest.Builder.newInstance()
                .contractOffer(offer)
                .connectorAddress(process.getCounterPartyAddress())
                .protocol(process.getProtocol())
                .connectorId(process.getCounterPartyId())
                .correlationId(process.getId())
                .type(type)
                .build();

        // TODO protocol-independent response type?
        return dispatcherRegistry.send(Object.class, request, process::getId);
    }

    /**
     * Processes {@link ContractNegotiation} in state INITIAL. Transition ContractNegotiation to REQUESTING.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processInitial(ContractNegotiation negotiation) {
        negotiation.transitionRequesting();
        negotiationStore.save(negotiation);
        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state REQUESTING. Tries to send the current offer to the respective
     * provider. If this succeeds, the ContractNegotiation is transitioned to state REQUESTED. Else, it is transitioned
     * to INITIAL for a retry.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processRequesting(ContractNegotiation negotiation) {
        if (sendRetryManager.shouldDelay(negotiation)) {
            breakLease(negotiation);
            return false;
        }

        var offer = negotiation.getLastContractOffer();
        sendOffer(offer, negotiation, ContractOfferRequest.Type.INITIAL)
                .whenComplete(onInitialOfferSent(negotiation.getId()));

        return true;
    }

    /**
     * Processes {@link ContractNegotiation} in state CONSUMER_OFFERING. Tries to send the current offer to the
     * respective provider. If this succeeds, the ContractNegotiation is transitioned to state CONSUMER_OFFERED. Else,
     * it is transitioned to CONSUMER_OFFERING for a retry.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processConsumerOffering(ContractNegotiation negotiation) {
        if (sendRetryManager.shouldDelay(negotiation)) {
            breakLease(negotiation);
            return false;
        }

        var offer = negotiation.getLastContractOffer();
        sendOffer(offer, negotiation, ContractOfferRequest.Type.COUNTER_OFFER)
                .whenComplete(onCounterOfferSent(negotiation.getId()));

        return true;
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
        if (sendRetryManager.shouldDelay(negotiation)) {
            breakLease(negotiation);
            return false;
        }

        //TODO this is a dummy agreement used to approve the provider's offer, real agreement will be created and sent by provider
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
                .contractEndDate(clock.instant().plus(365, ChronoUnit.DAYS).getEpochSecond()) // TODO Make configurable (issue #722)
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

        // TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, request, negotiation::getId)
                .whenComplete(onAgreementSent(negotiation.getId()));

        return false;
    }

    /**
     * Processes {@link ContractNegotiation} in state DECLINING. Tries to send a contract rejection to the respective
     * provider. If this succeeds, the ContractNegotiation is transitioned to state DECLINED. Else, it is transitioned
     * to DECLINING for a retry.
     *
     * @return true if processed, false otherwise
     */
    @WithSpan
    private boolean processDeclining(ContractNegotiation negotiation) {
        if (sendRetryManager.shouldDelay(negotiation)) {
            breakLease(negotiation);
            return false;
        }

        var rejection = ContractRejection.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .correlationId(negotiation.getId())
                .rejectionReason(negotiation.getErrorDetail())
                .build();

        // TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, rejection, negotiation::getId)
                .whenComplete(onRejectionSent(negotiation.getId()));
        return false;
    }

    @NotNull
    private BiConsumer<Object, Throwable> onInitialOfferSent(String id) {
        return new AsyncSendResultHandler(id, "send initial offer")
                .onSuccess(negotiation -> {
                    negotiation.transitionRequested();
                    negotiationStore.save(negotiation);
                    observable.invokeForEach(l -> l.requested(negotiation));
                })
                .onFailure(negotiation -> {
                    negotiation.transitionRequesting();
                    negotiationStore.save(negotiation);
                })
                .build();
    }

    @NotNull
    private BiConsumer<Object, Throwable> onCounterOfferSent(String negotiationId) {
        return new AsyncSendResultHandler(negotiationId, "send counter offer")
                .onSuccess(negotiation -> {
                    negotiation.transitionOffered();
                    negotiationStore.save(negotiation);
                    observable.invokeForEach(l -> l.offered(negotiation));
                })
                .onFailure(negotiation -> {
                    negotiation.transitionOffering();
                    negotiationStore.save(negotiation);
                })
                .build();
    }

    @NotNull
    private BiConsumer<Object, Throwable> onAgreementSent(String negotiationId) {
        return new AsyncSendResultHandler(negotiationId, "send agreement")
                .onSuccess(negotiation -> {
                    negotiation.transitionApproved();
                    negotiationStore.save(negotiation);
                    observable.invokeForEach(l -> l.approved(negotiation));
                })
                .onFailure(negotiation -> {
                    negotiation.transitionApproving();
                    negotiationStore.save(negotiation);
                })
                .build();
    }

    @NotNull
    private BiConsumer<Object, Throwable> onRejectionSent(String negotiationId) {
        return new AsyncSendResultHandler(negotiationId, "send rejection")
                .onSuccess(negotiation -> {
                    negotiation.transitionDeclined();
                    negotiationStore.save(negotiation);
                    observable.invokeForEach(l -> l.declined(negotiation));
                })
                .onFailure(negotiation -> {
                    negotiation.transitionDeclining();
                    negotiationStore.save(negotiation);
                })
                .build();
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
