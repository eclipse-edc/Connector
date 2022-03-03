/*
 *  Copyright (c) 2021-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *
 */

package org.eclipse.dataspaceconnector.contract.negotiation.store;

import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiation generateNegotiation() {
        return generateNegotiation(ContractNegotiationStates.UNSAVED);
    }

    public static ContractNegotiation generateNegotiation(ContractNegotiationStates state) {
        return generateNegotiation(UUID.randomUUID().toString(), state);
    }

    public static ContractNegotiation generateNegotiation(String id, ContractNegotiationStates state) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .correlationId(UUID.randomUUID().toString())
                .counterPartyId("test-counterparty-1")
                .counterPartyAddress("test-counterparty-address")
                .protocol("test-protocol")
                .stateCount(1)
                .contractAgreement(ContractAgreement.Builder.newInstance().id("1")
                        .providerAgentId("provider")
                        .consumerAgentId("consumer")
                        .asset(Asset.Builder.newInstance().build())
                        .policy(Policy.Builder.newInstance().build())
                        .contractStartDate(Instant.now().getEpochSecond())
                        .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                        .contractSigningDate(Instant.now().getEpochSecond())
                        .id("1:2").build())
                .state(state.code())
                .build();
    }

    public static ContractNegotiationDocument generateDocument() {
        return new ContractNegotiationDocument(generateNegotiation(), "test-part-key");
    }
}
