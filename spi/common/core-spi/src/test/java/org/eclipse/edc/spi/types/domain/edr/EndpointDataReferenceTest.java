/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.edr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class EndpointDataReferenceTest {

    @Test
    void buildMinimalEdr() {
        assertThatNoException().isThrownBy(() -> EndpointDataReference.Builder.newInstance()
                .endpoint("http://foo.bar")
                .id("id")
                .contractId("contractId")
                .build());
    }

    @Test
    void buildEdrWithAuth() {
        assertThatNoException().isThrownBy(() -> EndpointDataReference.Builder.newInstance()
                .endpoint("http://foo.bar")
                .authKey("authKey")
                .authCode("authCode")
                .id("id")
                .contractId("contractId")
                .build());
    }

    @Test
    void assertIdMandatory() {
        assertThatNullPointerException().isThrownBy(() -> EndpointDataReference.Builder.newInstance()
                        .endpoint("http://foo.bar")
                        .contractId("contractId")
                        .build())
                .withMessageContaining("id");
    }

    @Test
    void assertContractIdMandatory() {
        assertThatNullPointerException().isThrownBy(() -> EndpointDataReference.Builder.newInstance()
                        .endpoint("http://foo.bar")
                        .id("id")
                        .build())
                .withMessageContaining("contractId");
    }
}