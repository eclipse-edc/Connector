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

package org.eclipse.dataspaceconnector.spi.types.domain.contract;

import org.eclipse.dataspaceconnector.spi.contract.ContractId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractIdTest {

    @Test
    void isValid() {
        var id = ContractId.parse("thisis:avalidid");

        assertThat(id.isValid()).isTrue();
    }

    @Test
    void isValid_falseIfNoColonPresent() {
        var id = ContractId.parse("thisisaninvalidid");

        assertThat(id.isValid()).isFalse();
    }

    @Test
    void isValid_falseIfTooManyColonsPresent() {
        var id = ContractId.parse("thisis:an:invalidid");

        assertThat(id.isValid()).isFalse();
    }

    @Test
    void definitionPart_returnsTheFirstPartOfTheId() {
        var id = ContractId.parse("definitionPart:agreementPart");

        assertThat(id.definitionPart()).isEqualTo("definitionPart");
    }
}
