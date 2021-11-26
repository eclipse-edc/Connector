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

import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.contract.validation.OfferValidationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
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

import static org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse.Status.OK;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl implements ProviderContractNegotiationManager {

    private final AtomicBoolean active = new AtomicBoolean();

    private ContractNegotiationStore negotiationStore;
    private ContractValidationService validationService;

    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds

    private Monitor monitor;
    private ExecutorService executor;

    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    private ProviderContractNegotiationManagerImpl() { }

    //TODO error state

    //TODO logging

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
        negotiation.transitionDeclined();
        negotiationStore.update(negotiation);

        return new NegotiationResponse(OK);
    }

    @Override
    public NegotiationResponse requested(ClaimToken token, String correlationId, ContractOfferRequest request) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .counterPartyId(request.getConnectorId())
                .counterPartyAddress(request.getConnectorAddress())
                .protocol(request.getProtocol())
                .state(0)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .type(ContractNegotiation.Type.PROVIDER)
                .build();
        negotiationStore.create(negotiation); //TODO should transition state to requested

        OfferValidationResult result = validationService.validate(token, request.getContractOffer());
        if (result.invalid()) {
            //TODO how to decide whether to decline or send counter offer?
            negotiation.transitionDeclining(); //TODO error detail
            negotiationStore.update(negotiation);
            return new NegotiationResponse(OK, negotiation);
        }

        negotiation.addContractOffer(result.getValidatedOffer());
        negotiation.transitionConfirming();
        negotiationStore.update(negotiation);
        return new NegotiationResponse(OK, negotiation);
    }

    @Override
    public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer offer, String hash) {
        var negotiation = negotiationStore.find(negotiationId);

        OfferValidationResult result = validationService.validate(token, offer);
        if (result.invalid()) {
            //TODO how to decide whether to decline or counter offer?
            negotiation.transitionDeclining(); //TODO set error detail
            negotiationStore.update(negotiation);
            return new NegotiationResponse(OK, negotiation);
        }

        negotiation.addContractOffer(offer);
        negotiation.transitionConfirming();
        negotiationStore.update(negotiation);
        return new NegotiationResponse(OK, negotiation);
    }

    @Override
    public NegotiationResponse consumerApproved(ClaimToken token, String negotiationId, ContractAgreement agreement, String hash) {
        var negotiation = negotiationStore.find(negotiationId);

        boolean validationPassed = validationService.validate(token, agreement);
        if (!validationPassed) {
            negotiation.transitionDeclining(); //TODO set error detail
            negotiationStore.update(negotiation);
            return new NegotiationResponse(OK, negotiation);
        }

        negotiation.setContractAgreement(agreement);
        negotiation.transitionApproved(); // TODO Shouldn't this be confirming?
        negotiationStore.update(negotiation);
        return new NegotiationResponse(OK, negotiation);
    }

    private void run() {
        while (active.get()) {
            try {
                int providerOffering = checkProviderOffering();

                int declining = checkDeclining();

                int consumerApproved = checkConsumerApproved();

                int confirming = checkConfirming();

                if (providerOffering + declining + consumerApproved + confirming == 0) {
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
            //TODO where is current offer constructed?
            var currentOffer = negotiation.getContractOffers().get(negotiation.getContractOffers().size() - 1);

            var contractOfferRequest = ContractOfferRequest.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .contractOffer(currentOffer)
                    .build();

            //TODO response type (cannot be specific zu multipart)
            var response = dispatcherRegistry.send(Object.class, contractOfferRequest, () -> null);

            if (response.isCompletedExceptionally()) {
                negotiation.transitionOffering();
                continue;
            }

            negotiation.transitionOffered();
            negotiationStore.update(negotiation);
        }

        return offeringNegotiations.size();
    }

    private int checkDeclining() {
        var decliningNegotiations = negotiationStore.nextForState(ContractNegotiationStates.DECLINING.code(), batchSize);

        for (var negotiation: decliningNegotiations) {
            var currentOffer = negotiation.getContractOffers().get(negotiation.getContractOffers().size() - 1);

            ContractRejection rejection = ContractRejection.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .correlatedContractId(currentOffer.getId())
                    .rejectionReason(negotiation.getErrorDetail())
                    .build();

            //TODO response type (cannot be specific zu multipart)
            var response = dispatcherRegistry.send(Object.class, rejection, () -> null);

            if (response.isCompletedExceptionally()) {
                negotiation.transitionDeclining();
                continue;
            }

            negotiation.transitionDeclined();
        }

        return decliningNegotiations.size();
    }

    private int checkConsumerApproved() {
        var consumerApprovedNegotiations = negotiationStore.nextForState(ContractNegotiationStates.CONSUMER_APPROVED.code(), batchSize);

        for (var negotiation: consumerApprovedNegotiations) {
            ContractAgreementRequest request = ContractAgreementRequest.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .contractAgreement(negotiation.getContractAgreement())
                    .build();

            //TODO response type (cannot be specific zu multipart)
            var response = dispatcherRegistry.send(Object.class, request, () -> null);

            if (response.isCompletedExceptionally()) {
                negotiation.transitionApproved();
                continue;
            }

            negotiation.transitionConfirmed();
            negotiationStore.update(negotiation);
        }

        return consumerApprovedNegotiations.size();
    }

    private int checkConfirming() {
        var confirmingNegotiations = negotiationStore.nextForState(ContractNegotiationStates.CONFIRMING.code(), batchSize);

        for (var negotiation: confirmingNegotiations) {
            var agreement = negotiation.getContractAgreement();
            ContractAgreementRequest request = ContractAgreementRequest.Builder.newInstance()
                    .protocol(negotiation.getProtocol())
                    .connectorId(negotiation.getCounterPartyId())
                    .connectorAddress(negotiation.getCounterPartyAddress())
                    .contractAgreement(agreement)
                    .build();

            //TODO response type (cannot be specific zu multipart)
            var response = dispatcherRegistry.send(Object.class, request, () -> null);

            if (response.isCompletedExceptionally()) {
                negotiation.transitionConfirming();
                continue;
            }

            negotiation.transitionConfirmed();
            negotiationStore.update(negotiation);

            //TODO how to check if consumer also approved?
        }

        return confirmingNegotiations.size();
    }

    public static class Builder {
        private final ProviderContractNegotiationManagerImpl manager;

        private Builder() {
            manager = new ProviderContractNegotiationManagerImpl();
        }

        public Builder newInstance() {
            return new Builder();
        }

        public Builder negotiationStore(ContractNegotiationStore negotiationStore) {
            manager.negotiationStore = negotiationStore;
            return this;
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

        public Builder waitStrategy(TransferWaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return this;
        }

        public Builder dispatcherRegistry(RemoteMessageDispatcherRegistry dispatcherRegistry) {
            manager.dispatcherRegistry = dispatcherRegistry;
            return this;
        }

        public ProviderContractNegotiationManagerImpl build() {
            Objects.requireNonNull(manager.negotiationStore, "contractNegotiationStore");
            Objects.requireNonNull(manager.validationService, "contractValidationService");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            return manager;
        }
    }
}
