/*
 *  Copyright (c) 2021 Microsoft Corporation
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
 *
 */
package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.core.manager.EntitiesProcessor;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationWaitStrategy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.DEFINITION_PART;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.parseContractId;
import static org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult.Status.FATAL_ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl implements ProviderContractNegotiationManager {

    private final AtomicBoolean active = new AtomicBoolean();

    private int batchSize = 5;
    private NegotiationWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds

    private ContractNegotiationStore negotiationStore;
    private ContractValidationService validationService;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private Monitor monitor;
    private ExecutorService executor;
    private ContractNegotiationObservable observable;
    private Predicate<Boolean> isProcessed = it -> it;

    private ProviderContractNegotiationManagerImpl() { }

    //TODO check state count for retry

    //TODO validate previous offers against hash?

    public void start(ContractNegotiationStore negotiationStore) {
        this.negotiationStore = negotiationStore;
        active.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::run);
    }

    public void stop() {
        active.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Tells this manager that a {@link ContractNegotiation} has been declined by the counter-party.
     * Transitions the corresponding ContractNegotiation to state DECLINED.
     *
     * @param token Claim token of the consumer that sent the rejection.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @return a {@link NegotiationResult}: OK, if successfully transitioned to declined;
     *         FATAL_ERROR, if no match found for Id.
     */
    @Override
    public NegotiationResult declined(ClaimToken token, String correlationId) {
        var negotiation = negotiationStore.findForCorrelationId(correlationId);
        if (negotiation == null) {
            return NegotiationResult.failure(FATAL_ERROR);
        }

        monitor.debug("[Provider] Contract rejection received. Abort negotiation process");

        // Remove agreement if present
        if (negotiation.getContractAgreement() != null) {
            negotiation.setContractAgreement(null);
        }
        negotiation.transitionDeclined();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.declined(negotiation));
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return NegotiationResult.success(negotiation);
    }

    /**
     * Initiates a new {@link ContractNegotiation}. The ContractNegotiation is created and
     * persisted, which moves it to state REQUESTED. It is then validated and transitioned to
     * CONFIRMING, PROVIDER_OFFERING or DECLINING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param request Container object containing all relevant request parameters.
     * @return a {@link NegotiationResult}: OK
     */
    @Override
    public NegotiationResult requested(ClaimToken token, ContractOfferRequest request) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .counterPartyId(request.getConnectorId())
                .counterPartyAddress(request.getConnectorAddress())
                .protocol(request.getProtocol())
                .type(ContractNegotiation.Type.PROVIDER)
                .build();

        negotiation.transitionRequested();

        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.requested(negotiation));
        
        monitor.debug(String.format("[Provider] ContractNegotiation initiated. %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return processIncomingOffer(negotiation, token, request.getContractOffer());
    }

    /**
     * Tells this manager that a new contract offer has been received for a
     * {@link ContractNegotiation}. The offer is validated and the ContractNegotiation is
     * transitioned to CONFIRMING, PROVIDER_OFFERING or DECLINING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @param offer The contract offer.
     * @param hash A hash of all previous contract offers.
     * @return a {@link NegotiationResult}: FATAL_ERROR, if no match found for Id; OK otherwise
     */
    @Override
    public NegotiationResult offerReceived(ClaimToken token, String correlationId, ContractOffer offer, String hash) {
        var negotiation = negotiationStore.findForCorrelationId(correlationId);
        if (negotiation == null) {
            return NegotiationResult.failure(FATAL_ERROR);
        }

        return processIncomingOffer(negotiation, token, offer);
    }

    /**
     * Processes an incoming offer for a {@link ContractNegotiation}. The offer is validated and
     * the corresponding ContractNegotiation is transitioned to CONFIRMING, PROVIDER_OFFERING or
     * DECLINING.
     *
     * @param negotiation The ContractNegotiation.
     * @param token Claim token of the consumer that send the contract request.
     * @param offer The contract offer.
     * @return a {@link NegotiationResult}: OK
     */
    private NegotiationResult processIncomingOffer(ContractNegotiation negotiation, ClaimToken token, ContractOffer offer) {
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
            negotiation.setErrorDetail("Contract rejected."); //TODO set error detail
            negotiation.transitionDeclining();
            negotiationStore.save(negotiation);
            observable.invokeForEach(l -> l.declining(negotiation));
            
            monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return NegotiationResult.success(negotiation);
        }

        monitor.debug("[Provider] Contract offer received. Will be approved.");
        // negotiation.addContractOffer(result.getValidatedOffer()); TODO
        negotiation.transitionConfirming();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.confirming(negotiation));
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return NegotiationResult.success(negotiation);
    }

    /**
     * Tells this manager that a previously sent contract offer has been approved by the consumer.
     * Transitions the corresponding {@link ContractNegotiation} to state CONFIRMING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param correlationId Id of the ContractNegotiation on consumer side.
     * @param agreement Agreement sent by consumer.
     * @param hash A hash of all previous contract offers.
     * @return a {@link NegotiationResult}: FATAL_ERROR, if no match found for Id; OK otherwise
     */
    @Override
    public NegotiationResult consumerApproved(ClaimToken token, String correlationId, ContractAgreement agreement, String hash) {
        var negotiation = negotiationStore.findForCorrelationId(correlationId);
        if (negotiation == null) {
            return NegotiationResult.failure(FATAL_ERROR);
        }

        monitor.debug("[Provider] Contract offer has been approved by consumer.");
        negotiation.transitionConfirming();
        negotiationStore.save(negotiation);
        observable.invokeForEach(l -> l.confirming(negotiation));
        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return NegotiationResult.success(negotiation);
    }

    /**
     * Continuously checks all unfinished {@link ContractNegotiation}s and performs actions based on
     * their states.
     */
    private void run() {
        while (active.get()) {
            try {
                var providerOffering = onNegotiationsInState(PROVIDER_OFFERING).doProcess(this::processProviderOffering);
                var declining = onNegotiationsInState(DECLINING).doProcess(this::processDeclining);
                var confirming = onNegotiationsInState(CONFIRMING).doProcess(this::processConfirming);

                var totalProcessed = providerOffering + declining + confirming;

                if (totalProcessed == 0) {
                    Thread.sleep(waitStrategy.waitForMillis());
                }
                waitStrategy.success();
            } catch (Error e) {
                throw e; // let the thread die and don't reschedule as the error is unrecoverable
            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Throwable e) {
                monitor.severe("Error caught in provider contract negotiation manager", e);
                try {
                    Thread.sleep(waitStrategy.retryInMillis());
                } catch (InterruptedException e2) {
                    Thread.interrupted();
                    active.set(false);
                    break;
                }
            }
        }
    }

    private EntitiesProcessor<ContractNegotiation> onNegotiationsInState(ContractNegotiationStates state) {
        return new EntitiesProcessor<>(() -> negotiationStore.nextForState(state.code(), batchSize));
    }

    /**
     * Processes {@link ContractNegotiation} in state PROVIDER_OFFERING. Tries to send the current
     * offer to the respective consumer. If this succeeds, the ContractNegotiation is transitioned
     * to state PROVIDER_OFFERED. Else, it is transitioned to PROVIDER_OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
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
                .whenComplete((response, throwable) -> {
                    if (throwable == null) {
                        negotiation.transitionOffered();
                        negotiationStore.save(negotiation);
                        observable.invokeForEach(l -> l.providerOffered(negotiation));
                        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
                    } else {
                        negotiation.transitionOffering();
                        negotiationStore.save(negotiation);
                        observable.invokeForEach(l -> l.providerOffering(negotiation));
                        String message = format("[Provider] Failed to send contract offer with id %s. ContractNegotiation %s stays in state %s.",
                                currentOffer.getId(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                        monitor.debug(message, throwable);
                    }
                });
        return false;
    }

    /**
     * Processes {@link ContractNegotiation} in state DECLINING. Tries to send a contract rejection
     * to the respective consumer. If this succeeds, the ContractNegotiation is transitioned
     * to state DECLINED. Else, it is transitioned to DECLINING for a retry.
     *
     * @return true if processed, false elsewhere
     */
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
                .whenComplete((response, throwable) -> {
                    if (throwable == null) {
                        negotiation.transitionDeclined();
                        negotiationStore.save(negotiation);
                        observable.invokeForEach(l -> l.declined(negotiation));
                        monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
                    } else {
                        negotiation.transitionDeclining();
                        negotiationStore.save(negotiation);
                        observable.invokeForEach(l -> l.declining(negotiation));
                        String message = format("[Provider] Failed to send contract rejection. ContractNegotiation %s stays in state %s.",
                                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                        monitor.debug(message, throwable);
                    }
                });
        return false;
    }

    /**
     * Processes {@link ContractNegotiation} in state CONFIRMING. Tries to send a contract
     * agreement to the respective consumer. If this succeeds, the ContractNegotiation is
     * transitioned to state CONFIRMED. Else, it is transitioned to CONFIRMING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @NotNull
    private Boolean processConfirming(ContractNegotiation negotiation) {
        var retrievedAgreement = negotiation.getContractAgreement();

        ContractAgreement agreement;
        if (retrievedAgreement == null) {
            var lastOffer = negotiation.getLastContractOffer();

            var contractIdTokens = parseContractId(lastOffer.getId());
            if (contractIdTokens.length != 2) {
                monitor.severe("ProviderContractNegotiationManagerImpl.checkConfirming(): Offer Id not correctly formatted.");
                return false;
            }
            var definitionId = contractIdTokens[DEFINITION_PART];

            //TODO move to own service
            agreement = ContractAgreement.Builder.newInstance()
                    .id(ContractId.createContractId(definitionId))
                    .contractEndDate(Instant.now().getEpochSecond() + 60 * 60 /* Five Minutes */) // TODO
                    .contractSigningDate(Instant.now().getEpochSecond() - 60 * 5 /* Five Minutes */)
                    .contractStartDate(Instant.now().getEpochSecond())
                    .providerAgentId(String.valueOf(lastOffer.getProvider()))
                    .consumerAgentId(String.valueOf(lastOffer.getConsumer()))
                    .policy(lastOffer.getPolicy())
                    .asset(lastOffer.getAsset())
                    .build();
        } else {
            agreement = retrievedAgreement;
        }

        ContractAgreementRequest request = ContractAgreementRequest.Builder.newInstance()
                .protocol(negotiation.getProtocol())
                .connectorId(negotiation.getCounterPartyId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractAgreement(agreement)
                .correlationId(negotiation.getCorrelationId())
                .build();

        //TODO protocol-independent response type?
        negotiation.transitionConfirmingSent();
        negotiationStore.save(negotiation);
        dispatcherRegistry.send(Object.class, request, () -> null)
                .whenComplete(onAgreementSent(negotiation.getId(), agreement));
        return true;
    }

    @NotNull
    private BiConsumer<Object, Throwable> onAgreementSent(String id, ContractAgreement agreement) {
        return (response, throwable) -> {
            ContractNegotiation negotiation = negotiationStore.find(id);
            if (negotiation == null) {
                monitor.severe(String.format("[Provider] ContractNegotiation %s not found.", id));
                return;
            }

            if (throwable == null) {
                negotiation.setContractAgreement(agreement);
                negotiation.transitionConfirmed();
                negotiationStore.save(negotiation);
                observable.invokeForEach(l -> l.confirmed(negotiation));
                monitor.debug(String.format("[Provider] ContractNegotiation %s is now in state %s.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            } else {
                negotiation.transitionConfirming();
                negotiationStore.save(negotiation);
                observable.invokeForEach(l -> l.confirming(negotiation));
                String message = format("[Provider] Failed to send contract agreement with id %s. ContractNegotiation %s stays in state %s.",
                        agreement.getId(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                monitor.debug(message, throwable);
            }
        };
    }

    /**
     * Builder for ProviderContractNegotiationManagerImpl.
     */
    public static class Builder {
        private final ProviderContractNegotiationManagerImpl manager;

        private Builder() {
            manager = new ProviderContractNegotiationManagerImpl();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder validationService(ContractValidationService validationService) {
            manager.validationService = validationService;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder batchSize(int batchSize) {
            manager.batchSize = batchSize;
            return this;
        }

        public Builder waitStrategy(NegotiationWaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return this;
        }

        public Builder dispatcherRegistry(RemoteMessageDispatcherRegistry dispatcherRegistry) {
            manager.dispatcherRegistry = dispatcherRegistry;
            return this;
        }

        public Builder observable(ContractNegotiationObservable observable) {
            manager.observable = observable;
            return this;
        }

        public ProviderContractNegotiationManagerImpl build() {
            Objects.requireNonNull(manager.validationService, "contractValidationService");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.observable, "observable");
            return manager;
        }
    }
}
