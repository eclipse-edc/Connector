/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage.Type.COUNTER_OFFER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage.Type.INITIAL;

class ContractRequestMessageTest {

    public static final String CALLBACK_ADDRESS = "http://test.com";
    public static final String ID = "id1";
    public static final String ASSET_ID = "asset1";
    public static final String PROTOCOL = "DPS";

    @Test
    void verify_noCallbackNeededForCounterOffer() {
        ContractRequestMessage.Builder.newInstance()
                .callbackAddress("http://any")
                .type(COUNTER_OFFER)
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .protocol(PROTOCOL)
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(ID)
                        .assetId(ASSET_ID)
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
    }

    @Test
    void verify_contractOfferIdOrContractOffer() {
        ContractRequestMessage.Builder.newInstance()
                .callbackAddress("http://any")
                .type(INITIAL)
                .callbackAddress("any")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .protocol(PROTOCOL)
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(ID)
                        .assetId(ASSET_ID)
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .counterPartyAddress(CALLBACK_ADDRESS)
                .build();

        // verify no contract offer is set
        assertThatThrownBy(() -> ContractRequestMessage.Builder.newInstance()
                .callbackAddress("http://any")
                .type(INITIAL)
                .callbackAddress("any")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .protocol(PROTOCOL)
                .counterPartyAddress(CALLBACK_ADDRESS)
                .build()).isInstanceOf(NullPointerException.class).hasMessageContaining("contractOffer");

    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(ID)
                .assetId(ASSET_ID)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
