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
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.offer;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractOfferTest {

    private static final String ASSET_ID = "asset-id";

    @Test
    void success() {
        assertThatNoException().isThrownBy(() -> ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create(UUID.randomUUID().toString(), ASSET_ID).toString())
                .policy(Policy.Builder.newInstance().target(ASSET_ID).build())
                .assetId(ASSET_ID)
                .build());
    }

    @Test
    void verifyIdNotNull() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyPolicyNotNull() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create(UUID.randomUUID().toString(), ASSET_ID).toString())
                .assetId(ASSET_ID)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Policy must not be null");
    }

    @Test
    void verifyAssetNotNull() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create(UUID.randomUUID().toString(), ASSET_ID).toString())
                .policy(Policy.Builder.newInstance().build())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Asset id must not be null");
    }

    @Test
    void verifyContractIdIsValid() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance()
                .id("offer-id")
                .policy(Policy.Builder.newInstance().build())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contract id should be in the form [definition-id]:[asset-id]:[UUID] but it was offer-id");
    }

    @Test
    void verifyAssetIdPartIsEqualToAssetId() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create(UUID.randomUUID().toString(), ASSET_ID).toString())
                .policy(Policy.Builder.newInstance().build())
                .assetId("another-asset-id")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asset id %s must be equal to asset id part of offer id %s".formatted("another-asset-id", ASSET_ID));
    }
}
