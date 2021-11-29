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

import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationWaitStrategy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.contract.validation.OfferValidationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractRejection;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse.Status.FATAL_ERROR;
import static org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse.Status.OK;

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

    @Override
    public NegotiationResponse declined(ClaimToken token, String negotiationId) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return new NegotiationResponse(FATAL_ERROR);
        }

        negotiation.transitionDeclined();
        negotiationStore.update(negotiation);
        monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return new NegotiationResponse(OK);
    }

    @Override
    public NegotiationResponse requested(ClaimToken token, ContractOfferRequest request) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .counterPartyId(request.getConnectorId())
                .counterPartyAddress(request.getConnectorAddress())
                .protocol(request.getProtocol())
                .state(0)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .type(ContractNegotiation.Type.PROVIDER)
                .build();

        negotiationStore.create(negotiation);
        monitor.debug(String.format("Created ContractNegotiation %s.", negotiation.getId()));

        return processIncomingOffer(negotiation, token, request.getContractOffer());
    }

    @Override
    public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer offer, String hash) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return new NegotiationResponse(FATAL_ERROR);
        }

        return processIncomingOffer(negotiation, token, offer);
    }

    private NegotiationResponse processIncomingOffer(ContractNegotiation negotiation, ClaimToken token, ContractOffer offer) {
        OfferValidationResult result;
        if (negotiation.getContractOffers().isEmpty()) {
            result = validationService.validate(token, offer);
        } else {
            var lastOffer = negotiation.getLastContractOffer();
            result = validationService.validate(token, offer, lastOffer);
        }

        negotiation.addContractOffer(offer); // TODO persist unchecked offer of consumer?

        if (result.invalid()) {
            if (result.isCounterOfferAvailable()) {
                negotiation.addContractOffer(result.getCounterOffer());
                monitor.debug("Contract offer received. A counter offer is available");
                negotiation.transitionOffering();
            } else {
                monitor.debug("Contract offer received. Will be rejected");
                negotiation.transitionDeclining(); //TODO set error detail
            }

            negotiationStore.update(negotiation);
            monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return new NegotiationResponse(OK, negotiation);
        }

        // negotiation.addContractOffer(result.getValidatedOffer()); TODO
        negotiation.transitionConfirming();
        negotiationStore.update(negotiation);
        monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));

        return new NegotiationResponse(OK, negotiation);
    }

    @Override
    public NegotiationResponse consumerApproved(ClaimToken token, String negotiationId, ContractAgreement agreement, String hash) {
        var negotiation = negotiationStore.find(negotiationId);
        if (negotiation == null) {
            return new NegotiationResponse(FATAL_ERROR);
        }

        // TODO Validate against another offer?
        boolean validationPassed = validationService.validate(token, agreement);
        if (!validationPassed) {
            negotiation.transitionDeclining(); //TODO set error detail
            negotiationStore.update(negotiation);
            monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
            return new NegotiationResponse(OK, negotiation);
        }

        negotiation.setContractAgreement(agreement);
        negotiation.transitionConfirmed();
        negotiationStore.update(negotiation);
        monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        return new NegotiationResponse(OK, negotiation);
    }

    private void run() {
        while (active.get()) {
            try {
                int providerOffering = checkProviderOffering();

                int declining = checkDeclining();

                int confirming = checkConfirming();

                if (providerOffering + declining + confirming == 0) {
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

    private int checkProviderOffering() {
        var offeringNegotiations = negotiationStore.nextForState(ContractNegotiationStates.PROVIDER_OFFERING.code(), batchSize);

        for (var negotiation: offeringNegotiations) {
            var currentOffer = negotiation.getLastContractOffer();

            var contractOfferRequest = ContractOfferRequest.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .contractOffer(currentOffer)
                    .build();

            //TODO protocol-independent response type?
            var response = dispatcherRegistry.send(Object.class, contractOfferRequest, () -> null);

            if (response.isCompletedExceptionally()) {
                negotiation.transitionOffering();
                negotiationStore.update(negotiation);
                monitor.debug(String.format("ContractNegotiation %s stays in state %s due to error.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
                continue;
            }

            negotiation.transitionOffered();
            negotiationStore.update(negotiation);
            monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        }

        return offeringNegotiations.size();
    }

    private int checkDeclining() {
        var decliningNegotiations = negotiationStore.nextForState(ContractNegotiationStates.DECLINING.code(), batchSize);

        for (var negotiation: decliningNegotiations) {
            var currentOffer = negotiation.getLastContractOffer();

            ContractRejection rejection = ContractRejection.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .correlationId(negotiation.getId())
                    .rejectionReason(negotiation.getErrorDetail())
                    .build();

            //TODO protocol-independent response type?
            var response = dispatcherRegistry.send(Object.class, rejection, () -> null);
            if (response.isCompletedExceptionally()) {
                negotiation.transitionDeclining();
                negotiationStore.update(negotiation);
                monitor.debug(String.format("ContractNegotiation %s stays in state %s due to error.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
                continue;
            }

            negotiation.transitionDeclined();
            negotiationStore.update(negotiation);
            monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        }

        return decliningNegotiations.size();
    }

    private int checkConfirming() {
        var confirmingNegotiations = negotiationStore.nextForState(ContractNegotiationStates.CONFIRMING.code(), batchSize);

        for (var negotiation: confirmingNegotiations) {
            var agreement = negotiation.getContractAgreement(); // TODO build agreement

            if (agreement == null) {
                var lastOffer = negotiation.getLastContractOffer();

                //TODO move to own service
                agreement = ContractAgreement.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .contractStartDate(lastOffer.getContractStart().toInstant().toEpochMilli())
                        .contractEndDate(lastOffer.getContractEnd().toInstant().toEpochMilli())
                        .contractSigningDate(Instant.now().toEpochMilli())
                        .providerAgentId(lastOffer.getProvider().toString())
                        .consumerAgentId(lastOffer.getConsumer().toString())
                        .policy(lastOffer.getPolicy())
                        .assetIds(lastOffer.getAssets().stream().map(Asset::getId).collect(Collectors.toList()))
                        .build();
            }

            ContractAgreementRequest request = ContractAgreementRequest.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .contractAgreement(agreement)
                    .correlationId(negotiation.getId())
                    .build();

            //TODO protocol-independent response type?
            var response = dispatcherRegistry.send(Object.class, request, () -> null);

            if (response.isCompletedExceptionally()) {
                negotiation.transitionConfirming();
                negotiationStore.update(negotiation);
                monitor.debug(String.format("ContractNegotiation %s stays in state %s due to error.",
                        negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
                continue;
            }

            negotiation.transitionConfirmed();
            negotiationStore.update(negotiation);
            monitor.debug(String.format("ContractNegotiation %s is now in state %s.",
                    negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
        }

        return confirmingNegotiations.size();
    }

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

        public Builder executorService(ExecutorService executor) {
            manager.executor = executor;
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

        public ProviderContractNegotiationManagerImpl build() {
            Objects.requireNonNull(manager.validationService, "contractValidationService");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            return manager;
        }
    }
}
