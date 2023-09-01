/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.contract.spi.types;

import org.eclipse.edc.connector.contract.spi.ContractOfferId;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ContractOfferIdTest {

    private final Base64.Encoder base64encoder = Base64.getEncoder();

    @Test
    void parseId_shouldSucceedWhenItsValid() {
        var base64representation = encodedContractId("definitionId", "assetId", "uuid");

        var result = ContractOfferId.parseId(base64representation);

        assertThat(result).isSucceeded().satisfies(it -> {
            assertThat(it.definitionPart()).isEqualTo("definitionId");
            assertThat(it.assetIdPart()).isEqualTo("assetId");
            assertThat(it.toString()).isEqualTo(base64representation);
        });
    }

    @Test
    void parseId_shouldNotDecodePartsIfTheyArentBase64() {
        var result = ContractOfferId.parseId("not:base64:" + UUID.randomUUID());

        assertThat(result).isSucceeded().satisfies(it -> {
            assertThat(it.definitionPart()).isEqualTo("not");
            assertThat(it.assetIdPart()).isEqualTo("base64");
        });
    }

    @Test
    void shouldNotParse_whenInputIsNull() {
        var result = ContractOfferId.parseId(null);

        assertThat(result).isFailed();
    }

    @Test
    void shouldNotParse_whenTooFewParts() {
        var result = ContractOfferId.parseId("this:isinvalid");

        assertThat(result).isFailed();
    }

    @Test
    void shouldNotParse_whenTooManyParts() {
        var result = ContractOfferId.parseId("this:is:not:valid");

        assertThat(result).isFailed();
    }

    private String encodedContractId(String definitionId, String assetId, String uuid) {
        return format("%s:%s:%s",
                base64encoder.encodeToString(definitionId.getBytes()),
                base64encoder.encodeToString(assetId.getBytes()),
                base64encoder.encodeToString(uuid.getBytes())
        );
    }
}
