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

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HttpSinkRequestParamsSupplierTest {


    private HttpSinkRequestParamsSupplier supplier;

    @BeforeEach
    public void setUp() {
        supplier = new HttpSinkRequestParamsSupplier(null, new TypeManager());
    }

    @Test
    void selectAddress() {
        var source = mock(DataAddress.class);
        var destination = mock(DataAddress.class);
        var request = DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .build();

        var result = supplier.selectAddress(request);

        assertThat(result).isEqualTo(destination);
    }

    @Test
    void extractMethod() {
        var method = "test-method";
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

        assertThat(result).isEqualTo("POST");
    }

    @Test
    void extractPath() {
        var path = "test-path";
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
        var contentType = "test/content-type";
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

    @Test
    void extractNonChunkedTransfer() {
        var chunked = new Random().nextBoolean();
        var address = HttpDataAddress.Builder.newInstance()
                .nonChunkedTransfer(chunked)
                .build();

        var result = supplier.extractNonChunkedTransfer(address);

        assertThat(result).isEqualTo(chunked);
    }
}
