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
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationWaitStrategy;
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

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.DEFINITION_PART;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.parseContractId;
import static org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult.Status.FATAL_ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_APPROVING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_OFFERING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.INITIAL;

/**
 * Implementation of the {@link ConsumerContractNegotiationManager}.
 *
 * TODO
 * - InMemoryContractNegotiationStore (see InMemoryTransferProcessStore), implement ContractNegotiationStore
 * - ContractNegotiation: Builder, transfer change methods
 * - ConsumerContractNegotiationManager & ProviderContractNegotiationManager: add start and stop methods, builder
 * - method call in CoreTransferExtension
 */
public class ConsumerContractNegotiationManagerImpl extends ContractNegotiationObservable implements ConsumerContractNegotiationManager {
    private final AtomicBoolean active = new AtomicBoolean();
    private ContractNegotiationStore negotiationStore;
    private ContractValidationService validationService;

    private int batchSize = 5;
    private NegotiationWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private Monitor monitor;
    private ExecutorService executor;

    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    public ConsumerContractNegotiationManagerImpl() {
    }

    public void start(ContractNegotiationStore store) {
        negotiationStore = store;
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
     * Initiates a new {@link ContractNegotiation}. The ContractNegotiation is created and
     * persisted, which moves it to state REQUESTING.
     *
     * @param contractOffer Container object containing all relevant request parameters.
     * @return a {@link NegotiationResult}: OK
     */
    @Override
    public NegotiationResult initiate(ContractOfferRequest contractOffer) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol(contractOffer.getProtocol())
                .counterPartyId(contractOffer.getConnectorId())
                .counterPartyAddress(contractOffer.getConnectorAddress())
                .build();

        negotiation.addContractOffer(contractOffer.getContractOffer());
        negotiation.transitionInitial();
        negotiationStore.save(negotiation);
        invokeForEach(l -> l.requesting(negotiation));

        monitor.debug(String.format("[Consumer] ContractNegotiation initiated. %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return NegotiationResult.success(negotiation);
    }

    /**
     * Tells this manager that a new contract offer has been received for a
     * {@link ContractNegotiation}. Validates the offer against the last contract offer for that
     * ContractNegotiation and transitions the ContractNegotiation to CONSUMER_APPROVING,
     * CONSUMER_OFFERING or DECLINING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param negotiationId Id of the ContractNegotiation.
     * @param contractOffer The contract offer.
     * @param hash A hash of all previous contract offers.
     * @return a {@link NegotiationResult}: FATAL_ERROR, if no match found for Id or no last
     *         offer found for negotiation; OK otherwise
     */
    @Override
    public NegotiationResult offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return NegotiationResult.failure(FATAL_ERROR);
        }

        var latestOffer = negotiation.getLastContractOffer();
        try {
            Objects.requireNonNull(latestOffer, "latestOffer");
        } catch (NullPointerException e) {
            monitor.severe("[Consumer] No offer found for validation. Process id: " + negotiation.getId());
            return NegotiationResult.failure(FATAL_ERROR);
        }

        Result<ContractOffer> result = validationService.validate(token, contractOffer, latestOffer);
        negotiation.addContractOffer(contractOffer); // TODO persist unchecked offer of provider?
        if (result.failed()) {
            monitor.debug("[Consumer] Contract offer received. Will be rejected.");
            negotiation.setErrorDetail("Contract rejected."); //TODO set error detail
            negotiation.transitionDeclining();
            negotiationStore.save(negotiation);
            invokeForEach(l -> l.declining(negotiation));
        } else {
            // Offer has been approved.
            monitor.debug("[Consumer] Contract offer received. Will be approved.");
            negotiation.transitionApproving();
            negotiationStore.save(negotiation);
            invokeForEach(l -> l.consumerApproving(negotiation));
        }
        
        monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return NegotiationResult.success(negotiation);
    }

    /**
     * Tells this manager that a previously sent contract offer has been confirmed by the provider.
     * Validates the contract agreement sent by the provider against the last contract offer and
     * transitions the corresponding {@link ContractNegotiation} to state CONFIRMED or DECLINING.
     *
     * @param token Claim token of the consumer that send the contract request.
     * @param negotiationId Id of the ContractNegotiation.
     * @param agreement Agreement sent by provider.
     * @param hash A hash of all previous contract offers.
     * @return a {@link NegotiationResult}: FATAL_ERROR, if no match found for Id or no last
     *         offer found for negotiation; OK otherwise
     */
    @Override
    public NegotiationResult confirmed(ClaimToken token, String negotiationId, ContractAgreement agreement, String hash) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return NegotiationResult.failure(FATAL_ERROR);
        }

        var latestOffer = negotiation.getLastContractOffer();
        try {
            Objects.requireNonNull(latestOffer, "latestOffer");
        } catch (NullPointerException e) {
            monitor.severe("[Consumer] No offer found for validation. Process id: " + negotiation.getId());
            return NegotiationResult.failure(FATAL_ERROR);
        }

        var result = validationService.validate(token, agreement, latestOffer);
        if (!result) {
            // TODO Add contract offer possibility.
            monitor.debug("[Consumer] Contract agreement received. Validation failed.");
            negotiation.setErrorDetail("Contract rejected."); //TODO set error detail
            negotiation.transitionDeclining();
            negotiationStore.save(negotiation);
            invokeForEach(l -> l.declining(negotiation));
            monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return NegotiationResult.success(negotiation);
        }

        // Agreement has been approved.
        negotiation.setContractAgreement(agreement); // TODO persist unchecked agreement of provider?
        monitor.debug("[Consumer] Contract agreement received. Validation successful.");
        if (negotiation.getState() != CONFIRMED.code()) {
            // TODO: otherwise will fail. But should do it, since it's already confirmed? A duplicated message received shouldn't be an issue
            negotiation.transitionConfirmed();
        }
        negotiationStore.save(negotiation);
        invokeForEach(l -> l.confirmed(negotiation));
        monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return NegotiationResult.success(negotiation);
    }

    /**
     * Tells this manager that a {@link ContractNegotiation} has been declined by the counter-party.
     * Transitions the corresponding ContractNegotiation to state DECLINED.
     *
     * @param token Claim token of the consumer that sent the rejection.
     * @param negotiationId Id of the ContractNegotiation.
     * @return a {@link NegotiationResult}: OK, if successfully transitioned to declined;
     *         FATAL_ERROR, if no match found for Id.
     */
    @Override
    public NegotiationResult declined(ClaimToken token, String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return NegotiationResult.failure(FATAL_ERROR);
        }

        monitor.debug("[Consumer] Contract rejection received. Abort negotiation process");
        negotiation.transitionDeclined();
        negotiationStore.save(negotiation);
        invokeForEach(l -> l.declined(negotiation));
        monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return NegotiationResult.success(negotiation);
    }

    /**
     * Builds and sends a {@link ContractOfferRequest} for a given {@link ContractNegotiation} and
     * {@link ContractOffer}.
     *
     * @param offer The contract offer.
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
     * Processes {@link ContractNegotiation}s in state INITIAL. Tries to send the current
     * offer to the respective provider and transition ContractNegotiation to REQUESTING.
     * If this succeeds, the ContractNegotiation is transitioned to state REQUESTED.
     * Else, it is transitioned to INITIAL for a retry.
     *
     * @return the number of processed ContractNegotiations.
     */
    private int sendContractOffers() {
        var negotiations = negotiationStore.nextForState(INITIAL.code(), batchSize);

        for (ContractNegotiation negotiation : negotiations) {
            var offer = negotiation.getLastContractOffer();
            negotiation.transitionRequesting();
            negotiationStore.save(negotiation);

            sendOffer(offer, negotiation, ContractOfferRequest.Type.INITIAL)
                    .whenComplete(onOfferSent(negotiation.getId(), offer));
        }

        return negotiations.size();
    }

    @NotNull
    private BiConsumer<Object, Throwable> onOfferSent(String id, ContractOffer offer) {
        return (response, throwable) -> {
            ContractNegotiation negotiation = negotiationStore.find(id);
            if (negotiation == null) {
                monitor.severe(String.format("[Consumer] ContractNegotiation %s not found.", id));
                return;
            }

            if (throwable == null) {
                negotiation.transitionRequested();
                negotiationStore.save(negotiation);
                invokeForEach(l -> l.requested(negotiation));
                monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            } else {
                negotiation.transitionInitial();
                negotiationStore.save(negotiation);
                invokeForEach(l -> l.requesting(negotiation));
                String message = format("[Consumer] Failed to send contract offer with id %s. ContractNegotiation %s stays in state %s.",
                        offer.getId(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState()));
                monitor.debug(message, throwable);
            }
        };
    }

    /**
     * Processes {@link ContractNegotiation}s in state CONSUMER_OFFERING. Tries to send the current
     * offer to the respective provider. If this succeeds, the ContractNegotiation is transitioned
     * to state CONSUMER_OFFERED. Else, it is transitioned to CONSUMER_OFFERING for a retry.
     *
     * @return the number of processed ContractNegotiations.
     */
    private int sendCounterOffers() {
        var processes = negotiationStore.nextForState(CONSUMER_OFFERING.code(), batchSize);

        for (ContractNegotiation process : processes) {
            var offer = process.getLastContractOffer();
            sendOffer(offer, process, ContractOfferRequest.Type.COUNTER_OFFER)
                    .whenComplete((response, throwable) -> {
                        if (throwable == null) {
                            process.transitionOffered();
                            negotiationStore.save(process);
                            invokeForEach(l -> l.consumerOffered(process));
                            monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                                    process.getId(), ContractNegotiationStates.from(process.getState())));
                        } else {
                            process.transitionOffering();
                            negotiationStore.save(process);
                            invokeForEach(l -> l.consumerOffering(process));
                            String message = format("[Consumer] Failed to send contract offer with id %s. ContractNegotiation %s stays in state %s.",
                                    offer.getId(), process.getId(), ContractNegotiationStates.from(process.getState()));
                            monitor.debug(message, throwable);
                        }
                    });
        }

        return processes.size();
    }

    /**
     * Processes {@link ContractNegotiation}s in state CONSUMER_APPROVING. Tries to send a dummy
     * contract agreement to the respective provider in order to approve the last offer sent by the
     * provider. If this succeeds, the ContractNegotiation is transitioned to state
     * CONSUMER_APPROVED. Else, it is transitioned to CONSUMER_APPROVING for a retry.
     *
     * @return the number of processed ContractNegotiations.
     */
    private int approveContractOffers() {
        var processes = negotiationStore.nextForState(CONSUMER_APPROVING.code(), batchSize);

        for (ContractNegotiation process : processes) {
            //TODO this is a dummy agreement used to approve the provider's offer, real agreement will be created and sent by provider
            var lastOffer = process.getLastContractOffer();

            var contractIdTokens = parseContractId(lastOffer.getId());
            if (contractIdTokens.length != 2) {
                monitor.severe("ConsumerContractNegotiationManagerImpl.approveContractOffers(): Offer Id not correctly formatted.");
                continue;
            }
            var definitionId = contractIdTokens[DEFINITION_PART];

            var agreement = ContractAgreement.Builder.newInstance()
                    .id(ContractId.createContractId(definitionId))
                    .contractSigningDate(LocalDate.MIN.toEpochDay())
                    .contractStartDate(LocalDate.MIN.toEpochDay())
                    .contractEndDate(LocalDate.MAX.toEpochDay())
                    .providerAgentId(String.valueOf(lastOffer.getProvider()))
                    .consumerAgentId(String.valueOf(lastOffer.getConsumer()))
                    .policy(lastOffer.getPolicy())
                    .asset(lastOffer.getAsset())
                    .build();

            var request = ContractAgreementRequest.Builder.newInstance()
                    .protocol(process.getProtocol())
                    .connectorId(process.getCounterPartyId())
                    .connectorAddress(process.getCounterPartyAddress())
                    .contractAgreement(agreement)
                    .correlationId(process.getId())
                    .build();

            // TODO protocol-independent response type?
            dispatcherRegistry.send(Object.class, request, process::getId)
                    .whenComplete((response, throwable) -> {
                        if (throwable == null) {
                            process.transitionApproved();
                            negotiationStore.save(process);
                            invokeForEach(l -> l.consumerApproved(process));
                            monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                                    process.getId(), ContractNegotiationStates.from(process.getState())));
                        } else {
                            process.transitionApproving();
                            negotiationStore.save(process);
                            invokeForEach(l -> l.consumerApproving(process));
                            String message = format("[Consumer] Failed to send contract agreement with id %s. ContractNegotiation %s stays in state %s.",
                                    agreement.getId(), process.getId(), ContractNegotiationStates.from(process.getState()));
                            monitor.debug(message, throwable);
                        }
                    });
        }

        return processes.size();
    }

    /**
     * Processes {@link ContractNegotiation}s in state DECLINING. Tries to send a contract rejection
     * to the respective provider. If this succeeds, the ContractNegotiation is transitioned
     * to state DECLINED. Else, it is transitioned to DECLINING for a retry.
     *
     * @return the number of processed ContractNegotiations.
     */
    private int declineContractOffers() {
        var processes = negotiationStore.nextForState(DECLINING.code(), batchSize);

        for (ContractNegotiation process : processes) {
            var rejection = ContractRejection.Builder.newInstance()
                    .protocol(process.getProtocol())
                    .connectorId(process.getCounterPartyId())
                    .connectorAddress(process.getCounterPartyAddress())
                    .correlationId(process.getId())
                    .rejectionReason(process.getErrorDetail())
                    .build();

            // TODO protocol-independent response type?
            dispatcherRegistry.send(Object.class, rejection, process::getId)
                    .whenComplete((response, throwable) -> {
                        if (throwable == null) {
                            process.transitionDeclined();
                            negotiationStore.save(process);
                            invokeForEach(l -> l.declined(process));
                            monitor.debug(String.format("[Consumer] ContractNegotiation %s is now in state %s.",
                                    process.getId(), ContractNegotiationStates.from(process.getState())));
                        } else {
                            process.transitionDeclining();
                            negotiationStore.save(process);
                            invokeForEach(l -> l.declining(process));
                            String message = format("[Consumer] Failed to send contract rejection. ContractNegotiation %s stays in state %s.",
                                    process.getId(), ContractNegotiationStates.from(process.getState()));
                            monitor.debug(message, throwable);
                        }
                    });
        }

        return processes.size();
    }

    /**
     * Continuously checks all unfinished {@link ContractNegotiation}s and performs actions based on
     * their states.
     */
    private void run() {
        while (active.get()) {
            try {
                int requesting = sendContractOffers();
                int offering = sendCounterOffers();
                int approving = approveContractOffers();
                int declining = declineContractOffers();

                if (requesting + offering + approving + declining == 0) {
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
                monitor.severe("Error caught in consumer contract negotiation manager", e);
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

    /**
     * Builder for ConsumerContractNegotiationManagerImpl.
     */
    public static class Builder {
        private final ConsumerContractNegotiationManagerImpl manager;

        private Builder() {
            manager = new ConsumerContractNegotiationManagerImpl();
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

        public ConsumerContractNegotiationManagerImpl build() {
            Objects.requireNonNull(manager.validationService, "contractValidationService");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            return manager;
        }
    }
}
