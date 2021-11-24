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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationResponse.Status.OK;

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

    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private Monitor monitor;

    public ConsumerContractNegotiationManagerImpl(ContractNegotiationStore store) {
        this.store = store;
    }

    @Override
    public NegotiationResponse initiate(ContractOfferRequest request) { // Triggered contract request by method call (see ArtifactRequestController)
        var contractNegotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol(request.getProtocol())
                .state(0)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .counterPartyId(request.getConnectorId())
                .build();

        store.create(contractNegotiation);
        return new NegotiationResponse(OK, contractNegotiation);
    }

    @Override
    public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse confirmed(ClaimToken token, String negotiationId, ContractAgreement contract, String hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NegotiationResponse declined(ClaimToken token, String negotiationId) {
        throw new UnsupportedOperationException();
    }



    private void run() {
        while (active.get()) {
            try {
//                int requesting = initiateContractOfferRequest();
//
//                int requested = sendOrProcessProvisionedRequests();
//
//                int provisioned = checkProvisioned();
//
//                int finished = checkCompleted();
//
//                int deprovisioning = checkDeprovisioningRequested();
//
//                int deprovisioned = checkDeprovisioned();
//
//                if (provisioning + provisioned + sent + finished + deprovisioning + deprovisioned == 0) {
//                    Thread.sleep(waitStrategy.waitForMillis());
//                }
//                waitStrategy.success();
//            } catch (Error e) {
//                throw e; // let the thread die and don't reschedule as the error is unrecoverable
//            } catch (InterruptedException e) {
//                Thread.interrupted();
//                active.set(false);
//                break;
            } catch (Throwable e) {
                monitor.severe("Error caught in transfer process manager", e);
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
