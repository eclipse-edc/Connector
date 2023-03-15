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
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONFIRMING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ProviderContractNegotiationManager {

    private StateMachineManager stateMachineManager;

    private ProviderContractNegotiationManagerImpl() {
    }

    //TODO check state count for retry

    //TODO validate previous offers against hash?

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("provider-contract-negotiation", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(PROVIDER_OFFERING, this::processProviderOffering))
                .processor(processNegotiationsInState(DECLINING, this::processDeclining))
                .processor(processNegotiationsInState(CONFIRMING, this::processConfirming))
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
     * Tells this manager that a {@link ContractNegotiation} has been declined by the counter-party. Transitions the
     * corresponding ContractNegotiation to state DECLINED.
     *
     * @param token         Claim token of the consumer that sent the rejection.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @return a {@link StatusResult}: OK, if successfully transitioned to declined; FATAL_ERROR, if no match found for correlationId.
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> declined(ClaimToken token, String correlationId) {
        var negotiation = findContractNegotiationById(correlationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR);
        }

        monitor.debug("[Provider] Contract rejection received. Abort negotiation process.");

        // Remove agreement if present
        if (negotiation.getContractAgreement() != null) {
            negotiation.setContractAgreement(null);
        }
        negotiation.transitionDeclined();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.declined(negotiation));
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return StatusResult.success(negotiation);
    }

    @Override
    public void enqueueCommand(ContractNegotiationCommand command) {
        commandQueue.enqueue(command);
    }

    /**
     * Initiates a new {@link ContractNegotiation}. The ContractNegotiation is created and persisted, which moves it to
     * state REQUESTED. It is then validated and transitioned to CONFIRMING, PROVIDER_OFFERING or DECLINING.
     *
     * @param token   Claim token of the consumer that send the contract request.
     * @param request Container object containing all relevant request parameters.
     * @return a {@link StatusResult}: OK
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> requested(ClaimToken token, ContractOfferRequest request) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .counterPartyId(request.getConnectorId())
                .counterPartyAddress(request.getConnectorAddress())
                .protocol(request.getProtocol())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(PROVIDER)
                .build();

        negotiation.transitionRequested();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.requested(negotiation));

        monitor.debug(String.format("[Provider] ContractNegotiation initiated. %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        var offer = request.getContractOffer();
        var result = validationService.validateInitialOffer(token, offer);

        negotiation.addContractOffer(offer);

        if (result.failed()) {
            monitor.debug("[Provider] Contract offer received. Will be rejected: " + result.getFailureDetail());
            negotiation.setErrorDetail(result.getFailureMessages().get(0));
            negotiation.transitionDeclining();
            negotiationStore.save(negotiation);

            monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return StatusResult.success(negotiation);
        }

        monitor.debug("[Provider] Contract offer received. Will be approved.");
        negotiation.transitionConfirming();
        negotiationStore.save(negotiation);
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return StatusResult.success(negotiation);
    }

    @Override
    protected String getName() {
        return PROVIDER.name();
    }

    private ContractNegotiation findContractNegotiationById(String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
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
        if (sendRetryManager.shouldDelay(negotiation)) {
            breakLease(negotiation);
            return false;
        }

        var currentOffer = negotiation.getLastContractOffer();

        var contractOfferRequest = ContractOfferRequest.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractOffer(currentOffer)
                .correlationId(negotiation.getCorrelationId())
                .build();

        //TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, contractOfferRequest)
                .whenComplete(onCounterOfferSent(negotiation.getId()));
        return false;
    }

    /**
     * Processes {@link ContractNegotiation} in state DECLINING. Tries to send a contract rejection to the respective
     * consumer. If this succeeds, the ContractNegotiation is transitioned to state DECLINED. Else, it is transitioned
     * to DECLINING for a retry.
     *
     * @return true if processed, false elsewhere
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
                .correlationId(negotiation.getCorrelationId())
                .rejectionReason(negotiation.getErrorDetail())
                .build();

        //TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, rejection)
                .whenComplete(onRejectionSent(negotiation.getId()));

        return false;
    }

    /**
     * Processes {@link ContractNegotiation} in state CONFIRMING. Tries to send a contract agreement to the respective
     * consumer. If this succeeds, the ContractNegotiation is transitioned to state CONFIRMED. Else, it is transitioned
     * to CONFIRMING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    private boolean processConfirming(ContractNegotiation negotiation) {
        if (sendRetryManager.shouldDelay(negotiation)) {
            breakLease(negotiation);
            return false;
        }

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
            //TODO move to own service
            agreement = ContractAgreement.Builder.newInstance()
                    .id(ContractId.createContractId(definitionId))
                    .contractStartDate(clock.instant().getEpochSecond())
                    .contractEndDate(lastOffer.getContractEnd())
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

        var request = ContractAgreementRequest.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractAgreement(agreement)
                .correlationId(negotiation.getCorrelationId())
                .policy(policy)
                .build();

        //TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, request)
                .whenComplete(onAgreementSent(negotiation.getId(), agreement));
        return true;
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

    @NotNull
    private BiConsumer<Object, Throwable> onAgreementSent(String id, ContractAgreement agreement) {
        return new AsyncSendResultHandler(id, "send agreement")
                .onSuccess(negotiation -> {
                    negotiation.setContractAgreement(agreement);
                    negotiation.transitionConfirmed();
                    negotiationStore.save(negotiation);
                    observable.invokeForEach(l -> l.confirmed(negotiation));
                })
                .onFailure(negotiation -> {
                    negotiation.transitionConfirming();
                    negotiationStore.save(negotiation);
                })
                .build();
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
