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

import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse.Status.OK;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;

/**
 * Implementation of the {@link ConsumerContractNegotiationManager}.
 *
 * TODO
 * - InMemoryContractNegotiationStore (see InMemoryTransferProcessStore), implement ContractNegotiationStore
 * - ContractNegotiation: Builder, transfer change methods
 * - ConsumerContractNegotiationManager & ProviderContractNegotiationManager: add start and stop methods, builder
 * - method call in CoreTransferExtension
 *
 */
public class ConsumerContractNegotiationManagerImpl implements ConsumerContractNegotiationManager {
    private final AtomicBoolean active = new AtomicBoolean();
    private ContractNegotiationStore store;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private Monitor monitor;
    private String connectorId;

    public ConsumerContractNegotiationManagerImpl(ContractNegotiationStore store, ServiceExtensionContext context) {
        this.store = store;
        this.connectorId = context.getConnectorId();

    }

    /**
     * Method triggered by event.
     */
    @Override
    public NegotiationResponse initiate(ContractOfferRequest contractOffer) {
        var contractNegotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol(contractOffer.getProtocol())
                .state(0)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .counterPartyId(contractOffer.getConnectorId())
                .build();

        contractNegotiation.addContractOffer(contractOffer.getContractOffer());
        store.create(contractNegotiation);

        return new NegotiationResponse(OK, contractNegotiation);
    }

    @Override
    public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
        var contractNegotiation = store.find(negotiationId);
        // TODO Validate contract offer against last offer.

        // Approve: set status to CONSUMER_APPROVING
        // counter offer: set status to CONSUMER_OFFERING
        // declined: set status to DECLINING

        return new NegotiationResponse(OK, contractNegotiation);
    }

    @Override
    public NegotiationResponse confirmed(ClaimToken token, String negotiationId, ContractAgreement contract, String hash) {
        var contractNegotiation = store.find(negotiationId);

        // TODO Validate agreement against last offer.

        // Depending on the current status, the contract negotiation status is set to CONFIRMED.
        var currentStatus = ContractNegotiationStates.from(contractNegotiation.getState());
        switch (currentStatus) {
            case REQUESTED:
                contractNegotiation.transitionConfirmedFromRequested();
            case CONSUMER_OFFERED:
                contractNegotiation.transitionConfirmedFromOffered();
        }

        return new NegotiationResponse(OK, contractNegotiation);
    }

    @Override
    public NegotiationResponse declined(ClaimToken token, String negotiationId) {
        var contractNegotiation = store.find(negotiationId);
        var currentStatus = ContractNegotiationStates.from(contractNegotiation.getState());

        // Depending on the current status, the contract negotiation status is set to DECLINED.
        switch (currentStatus) {
            case REQUESTED:
                contractNegotiation.transitionDecliningFromRequested();
            case CONSUMER_OFFERED:
                contractNegotiation.transitionDecliningFromOffered();
            case CONSUMER_APPROVED:
                contractNegotiation.transitionDecliningFromApproved();
        }

        return new NegotiationResponse(OK, contractNegotiation);
    }

    private int sendQueuedContractOffers() {
        var processes = store.nextForState(ContractNegotiationStates.REQUESTING.code(), batchSize);

        for (ContractNegotiation process : processes) {
            var offer = process.getLastContractOffer();

            var contractRequest = ContractOfferRequest.Builder.newInstance()
                    .contractOffer(offer)
                    .connectorAddress(process.getCounterPartyAddress())
                    .protocol(process.getProtocol())
                    .connectorId(connectorId)
                    .build();

            dispatcherRegistry.send(Void.class, contractRequest, process::getId);
            // TODO Check response for success. If no success, leave as requesting and retry later.
            store.update(process);
        }
        return processes.size();
    }

    private int sendCounterOffers() {
        // TODO
        return 0;
    }

    private int approveContractOffers() {
        // TODO
        return 0;
    }

    private int declineContractOffers() {
        // TODO
        return 0;
    }

    private void run() {
        while (active.get()) {
            try {
                int requested = sendQueuedContractOffers();
                int offered = sendCounterOffers();
                int approved = approveContractOffers();
                int declined = declineContractOffers();

                if (requested + offered + approved + declined == 0) {
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
                monitor.severe("Error caught in negotiation process manager", e);
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
}
