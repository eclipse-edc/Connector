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

package org.eclipse.edc.spi.types.domain.edr;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EndpointDataAddressConstantsTest {

    @Test
    void from_shouldConvertEdrToDataAddress() {

        var inputEdr = getEndpointDataReference();
        var dataAddress = EndpointDataAddressConstants.from(inputEdr);


        assertThat(dataAddress.getProperties()).containsAllEntriesOf(inputEdr.getProperties());
        assertThat(dataAddress.getType()).isEqualTo(EndpointDataReference.EDR_SIMPLE_TYPE);
        assertThat(dataAddress.getProperty(EndpointDataReference.ID)).isEqualTo(inputEdr.getId());
        assertThat(dataAddress.getProperty(EndpointDataReference.AUTH_KEY)).isEqualTo(inputEdr.getAuthKey());
        assertThat(dataAddress.getProperty(EndpointDataReference.AUTH_CODE)).isEqualTo(inputEdr.getAuthCode());
        assertThat(dataAddress.getProperty(EndpointDataReference.ENDPOINT)).isEqualTo(inputEdr.getEndpoint());

    }

    @Test
    void to_shouldConvertDataAddressToEdr() {

        var inputEdr = getEndpointDataReference();
        var dataAddress = EndpointDataAddressConstants.from(inputEdr);

        var outputEdrResult = EndpointDataAddressConstants.to(dataAddress);

        assertThat(outputEdrResult)
                .extracting(Result::getContent)
                .usingRecursiveComparison()
                .isEqualTo(inputEdr);
    }

    @Test
    void to_shouldFailToConvertDataAddressToEdr() {

        var dataAddress = DataAddress.Builder.newInstance().type("wrong").build();

        var outputEdrResult = EndpointDataAddressConstants.to(dataAddress);

        assertThat(outputEdrResult.failed()).isTrue();
    }

    private EndpointDataReference getEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("some.test.endpoint")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .properties(Map.of("key1", "value1"))
                .build();
    }

}
