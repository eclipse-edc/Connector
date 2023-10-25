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

package org.eclipse.edc.validator.dataaddress;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.dataaddress.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.spi.dataaddress.HttpDataAddressSchema.HTTP_DATA_TYPE;

class HttpDataDataAddressValidatorImplTest {

    private final HttpDataDataAddressValidator validator = new HttpDataDataAddressValidator();

    @Test
    void shouldPass_whenHttpDataIsValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", HTTP_DATA_TYPE)
                .property(BASE_URL, "http://this.is/valid/url")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenHttpDataBaseUrlNotValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", HTTP_DATA_TYPE)
                .property(BASE_URL, "not-a-valid-url")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isFailed();
    }

}
