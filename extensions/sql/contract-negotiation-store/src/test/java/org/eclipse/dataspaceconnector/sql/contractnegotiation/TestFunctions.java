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
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *
 */

package org.eclipse.dataspaceconnector.sql.contractnegotiation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiation createNegotiation(String id) {
        return ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .contractAgreement(null)
                .correlationId("corr-" + id)
                .state(ContractNegotiationStates.REQUESTED.code())
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();
    }

    public static ContractNegotiation createNegotiation(String id, ContractAgreement agreement) {
        return ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .contractAgreement(null)
                .contractAgreement(agreement)
                .correlationId("corr-" + id)
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();
    }

    public static ContractAgreement createContract(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(Instant.now().getEpochSecond())
                .build();
    }
}
