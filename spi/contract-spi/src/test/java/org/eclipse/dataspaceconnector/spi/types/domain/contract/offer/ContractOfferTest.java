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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractOfferTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void verifyMutualExclusivity() {
        var p = ContractOffer.Builder.newInstance().id("test-offer");

        assertThatThrownBy(() -> p.asset(Asset.Builder.newInstance().id("test-asset").build())
                .assetId("another-asset").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("asset and assetId are mutually exclusive");

        assertThatThrownBy(() -> p.policy(Policy.Builder.newInstance().build())
                .policyId("some-policy").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("policy and policyId are mutually exclusive");
    }

    @Test
    void verifyRequiredFields() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().build()).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().id("some-id")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either policy or policyId must be set");

        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().id("some-id")
                .assetId("test-assetId")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either policy or policyId must be set");
    }
}