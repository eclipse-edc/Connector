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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createDataAddress;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.NAME;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpDataSourceFactoryTest {

    private static final String TEST_ENDPOINT = "http://example.com";

    private HttpDataSourceFactory factory;

    @Test
    void verifyCanHandle() {
        assertThat(factory.canHandle(createRequest(TYPE).build())).isTrue();
    }

    @Test
    void verifyCannotHandle() {
        assertThat(factory.canHandle(createRequest("dummy").build())).isFalse();
    }

    @Test
    void verifyValidation_success() {
        var request = defaultRequest(defaultDataAddress());

        assertThat(factory.validate(request).succeeded()).isTrue();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidRequests")
    void verifyValidation_failure(String name, DataFlowRequest request) {
        assertThat(factory.validate(request).failed()).isTrue();
    }

    @Test
    void verifyCreateSource_success() {
        var request = defaultRequest(defaultDataAddress());

        assertThat(factory.createSource(request)).isNotNull();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidRequests")
    void verifyCreateSource_failure(String name, DataFlowRequest request) {
        assertThrows(EdcException.class, () -> factory.createSource(request));
    }

    private static DataAddress defaultDataAddress() {
        return DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, TEST_ENDPOINT)
                .property(NAME, "foo.json")
                .build();
    }

    private static DataFlowRequest defaultRequest(DataAddress address) {
        return defaultRequest(address, Map.of());
    }

    private static DataFlowRequest defaultRequest(DataAddress address, Map<String, String> additional) {
        var properties = new HashMap<String, String>();
        properties.put(METHOD, "GET");
        properties.putAll(additional);
        return createRequest(TYPE).sourceDataAddress(address).properties(properties).build();
    }

    /**
     * Serves some invalid {@link DataFlowRequest}.
     */
    private static Stream<Arguments> provideInvalidRequests() {
        var validAddress = defaultDataAddress();
        var missingEndpoint = createDataAddress(TYPE, Map.of()).build();
        return Stream.of(
                Arguments.of("MISSING METHOD", createRequest(Map.of(), validAddress, validAddress).build()),
                Arguments.of("MISSING ENDPOINT", defaultRequest(missingEndpoint))
        );
    }

    @BeforeEach
    void setUp() {
        factory = new HttpDataSourceFactory(mock(OkHttpClient.class), new RetryPolicy<>(), mock(Monitor.class));
    }
}
