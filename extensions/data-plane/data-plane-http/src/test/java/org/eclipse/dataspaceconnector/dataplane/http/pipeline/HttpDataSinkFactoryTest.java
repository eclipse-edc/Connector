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

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.http.schema.HttpDataSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.http.schema.HttpDataSchema.ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpDataSinkFactoryTest {
    private HttpDataSinkFactory factory;

    @Test
    void verifyCanHandle() {
        DataFlowRequest httpRequest = createRequest(HttpDataSchema.TYPE).build();
        DataFlowRequest nonHttpRequest = createRequest("Unknown").build();

        assertThat(factory.canHandle(httpRequest)).isTrue();
        assertThat(factory.canHandle(nonHttpRequest)).isFalse();
    }

    @Test
    void verifyCreateSource() {
        var dataAddress = DataAddress.Builder.newInstance().property(ENDPOINT, "http://example.com").type(HttpDataSchema.TYPE).build();
        DataFlowRequest validRequest = createRequest(HttpDataSchema.TYPE).sourceDataAddress(dataAddress).build();
        DataFlowRequest missingEndpointRequest = createRequest("Unknown").build();

        assertThat(factory.createSink(validRequest)).isNotNull();
        assertThrows(EdcException.class, () -> factory.createSink(missingEndpointRequest));
    }

    @BeforeEach
    void setUp() {
        factory = new HttpDataSinkFactory(mock(OkHttpClient.class), mock(ExecutorService.class), mock(Monitor.class));
    }


}
