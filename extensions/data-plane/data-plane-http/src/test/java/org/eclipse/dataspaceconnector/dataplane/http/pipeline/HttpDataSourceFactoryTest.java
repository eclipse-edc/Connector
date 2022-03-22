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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createDataAddress;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.SECRET_NAME;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpDataSourceFactoryTest {

    private static final String TEST_SECRET_NAME = "secret-test";
    private static final String TEST_SECRET_VALUE = "token-test";

    private static final OkHttpClient HTTP_CLIENT = mock(OkHttpClient.class);
    private static final RetryPolicy<Object> RETRY_POLICY = new RetryPolicy<>();
    private static final Monitor MONITOR = mock(Monitor.class);

    private Vault vaultMock;
    private HttpDataSourceFactory factory;

    @BeforeEach
    void setUp() {
        vaultMock = mock(Vault.class);
        factory = new HttpDataSourceFactory(HTTP_CLIENT, RETRY_POLICY, MONITOR, vaultMock);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = "dummy")
    void verifyCannotHandle(String type) {
        assertThat(factory.canHandle(createRequest(type).build())).isFalse();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidRequests")
    void verifySourceValidationAndCreation_failure(String name, DataFlowRequest request) {
        assertThat(factory.validate(request).failed()).isTrue();
        assertThrows(EdcException.class, () -> factory.createSource(request));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideValidRequestsWithExpectedSource")
    void verifySourceValidationAndCreation(String name, DataFlowRequest request, HttpDataSource expectedSource) {
        when(vaultMock.resolveSecret(TEST_SECRET_NAME)).thenReturn(TEST_SECRET_VALUE);
        assertThat(factory.canHandle(request)).isTrue();
        assertThat(factory.validate(request).succeeded()).isTrue();
        var source = factory.createSource(request);
        assertThat(source).isNotNull();

        // validate the generated data source field by field using reflection
        Arrays.stream(HttpDataSource.class.getDeclaredFields()).forEach(f -> {
            f.setAccessible(true);
            try {
                assertThat(f.get(source)).isEqualTo(f.get(expectedSource));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        });
    }

    /**
     * Serves some invalid {@link DataFlowRequest}.
     */
    private static Stream<Arguments> provideInvalidRequests() {
        var endpoint = "http://example.com";
        var validAddress = createDataAddress(TYPE, Map.of(ENDPOINT, endpoint)).build();
        var missingEndpoint = createDataAddress(TYPE, Map.of()).build();
        return Stream.of(
                Arguments.of("MISSING METHOD", createRequest(Map.of(), validAddress, validAddress).build()),
                Arguments.of("MISSING ENDPOINT", createHttpRequest(missingEndpoint, "GET")),
                Arguments.of("UNHANDLED MEDIA TYPE", createHttpRequest(validAddress, "GET", Map.of(MEDIA_TYPE, "dummy", BODY, "body-test")))
        );
    }

    /**
     * Serves some valid {@link DataFlowRequest} with the associated expected {@link HttpDataSource} that must be generated.
     */
    private static Stream<Arguments> provideValidRequestsWithExpectedSource() {
        var endpoint = "http://example.com";
        var name = "foo.json";
        var authKey = "apikey-test";
        var mediaType = "application/json";
        var body = "test";
        var queryParams = "?foo=bar";
        var defaultDataAddress = createDataAddress(TYPE, Map.of(ENDPOINT, endpoint)).build();
        var dataAddressWithName = createDataAddress(TYPE, Map.of(ENDPOINT, endpoint, NAME, name)).build();
        var dataAddressWithHardcodedAuth = createDataAddress(TYPE, Map.of(ENDPOINT, endpoint, AUTHENTICATION_KEY, authKey, AUTHENTICATION_CODE, TEST_SECRET_VALUE)).build();
        var dataAddressWithSecretBasedAuth = createDataAddress(TYPE, Map.of(ENDPOINT, endpoint, AUTHENTICATION_KEY, authKey, SECRET_NAME, TEST_SECRET_NAME)).build();
        var dataAddressWithSecretNameOnly = createDataAddress(TYPE, Map.of(ENDPOINT, endpoint, SECRET_NAME, TEST_SECRET_NAME)).build();

        var basicRequest = createHttpRequest(defaultDataAddress, "GET");
        var expectedBasicSource = defaultHttpSource(basicRequest).build();

        var requestWithName = createHttpRequest(dataAddressWithName, "GET");
        var sourceWithName = defaultHttpSource(requestWithName).name(name).build();

        var requestWithQueryParams = createHttpRequest(defaultDataAddress, "GET", Map.of(QUERY_PARAMS, queryParams));
        var sourceWithQueryParams = defaultHttpSource(requestWithQueryParams).queryParams(queryParams).build();

        var requestWithSecretBasedAuth = createHttpRequest(dataAddressWithSecretBasedAuth, "GET");
        var sourceWithAuthHeaderFromSecret = defaultHttpSource(requestWithSecretBasedAuth).header(authKey, TEST_SECRET_VALUE).build();

        var requestWithAuthHardcoded = createHttpRequest(dataAddressWithHardcodedAuth, "GET");
        var sourceWithAuthHeaderFromHardcode = defaultHttpSource(requestWithAuthHardcoded).header(authKey, TEST_SECRET_VALUE).build();

        var requestWithBody = createHttpRequest(defaultDataAddress, "POST", Map.of(MEDIA_TYPE, mediaType, BODY, body));
        var sourceWithBody = defaultHttpSource(requestWithBody).requestBody(MediaType.get(mediaType), body).build();

        var requestWithIncompleteHeader = createHttpRequest(dataAddressWithSecretNameOnly, "GET");
        var expectedSourceWithoutHeader = defaultHttpSource(requestWithIncompleteHeader).build();

        var requestWithMediaTypeOnly = createHttpRequest(defaultDataAddress, "PUT", Map.of(ENDPOINT, endpoint, BODY, body));
        var expectedSourceWithoutBody = defaultHttpSource(requestWithMediaTypeOnly).build();

        return Stream.of(
                Arguments.of("BASIC SOURCE", basicRequest, expectedBasicSource),
                Arguments.of("WITH NAME", requestWithName, sourceWithName),
                Arguments.of("WITH QUERY PARAMS", requestWithQueryParams, sourceWithQueryParams),
                Arguments.of("WITH AUTH SECRET", requestWithSecretBasedAuth, sourceWithAuthHeaderFromSecret),
                Arguments.of("WITH AUTH HARDCODED", requestWithAuthHardcoded, sourceWithAuthHeaderFromHardcode),
                Arguments.of("WITH BODY", requestWithBody, sourceWithBody),
                Arguments.of("WITHOUT AUTH KEY", requestWithIncompleteHeader, expectedSourceWithoutHeader),
                Arguments.of("WITHOUT MEDIA TYPE", requestWithMediaTypeOnly, expectedSourceWithoutBody)
        );
    }

    private static DataFlowRequest createHttpRequest(DataAddress address, String method) {
        return createHttpRequest(address, method, Map.of());
    }

    private static DataFlowRequest createHttpRequest(DataAddress address, String method, Map<String, String> additional) {
        var properties = new HashMap<String, String>();
        properties.put(METHOD, method);
        properties.putAll(additional);
        return createRequest(TYPE).sourceDataAddress(address).properties(properties).build();
    }

    private static HttpDataSource.Builder defaultHttpSource(DataFlowRequest request) {
        return HttpDataSource.Builder.newInstance()
                .sourceUrl(request.getSourceDataAddress().getProperty(ENDPOINT))
                .method(request.getProperties().get(METHOD))
                .requestId(request.getId())
                .httpClient(HTTP_CLIENT)
                .monitor(MONITOR)
                .retryPolicy(RETRY_POLICY);
    }
}
