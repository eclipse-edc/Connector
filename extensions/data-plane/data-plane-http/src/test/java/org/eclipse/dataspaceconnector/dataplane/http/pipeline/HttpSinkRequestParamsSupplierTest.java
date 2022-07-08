/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpMethod;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HttpSinkRequestParamsSupplierTest {

    private static final Faker FAKER = new Faker();

    private HttpSinkRequestParamsSupplier supplier;

    @BeforeEach
    public void setUp() {
        supplier = new HttpSinkRequestParamsSupplier(null);
    }

    @Test
    void selectAddress() {
        var source = mock(DataAddress.class);
        var destination = mock(DataAddress.class);
        var request = DataFlowRequest.Builder.newInstance()
                .processId(FAKER.internet().uuid())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .build();

        var result = supplier.selectAddress(request);

        assertThat(result).isEqualTo(destination);
    }

    @Test
    void extractMethod() {
        var method = FAKER.lorem().word();
        var address = HttpDataAddress.Builder.newInstance()
                .method(method)
                .build();

        var result = supplier.extractMethod(address, null);

        assertThat(result).isEqualTo(method);
    }

    @Test
    void extractMethodDefault() {
        var address = HttpDataAddress.Builder.newInstance()
                .build();

        var result = supplier.extractMethod(address, null);

        assertThat(result).isEqualTo(HttpMethod.POST.name());
    }

    @Test
    void extractPath() {
        var path = FAKER.lorem().word();
        var address = HttpDataAddress.Builder.newInstance()
                .path(path)
                .build();

        var result = supplier.extractPath(address, null);

        assertThat(result).isEqualTo(path);
    }

    @Test
    void extractQueryParams() {
        assertThat(supplier.extractPath(mock(HttpDataAddress.class), null)).isNull();
    }

    @Test
    void extractContentType() {
        var contentType = FAKER.lorem().word();
        var address = HttpDataAddress.Builder.newInstance()
                .contentType(contentType)
                .build();

        var result = supplier.extractContentType(address, null);

        assertThat(result).isEqualTo(contentType);
    }

    @Test
    void extractBody() {
        assertThat(supplier.extractPath(mock(HttpDataAddress.class), null)).isNull();
    }
}