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

package org.eclipse.edc.connector.transfer.edr;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataAddressToEndpointDataReferenceTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);

    private final DataAddressToEndpointDataReferenceTransformer transformer = new DataAddressToEndpointDataReferenceTransformer();

    @Test
    void transform_success() {
        var address = DataAddress.Builder.newInstance()
                .type(EndpointDataReference.EDR_SIMPLE_TYPE)
                .property(EndpointDataReference.ENDPOINT, "some.test.endpoint")
                .property(EndpointDataReference.AUTH_KEY, "test-authkey")
                .property(EndpointDataReference.AUTH_CODE, UUID.randomUUID().toString())
                .property(EndpointDataReference.ID, UUID.randomUUID().toString())
                .build();

        var edr = transformer.transform(address, context);

        assertThat(edr)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(EndpointDataReference.Builder.newInstance()
                        .endpoint(address.getProperty(EndpointDataReference.ENDPOINT))
                        .authKey(address.getProperty(EndpointDataReference.AUTH_KEY))
                        .authCode(address.getProperty(EndpointDataReference.AUTH_CODE))
                        .id(address.getProperty(EndpointDataReference.ID))
                        .build());
    }

    @Test
    void transform_failsIfTypeIsNotEdr() {
        var address = DataAddress.Builder.newInstance().type("wrong").build();

        var edr = transformer.transform(address, context);

        assertThat(edr).isNull();
        verify(context).reportProblem(anyString());
    }
}