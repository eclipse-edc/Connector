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

package org.eclipse.dataspaceconnector.contract.negotiation;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.dataspaceconnector.common.statemachine.StateMachine;
import org.eclipse.dataspaceconnector.common.statemachine.StateProcessorImpl;
import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.DEFINITION_PART;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.parseContractId;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ProviderContractNegotiationManager {

    private StateMachine stateMachine;

    private ProviderContractNegotiationManagerImpl() {
    }

    //TODO check state count for retry

    //TODO validate previous offers against hash?

    public void start() {
        stateMachine = StateMachine.Builder.newInstance("provider-contract-negotiation", monitor, executorInstrumentation, waitStrategy)
                .processor(processNegotiationsInState(PROVIDER_OFFERING, this::processProviderOffering))
                .processor(processNegotiationsInState(DECLINING, this::processDeclining))
                .processor(processNegotiationsInState(CONFIRMING, this::processConfirming))
                .processor(onCommands(this::processCommand))
                .build();

        stateMachine.start();
    }

    public void stop() {
        if (stateMachine != null) {
            stateMachine.stop();
        }
    }

    /**
     * Tells this manager that a {@link ContractNegotiation} has been declined by the counter-party. Transitions the
     * corresponding ContractNegotiation to state DECLINED.
     *
     * @param token Claim token of the consumer that sent the rejection.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @return a {@link StatusResult}: OK, if successfully transitioned to declined; FATAL_ERROR, if no match found for
     *         Id.
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
        update(negotiation, l -> l.preDeclined(negotiation));
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
     * @param token Claim token of the consumer that send the contract request.
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

        update(negotiation, l -> l.preRequested(negotiation));

        monitor.debug(String.format("[Provider] ContractNegotiation initiated. %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return processIncomingOffer(negotiation, token, request.getContractOffer());
    }

    /**
     * Tells this manager that a new contract offer has been received for a {@link ContractNegotiation}. The offer is
     * validated and the ContractNegotiation is transitioned to CONFIRMING, PROVIDER_OFFERING or DECLINING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @param offer The contract offer.
     * @param hash A hash of all previous contract offers.
     * @return a {@link StatusResult}: FATAL_ERROR, if no match found for Id; OK otherwise
     */
    @WithSpan
    @Override
    public StatusResult<ContractNegotiation> offerReceived(ClaimToken token, String correlationId, ContractOffer offer, String hash) {
        var negotiation = negotiationStore.findForCorrelationId(correlationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR);
        }

        return processIncomingOffer(negotiation, token, offer);
    }

    /**
     * Tells this manager that a previously sent contract offer has been approved by the consumer. Transitions the
     * corresponding {@link ContractNegotiation} to state CONFIRMING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @param agreement Agreement sent by consumer.
     * @param hash A hash of all previous contract offers.
     * @return a {@link StatusResult}: FATAL_ERROR, if no match found for Id; OK otherwise
     */
    @Override
    public StatusResult<ContractNegotiation> consumerApproved(ClaimToken token, String correlationId, ContractAgreement agreement, String hash) {
        var negotiation = negotiationStore.findForCorrelationId(correlationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR);
        }

        monitor.debug("[Provider] Contract offer has been approved by consumer.");
        negotiation.transitionConfirming();
        update(negotiation, l -> l.preConfirming(negotiation));
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return StatusResult.success(negotiation);
    }

    private ContractNegotiation findContractNegotiationById(String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            negotiation = negotiationStore.findForCorrelationId(negotiationId);
        }

        return negotiation;
    }

    /**
     * Processes an incoming offer for a {@link ContractNegotiation}. The offer is validated and the corresponding
     * ContractNegotiation is transitioned to CONFIRMING, PROVIDER_OFFERING or DECLINING.
     *
     * @param negotiation The ContractNegotiation.
     * @param token Claim token of the consumer that send the contract request.
     * @param offer The contract offer.
     * @return a {@link StatusResult}: OK
     */
    private StatusResult<ContractNegotiation> processIncomingOffer(ContractNegotiation negotiation, ClaimToken token, ContractOffer offer) {
        Result<ContractOffer> result;
        if (negotiation.getContractOffers().isEmpty()) {
            result = validationService.validate(token, offer);
        } else {
            var lastOffer = negotiation.getLastContractOffer();
            result = validationService.validate(token, offer, lastOffer);
        }

        negotiation.addContractOffer(offer); // TODO persist unchecked offer of consumer?

        if (result.failed()) {
            monitor.debug("[Provider] Contract offer received. Will be rejected.");
            negotiation.setErrorDetail(result.getFailureMessages().get(0));
            negotiation.transitionDeclining();
            update(negotiation, l -> l.preDeclining(negotiation));

            monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return StatusResult.success(negotiation);
        }

        monitor.debug("[Provider] Contract offer received. Will be approved.");
        // negotiation.addContractOffer(result.getValidatedOffer()); TODO
        negotiation.transitionConfirming();
        update(negotiation, l -> l.preConfirming(negotiation));
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return StatusResult.success(negotiation);
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

        //TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, contractOfferRequest, () -> null)
                .whenComplete(onCounterOfferSent(negotiation.getId(), currentOffer.getId()));
        return false;
    }

    @NotNull
    private BiConsumer<Object, Throwable> onCounterOfferSent(String negotiationId, String offerId) {
        return (response, throwable) -> {
            var negotiation = negotiationStore.find(negotiationId);
            if (negotiation == null) {
                monitor.severe(String.format("[Provider] ContractNegotiation %s not found.", negotiationId));
                return;
            }

            if (throwable == null) {
                negotiation.transitionOffered();
                update(negotiation, l -> l.preProviderOffered(negotiation));
                monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            } else {
                negotiation.transitionOffering();
                update(negotiation, l -> l.preProviderOffering(negotiation));
                String message = format("[Provider] Failed to send contract offer with id %s. ContractNegotiation %s stays in state %s.",
                        offerId, negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                monitor.debug(message, throwable);
            }
        };
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
        ContractRejection rejection = ContractRejection.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .correlationId(negotiation.getCorrelationId())
                .rejectionReason(negotiation.getErrorDetail())
                .build();

        //TODO protocol-independent response type?
        dispatcherRegistry.send(Object.class, rejection, () -> null)
                .whenComplete(onRejectionSent(negotiation.getId()));

        return false;
    }

    @NotNull
    private BiConsumer<Object, Throwable> onRejectionSent(String negotiationId) {
        return (response, throwable) -> {
            var negotiation = negotiationStore.find(negotiationId);
            if (negotiation == null) {
                monitor.severe(String.format("[Provider] ContractNegotiation %s not found.", negotiationId));
                return;
            }

            if (throwable == null) {
                negotiation.transitionDeclined();
                update(negotiation, l -> l.preDeclined(negotiation));
                monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            } else {
                negotiation.transitionDeclining();
                update(negotiation, l -> l.preDeclining(negotiation));
                String message = format("[Provider] Failed to send contract rejection. ContractNegotiation %s stays in state %s.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                monitor.debug(message, throwable);
            }
        };
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
        var retrievedAgreement = negotiation.getContractAgreement();

        ContractAgreement agreement;
        Policy policy;
        if (retrievedAgreement == null) {
            var lastOffer = negotiation.getLastContractOffer();

            var contractIdTokens = parseContractId(lastOffer.getId());
            if (contractIdTokens.length != 2) {
                monitor.severe("ProviderContractNegotiationManagerImpl.checkConfirming(): Offer Id not correctly formatted.");
                return false;
            }
            var definitionId = contractIdTokens[DEFINITION_PART];

            policy = lastOffer.getPolicy();
            //TODO move to own service
            agreement = ContractAgreement.Builder.newInstance()
                    .id(ContractId.createContractId(definitionId))
                    .contractStartDate(clock.instant().getEpochSecond())
                    .contractEndDate(clock.instant().plus(365, ChronoUnit.DAYS).getEpochSecond()) // TODO Make configurable (issue #722)
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
        dispatcherRegistry.send(Object.class, request, () -> null)
                .whenComplete(onAgreementSent(negotiation.getId(), agreement));
        return true;
    }

    @NotNull
    private BiConsumer<Object, Throwable> onAgreementSent(String id, ContractAgreement agreement) {
        return (response, throwable) -> {
            var negotiation = negotiationStore.find(id);
            if (negotiation == null) {
                monitor.severe(String.format("[Provider] ContractNegotiation %s not found.", id));
                return;
            }

            if (throwable == null) {
                negotiation.setContractAgreement(agreement);
                negotiation.transitionConfirmed();
                update(negotiation, l -> l.preConfirmed(negotiation));
                monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            } else {
                negotiation.transitionConfirming();
                update(negotiation, l -> l.preConfirming(negotiation));
                String message = format("[Provider] Failed to send contract agreement with id %s. ContractNegotiation %s stays in state %s.",
                        agreement.getId(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                monitor.debug(message, throwable);
            }
        };
    }

    private void update(ContractNegotiation negotiation, Consumer<ContractNegotiationListener> observe) {
        observable.invokeForEach(observe);
        negotiationStore.save(negotiation);
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
