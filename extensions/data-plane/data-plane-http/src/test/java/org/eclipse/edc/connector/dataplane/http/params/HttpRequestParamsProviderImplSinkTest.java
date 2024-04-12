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

package org.eclipse.edc.connector.dataplane.http.params;

import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HttpRequestParamsProviderImplSinkTest {

    private final HttpRequestParamsProvider provider = new HttpRequestParamsProviderImpl(mock(Vault.class), new JacksonTypeManager());

    @Test
    void shouldMapDataFlowRequestToHttpRequest() {
        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://destination")
                .method("test-method")
                .path("test-path")
                .queryParams("foo=bar")
                .contentType("test/content-type")
                .nonChunkedTransfer(true)
                .build();
        var dataFlowRequest = DataFlowStartMessage.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(dummyAddress())
                .destinationDataAddress(destination)
                .build();

        var params = provider.provideSinkParams(dataFlowRequest);

        assertThat(params.getMethod()).isEqualTo("test-method");
        assertThat(params.getBaseUrl()).isEqualTo("http://destination");
        assertThat(params.getPath()).isEqualTo("test-path");
        assertThat(params.getQueryParams()).isEqualTo(null); // query is not mapped to destination
        assertThat(params.getBody()).isEqualTo(null); // body is null because it will be set by the HttpDataSink
        assertThat(params.getContentType()).isEqualTo("test/content-type");
        assertThat(params.isNonChunkedTransfer()).isTrue();
    }

    @Test
    void shouldMapDataFlowRequestToHttpRequest_withDefaultValues() {
        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://destination")
                .build();
        var dataFlowRequest = DataFlowStartMessage.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(dummyAddress())
                .destinationDataAddress(destination)
                .build();

        var params = provider.provideSinkParams(dataFlowRequest);

        assertThat(params.getMethod()).isEqualTo("POST");
        assertThat(params.getBaseUrl()).isEqualTo("http://destination");
        assertThat(params.getPath()).isEqualTo(null);
        assertThat(params.getQueryParams()).isEqualTo(null);
        assertThat(params.getBody()).isEqualTo(null);
        assertThat(params.getContentType()).isEqualTo("application/octet-stream");
        assertThat(params.isNonChunkedTransfer()).isFalse();
    }

    private HttpDataAddress dummyAddress() {
        return HttpDataAddress.Builder.newInstance().baseUrl("http://source").build();
    }
}
