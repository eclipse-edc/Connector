/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class FakeContractNegotiationManager {

    static ContractNegotiation fakeContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .counterPartyId("test-counterparty-1")
                .counterPartyAddress("test-counterparty-address")
                .protocol("test-protocol")
                .stateCount(1)
                .contractAgreement(ContractAgreement.Builder.newInstance().id("1")
                        .providerAgentId("provider")
                        .consumerAgentId("consumer")
                        .assetId(UUID.randomUUID().toString())
                        .policy(Policy.Builder.newInstance().build())
                        .contractStartDate(Instant.now().getEpochSecond())
                        .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                        .contractSigningDate(Instant.now().getEpochSecond())
                        .id("1:2").build())
                .state(ContractNegotiationStates.CONFIRMED.code())
                .build();
    }

    static class Provider implements ProviderContractNegotiationManager {

        @Override
        public StatusResult<ContractNegotiation> declined(ClaimToken token, String negotiationId) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public void enqueueCommand(ContractNegotiationCommand command) {
        }

        @Override
        public StatusResult<ContractNegotiation> requested(ClaimToken token, ContractOfferRequest request) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public StatusResult<ContractNegotiation> offerReceived(ClaimToken token, String correlationId, ContractOffer offer, String hash) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public StatusResult<ContractNegotiation> consumerApproved(ClaimToken token, String correlationId, ContractAgreement agreement, String hash) {
            return StatusResult.success(fakeContractNegotiation());
        }
    }

    static class Consumer implements ConsumerContractNegotiationManager {

        @Override
        public StatusResult<ContractNegotiation> initiate(ContractOfferRequest contractOffer) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public StatusResult<ContractNegotiation> offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public StatusResult<ContractNegotiation> confirmed(ClaimToken token, String negotiationId, ContractAgreement agreement, Policy policy) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public StatusResult<ContractNegotiation> declined(ClaimToken token, String negotiationId) {
            return StatusResult.success(fakeContractNegotiation());
        }

        @Override
        public void enqueueCommand(ContractNegotiationCommand command) {
        }
    }
}
