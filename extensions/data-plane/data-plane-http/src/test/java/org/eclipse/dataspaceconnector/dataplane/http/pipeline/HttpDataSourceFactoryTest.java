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

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.NAME;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpDataSourceFactoryTest {
    private HttpDataSourceFactory factory;

    @Test
    void verifyCanHandle() {
        DataFlowRequest httpRequest = createRequest(TYPE).build();
        DataFlowRequest nonHttpRequest = createRequest("Unknown").build();

        assertThat(factory.canHandle(httpRequest)).isTrue();
        assertThat(factory.canHandle(nonHttpRequest)).isFalse();
    }

    @Test
    void verifyValidation_success() {
        var dataAddress = DataAddress.Builder.newInstance().property(ENDPOINT, "http://example.com").property(NAME, "foo").type(TYPE).build();
        var validRequest = createRequest(TYPE).sourceDataAddress(dataAddress).build();
        assertThat(factory.validate(validRequest).succeeded()).isTrue();
    }

    @Test
    void verifyValidation_failsBecauseMissingEndpoint() {
        var missingEndpointRequest = createRequest("Unknown").build();
        assertThat(factory.validate(missingEndpointRequest).failed()).isTrue();
    }

    @Test
    void verifyValidation_failsBecauseMissingName() {
        var missingNameAddress = DataAddress.Builder.newInstance().property(ENDPOINT, "http://example.com").type(TYPE).build();
        var missingNameRequest = createRequest(TYPE).sourceDataAddress(missingNameAddress).build();
        assertThat(factory.validate(missingNameRequest).failed()).isTrue();
    }

    @Test
    void verifyValidation_failsBecauseMissingAuthCode() {
        var missingAuthCodeAddress = DataAddress.Builder.newInstance().property(ENDPOINT, "http://example.com").type(TYPE).build();
        var missingAuthCodeRequest = createRequest(TYPE).sourceDataAddress(missingAuthCodeAddress)
                .properties(Map.of(AUTHENTICATION_KEY, "Api-Key"))
                .build();
        assertThat(factory.validate(missingAuthCodeRequest).failed()).isTrue();
    }

    @Test
    void verifyCreateSource() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "http://example.com")
                .property(NAME, "foo.json")
                .build();


        var missingEndpointAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "http://example.com")
                .build();

        var missingNameAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(NAME, "foo.json")
                .build();

        var validRequest = createRequest(TYPE).sourceDataAddress(dataAddress).build();
        var missingEndpointRequest = createRequest(TYPE).sourceDataAddress(missingEndpointAddress).build();
        var missingNameRequest = createRequest(TYPE).sourceDataAddress(missingNameAddress).build();


        assertThat(factory.createSource(validRequest)).isNotNull();
        assertThrows(EdcException.class, () -> factory.createSource(missingEndpointRequest));
        assertThrows(EdcException.class, () -> factory.createSource(missingNameRequest));
    }

    @BeforeEach
    void setUp() {
        factory = new HttpDataSourceFactory(mock(OkHttpClient.class), new RetryPolicy<>(), mock(Monitor.class));
    }

}
