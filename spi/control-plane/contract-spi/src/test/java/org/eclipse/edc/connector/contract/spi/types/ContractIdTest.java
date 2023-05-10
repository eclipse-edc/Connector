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

import org.eclipse.edc.connector.contract.spi.ContractId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContractIdTest {

    @ParameterizedTest
    @ValueSource(strings = { "this:is:valid", "this:is:valid:" })
    void isValid(String idString) {
        var id = ContractId.parse(idString);

        assertThat(id.isValid()).isTrue();
    }

    @Test
    void isValid_tooFewParts() {
        assertThat(ContractId.parse("thisis:invalid").isValid()).isFalse();
    }

    @Test
    void isValid_falseIfNoColonPresent() {
        var id = ContractId.parse("thisisaninvalidid");

        assertThat(id.isValid()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "thisis:a:very:invalidid", ":this:is:invalid" })
    void isValid_falseIfTooManyColonsPresent(String idString) {
        var id = ContractId.parse(idString);
        assertThat(id.isValid()).isFalse();
    }

    @Test
    void definitionPart_returnsTheFirstPartOfTheId() {
        var id = ContractId.parse("definitionPart:assetPart:agreementPart");

        assertThat(id.definitionPart()).isEqualTo("definitionPart");
    }

    @Test
    void assetIdPart_returnsSecondPartOfTheId() {
        var id = ContractId.parse("definitionPart:assetPart:agreementPart");

        assertThat(id.assetIdPart()).isEqualTo("assetPart");
    }
}
