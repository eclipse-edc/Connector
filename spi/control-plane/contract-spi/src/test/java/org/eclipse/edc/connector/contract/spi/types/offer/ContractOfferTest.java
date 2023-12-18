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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractOfferTest {

    @Test
    void verifyIdNotNull() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyPolicyNotNull() {
        assertThatThrownBy(() -> ContractOffer.Builder.newInstance().id("some-id").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Policy must not be null");
    }
}
