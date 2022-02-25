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
package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createHttpResponse;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpDataSinkFactoryTest {
    private HttpDataSinkFactory factory;
    private OkHttpClient httpClient;

    @Test
    void verifyCanHandle() {
        var httpRequest = createRequest(TYPE).build();
        var nonHttpRequest = createRequest("Unknown").build();

        assertThat(factory.canHandle(httpRequest)).isTrue();
        assertThat(factory.canHandle(nonHttpRequest)).isFalse();
    }

    @Test
    void verifyValidation() {
        var dataAddress = DataAddress.Builder.newInstance().property(ENDPOINT, "http://example.com").type(TYPE).build();
        var validRequest = createRequest(TYPE).destinationDataAddress(dataAddress).build();
        assertThat(factory.validate(validRequest).succeeded()).isTrue();

        var missingEndpointRequest = createRequest("Unknown").build();
        assertThat(factory.validate(missingEndpointRequest).failed()).isTrue();
    }

    @Test
    void verifyCreateSource() {
        var dataAddress = DataAddress.Builder.newInstance().property(ENDPOINT, "http://example.com").type(TYPE).build();
        var validRequest = createRequest(TYPE).destinationDataAddress(dataAddress).build();
        var missingEndpointRequest = createRequest("Unknown").build();

        assertThat(factory.createSink(validRequest)).isNotNull();
        assertThrows(EdcException.class, () -> factory.createSink(missingEndpointRequest));
    }

    @Test
    void verifyCreateAuthenticatingSource() throws InterruptedException, ExecutionException, IOException {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "http://example.com")
                .property(AUTHENTICATION_KEY, "x-api-key")
                .property(AUTHENTICATION_CODE, "123")
                .build();

        var validRequest = createRequest(TYPE).destinationDataAddress(dataAddress).build();

        var call = mock(Call.class);
        when(call.execute()).thenReturn(createHttpResponse().build());

        when(httpClient.newCall(isA(Request.class))).thenAnswer(r -> {
            assertThat(((Request) r.getArgument(0)).headers("x-api-key").get(0)).isEqualTo("123");  // verify auth header set
            return call;
        });

        var sink = factory.createSink(validRequest);

        var result = sink.transfer(new InputStreamDataSource("test", new ByteArrayInputStream("test".getBytes()))).get();

        assertThat(result.failed()).isFalse();

        verify(call).execute();
    }

    @BeforeEach
    void setUp() {
        httpClient = mock(OkHttpClient.class);
        factory = new HttpDataSinkFactory(httpClient, Executors.newFixedThreadPool(1), 5, mock(Monitor.class));
    }


}
