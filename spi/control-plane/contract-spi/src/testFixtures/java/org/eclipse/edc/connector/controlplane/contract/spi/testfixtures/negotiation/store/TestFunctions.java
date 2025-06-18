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

package org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.negotiation.store;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.ProtocolMessages;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestFunctions {

    private TestFunctions() {
    }

    public static ContractNegotiation createNegotiation(String id) {
        return createNegotiationBuilder(id)
                .build();
    }

    public static ContractNegotiation createNegotiation(String id, ContractAgreement agreement) {
        return createNegotiationBuilder(id)
                .contractAgreement(agreement)
                .build();
    }

    public static ContractAgreement createAgreement(ContractOfferId contractOfferId) {
        return createAgreementBuilder(contractOfferId.toString())
                .build();
    }

    public static ContractAgreement.Builder createAgreementBuilder(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("provider")
                .consumerId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(createPolicy())
                .contractSigningDate(Instant.now().getEpochSecond());
    }

    public static ContractNegotiation.Builder createNegotiationBuilder(String id) {
        return ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .contractAgreement(null)
                .correlationId("corr-" + id)
                .state(ContractNegotiationStates.REQUESTED.code())
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .callbackAddresses(List.of(createCallbackAddress()))
                .protocol("protocol")
                .protocolMessages(new ProtocolMessages());
    }

    public static CallbackAddress createCallbackAddress() {
        return CallbackAddress.Builder.newInstance()
                .uri("local://test")
                .events(Set.of("contract.negotiation.initiated"))
                .build();
    }

    public static Policy createPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type("use")
                                .build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("foo"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("bar"))
                                .build())
                        .build())
                .build();
    }
}
