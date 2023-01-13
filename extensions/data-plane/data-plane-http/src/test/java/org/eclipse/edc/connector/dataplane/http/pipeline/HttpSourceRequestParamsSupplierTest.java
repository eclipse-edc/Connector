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

import org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class HttpSourceRequestParamsSupplierTest {

    private HttpSourceRequestParamsSupplier supplier;

    @BeforeEach
    public void setUp() {
        supplier = new HttpSourceRequestParamsSupplier(null, new TypeManager());
    }

    @Test
    void selectAddress() {
        var source = mock(DataAddress.class);
        var request = createRequest(source);

        var result = supplier.selectAddress(request);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void extractMethod() {
        var method = "test-method";
        var address = HttpDataAddress.Builder.newInstance()
                .proxyMethod(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.METHOD, method));

        var result = supplier.extractMethod(address, request);

        assertThat(result).isEqualTo(method);
    }

    @Test
    void extractMethodThrowsIfMissingMethod() {
        var address = HttpDataAddress.Builder.newInstance()
                .proxyMethod(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> supplier.extractMethod(address, request));
    }

    @Test
    void extractMethodDefault() {
        var method = "test-method";
        var address = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.METHOD, method));

        var result = supplier.extractMethod(address, request);

        assertThat(result).isEqualTo("GET");
    }

    @Test
    void extractPath() {
        var path = "test-path";
        var address = HttpDataAddress.Builder.newInstance()
                .proxyPath(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.PATH, path));

        var result = supplier.extractPath(address, request);

        assertThat(result).isEqualTo(path);
    }

    @Test
    void extractPathEmpty() {
        var address = HttpDataAddress.Builder.newInstance()
                .proxyPath(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address);

        var result = supplier.extractPath(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractPathFilteredByProxy() {
        var path = "test-path";
        var address = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.PATH, path));

        var result = supplier.extractPath(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractQueryParams_onlyInRequest() {
        var queryParams = "foo=bar&hello=world";
        var address = HttpDataAddress.Builder.newInstance()
                .proxyQueryParams(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.QUERY_PARAMS, queryParams));

        var result = supplier.extractQueryParams(address, request);

        assertThat(result).isEqualTo(queryParams);
    }

    @Test
    void extractQueryParams_onlyInDataAddress() {
        var queryParams = "foo=bar&hello=world";
        var address = HttpDataAddress.Builder.newInstance()
                .queryParams(queryParams)
                .proxyQueryParams(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of());

        var result = supplier.extractQueryParams(address, request);

        assertThat(result).isEqualTo(queryParams);
    }

    @Test
    void extractQueryParams_dataAddressAndRequest() {
        var queryParams = "foo=bar";
        var address = HttpDataAddress.Builder.newInstance()
                .queryParams(queryParams)
                .proxyQueryParams(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.QUERY_PARAMS, "hello=world"));

        var result = supplier.extractQueryParams(address, request);

        assertThat(result).isEqualTo("foo=bar&hello=world");
    }

    @Test
    void extractQueryParamsFilteredByProxy() {
        var queryParams = "foo=bar&hello=world";
        var address = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.QUERY_PARAMS, queryParams));

        var result = supplier.extractQueryParams(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractQueryParamsEmpty() {
        var address = HttpDataAddress.Builder.newInstance()
                .proxyQueryParams(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address);

        var result = supplier.extractQueryParams(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractContentType() {
        var contentType = "test/content-type";
        var address = HttpDataAddress.Builder.newInstance()
                .proxyBody(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.MEDIA_TYPE, contentType));

        var result = supplier.extractContentType(address, request);

        assertThat(result).isEqualTo(contentType);
    }

    @Test
    void extractContentTypeEmpty() {
        var address = HttpDataAddress.Builder.newInstance()
                .proxyBody(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address);

        var result = supplier.extractContentType(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractContentFilteredByProxy() {
        var contentType = "test-contenttype";
        var address = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.MEDIA_TYPE, contentType));

        var result = supplier.extractContentType(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractBody() {
        var body = "Test Body";
        var address = HttpDataAddress.Builder.newInstance()
                .proxyBody(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.BODY, body));

        var result = supplier.extractBody(address, request);

        assertThat(result).isEqualTo(body);
    }

    @Test
    void extractBodyEmpty() {
        var address = HttpDataAddress.Builder.newInstance()
                .proxyBody(Boolean.TRUE.toString())
                .build();
        var request = createRequest(address);

        var result = supplier.extractBody(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractBodyFilteredByProxy() {
        var body = "Test Body";
        var address = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(address, Map.of(DataFlowRequestSchema.BODY, body));

        var result = supplier.extractBody(address, request);

        assertThat(result).isNull();
    }

    @Test
    void extractNonChunkedTransfer() {
        var chunked = new Random().nextBoolean();
        var address = HttpDataAddress.Builder.newInstance()
                .nonChunkedTransfer(chunked)
                .build();

        var result = supplier.extractNonChunkedTransfer(address);

        assertThat(result).isFalse();
    }

    private DataFlowRequest createRequest(DataAddress source) {
        return createRequest(source, Map.of());
    }

    private DataFlowRequest createRequest(DataAddress source, Map<String, String> props) {
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(mock(DataAddress.class))
                .properties(props)
                .build();
    }

}
