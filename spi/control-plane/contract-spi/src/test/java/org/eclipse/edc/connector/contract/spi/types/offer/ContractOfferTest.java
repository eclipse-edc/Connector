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

package org.eclipse.edc.connector.contract.spi.types.offer;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.byLessThan;

class ContractOfferTest {

    @Test
    void verifyRequiredFields() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().build()).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().id("some-id")
                .asset(Asset.Builder.newInstance().id("test-assetId").build())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Policy must not be null");
    }


    @Test
    void verifyAssetNotNull() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().id("some-id")
                .policy(Policy.Builder.newInstance().build())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Asset must not be null");
    }

    @Test
    void verifyDefaultContractValidity() {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("test").build())
                .build();

        assertThat(contractOffer.getContractEnd()).isCloseTo(ZonedDateTime.now().plusYears(1), byLessThan(1, ChronoUnit.SECONDS));
    }
}
