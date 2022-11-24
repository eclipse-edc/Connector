/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.service.dataaddress;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataAddressValidatorImplTest {

    @Test
    void shouldFail_whenHttpDataBaseUrlNotValid() {
        var validator = new DataAddressValidatorImpl();

        var dataAddress = DataAddress.Builder.newInstance().property("type", "HttpData").property("baseUrl", "not-a-valid-url").build();

        var result = validator.validate(dataAddress);

        assertThat(result.failed()).isTrue();
    }
}
