/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.dataspaceconnector.contract.negotiation.store;

import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiation createNegotiation() {
        return createNegotiation(ContractNegotiationStates.UNSAVED);
    }

    public static ContractNegotiation createNegotiation(ContractNegotiationStates state) {
        return createNegotiation(UUID.randomUUID().toString(), state);
    }

    public static ContractNegotiation createNegotiation(String id, ContractNegotiationStates state) {
        return createNegotiationBuilder(id)
                .state(state.code())
                .contractAgreement(createContractBuilder().build())
                .build();
    }

    public static ContractNegotiation createNegotiation(String id, ContractAgreement agreement) {
        return createNegotiationBuilder(id).contractAgreement(agreement).build();
    }

    public static ContractNegotiation.Builder createNegotiationBuilder(String id) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .correlationId(UUID.randomUUID().toString())
                .counterPartyId("test-counterparty-1")
                .counterPartyAddress("test-counterparty-address")
                .protocol("test-protocol")
                .stateCount(1);
    }

    public static ContractAgreement.Builder createContractBuilder() {
        return createContractBuilder("1:2");
    }

    public static ContractAgreement.Builder createContractBuilder(String id) {
        return ContractAgreement.Builder.newInstance()
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(Instant.now().getEpochSecond())
                .id(id);
    }

    public static ContractNegotiationDocument generateDocument() {
        return generateDocument(createNegotiation());
    }

    public static ContractNegotiationDocument generateDocument(ContractNegotiation negotiation) {
        return new ContractNegotiationDocument(negotiation, "test-part-key");
    }
}
