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
 *       Amadeus - test retrieval of auth code from vault
 *       Amadeus - add test for mapping of path segments
 *       Mercedes Benz Tech Innovation - add toggles for proxy behavior
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures;
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
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_BODY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_METHOD;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_PATH;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_QUERY_PARAMS;
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
        assertThat(factory.canHandle(HttpTestFixtures.createRequest(type).build())).isFalse();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidRequests")
    void verifySourceValidationAndCreation_failure(String name, TestInstance testInstance) {
        var request = testInstance.createRequest();

        assertThat(factory.validate(request).failed()).isTrue();
        assertThrows(EdcException.class, () -> factory.createSource(request));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideTestInstances")
    void verifySourceValidationAndCreation(String name, TestInstance testInstance) {
        when(vaultMock.resolveSecret(TEST_SECRET_NAME)).thenReturn(TEST_SECRET_VALUE);

        var request = testInstance.createRequest();
        var expectedSource = testInstance.createDataSource();

        assertThat(factory.canHandle(request)).isTrue();

        var test = factory.validate(request);

        assertThat(factory.validate(request).succeeded()).isTrue();
        var source = factory.createSource(request);
        assertThat(source).isNotNull();

        // validate the generated data source field by field using reflection
        Arrays.stream(HttpDataSource.class.getDeclaredFields()).forEach(f -> {
            f.setAccessible(true);
            try {
                assertThat(f.get(source)).isEqualTo(f.get(expectedSource));
            } catch (IllegalAccessException e) {
                throw new AssertionError("Comparison failed for field: " + f.getName());
            }
        });
    }

    /**
     * Serves some invalid {@link DataFlowRequest}.
     */
    private static Stream<Arguments> provideInvalidRequests() {
        var endpoint = "http://example.com";
        var authKey = "apikey-test";

        var missingEndpoint = TestInstance.newInstance()
                .method("GET", true);

        var incompleteHeader = TestInstance.newInstance()
                .method("GET", true)
                .authKey(authKey)
                .endpoint(endpoint);

        var unknownMediaType = TestInstance.newInstance()
                .method("POST", true)
                .body("dummy", "hello world!", true)
                .endpoint(endpoint);

        return Stream.of(
                Arguments.of("MISSING ENDPOINT", missingEndpoint),
                Arguments.of("INCOMPLETE HEADER", incompleteHeader),
                Arguments.of("UNHANDLED MEDIA TYPE", unknownMediaType)
        );
    }

    /**
     * Serves some valid {@link DataFlowRequest} with the associated expected {@link HttpDataSource} that must be generated.
     */
    private static Stream<Arguments> provideTestInstances() {
        var endpoint = "http://example.com";
        var name = "foo.json";
        var authKey = "apikey-test";
        var mediaType = "application/json";
        var body = "test";
        var queryParams = "?foo=bar";

        var get = TestInstance.newInstance()
                .method("GET", true)
                .endpoint(endpoint);

        var getWithPath = TestInstance.newInstance()
                .method("GET", true)
                .basePath("hello/world", true)
                .endpoint(endpoint);

        var ignorePathIfProxyDisabled = TestInstance.newInstance()
                .method("GET", true)
                .name("some/name")
                .basePath("hello/world", false)
                .endpoint(endpoint);

        var getWithName = TestInstance.newInstance()
                .method("GET", true)
                .name(name)
                .endpoint(endpoint);

        var getWithQueryParams = TestInstance.newInstance()
                .method("GET", true)
                .queryParams(queryParams, true)
                .endpoint(endpoint);

        var ignoreQueryParamsIfProxyDisabled = TestInstance.newInstance()
                .method("GET", true)
                .queryParams(queryParams, false)
                .endpoint(endpoint);

        var getWithSecret = TestInstance.newInstance()
                .method("GET", true)
                .authHeader(authKey, TEST_SECRET_NAME, TEST_SECRET_VALUE)
                .endpoint(endpoint);

        var getWithAuthCode = TestInstance.newInstance()
                .method("GET", true)
                .authHeader(authKey, TEST_SECRET_VALUE)
                .endpoint(endpoint);

        var post = TestInstance.newInstance()
                .method("POST", true)
                .body(mediaType, body, true)
                .endpoint(endpoint);

        var ignoreMethodIfProxyDisabled = TestInstance.newInstance()
                .method("POST", false)
                .body(mediaType, body, true)
                .endpoint(endpoint);


        var ignoreBodyWithoutMediaType = TestInstance.newInstance()
                .method("POST", true)
                .body(body, true)
                .endpoint(endpoint);

        var ignoreBodyIfProxyDisabled = TestInstance.newInstance()
                .method("POST", true)
                .body(mediaType, body, false)
                .endpoint(endpoint);

        return Stream.of(
                Arguments.of("GET", get),
                Arguments.of("GET WITH NAME", getWithName),
                Arguments.of("GET WITH PATH", getWithPath),
                Arguments.of("IGNORE PATH IF PROXY DISABLED", ignorePathIfProxyDisabled),
                Arguments.of("GET WITH QUERY PARAMS", getWithQueryParams),
                Arguments.of("IGNORE QUERY PARAMS IF PROXY DISABLED", ignoreQueryParamsIfProxyDisabled),
                Arguments.of("GET WITH SECRET", getWithSecret),
                Arguments.of("WITH AUTH CODE", getWithAuthCode),
                Arguments.of("POST", post),
                Arguments.of("IGNORE BODY WITHOUT MEDIA TYPE", ignoreBodyWithoutMediaType),
                Arguments.of("IGNORE BODY IF PROXY DISABLED", ignoreBodyIfProxyDisabled),
                Arguments.of("IGNORE METHOD IF PROXY DISABLED", ignoreMethodIfProxyDisabled)
        );
    }

    /**
     * One test instance for parameterized tests.
     */
    private static final class TestInstance {

        private static final DataAddress DUMMY_ADDRESS = DataAddress.Builder.newInstance()
                .type("dummy")
                .build();

        private final Map<String, String> props;
        private final DataAddress.Builder address;
        private final DataFlowRequest.Builder request;
        private final HttpDataSource.Builder source;

        private TestInstance() {
            var requestId = UUID.randomUUID().toString();
            props = new HashMap<>();
            address = DataAddress.Builder.newInstance().type(TYPE);
            request = DataFlowRequest.Builder.newInstance().processId("1").id(requestId);
            source = defaultHttpSource().requestId(requestId);
        }

        public static TestInstance newInstance() {
            return new TestInstance();
        }

        public DataFlowRequest createRequest() {
            return request.sourceDataAddress(address.build()).destinationDataAddress(DUMMY_ADDRESS).properties(props).build();
        }

        public HttpDataSource createDataSource() {
            return source.build();
        }

        public TestInstance endpoint(String endpoint) {
            this.address.property(ENDPOINT, endpoint);
            source.sourceUrl(endpoint);
            return this;
        }

        public TestInstance method(String method, boolean proxy) {
            props.put(METHOD, method);
            address.property(PROXY_METHOD, Boolean.toString(proxy));
            source.method(proxy ? method : "GET");
            return this;
        }

        public TestInstance name(String name) {
            this.address.property(NAME, name);
            source.name(name);
            return this;
        }

        public TestInstance authKey(String authKey) {
            this.address.property(AUTHENTICATION_KEY, authKey);
            return this;
        }

        public TestInstance authCode(String authCode) {
            this.address.property(AUTHENTICATION_CODE, authCode);
            return this;
        }

        public TestInstance authHeader(String key, String code) {
            this.address.property(AUTHENTICATION_KEY, key);
            this.address.property(AUTHENTICATION_CODE, code);
            source.header(key, code);
            return this;
        }

        public TestInstance authHeader(String key, String secret, String code) {
            this.address.property(AUTHENTICATION_KEY, key);
            this.address.property(SECRET_NAME, secret);
            source.header(key, code);
            return this;
        }

        public TestInstance body(String body, boolean proxy) {
            props.put(BODY, body);
            address.property(PROXY_BODY, Boolean.toString(proxy));
            return this;
        }

        public TestInstance body(String mediaType, String body, boolean proxy) {
            props.put(MEDIA_TYPE, mediaType);
            props.put(BODY, body);
            address.property(PROXY_BODY, Boolean.toString(proxy));
            if (proxy) {
                source.requestBody(MediaType.parse(mediaType), body);
            }
            return this;
        }

        public TestInstance basePath(String basePath, boolean proxy) {
            props.put(PATH, "hello/world");
            address.property(PROXY_PATH, Boolean.toString(proxy));
            if (proxy) {
                source.name(basePath);
            }
            return this;
        }

        public TestInstance queryParams(String queryParams, boolean proxy) {
            props.put(QUERY_PARAMS, queryParams);
            address.property(PROXY_QUERY_PARAMS, Boolean.toString(proxy));
            source.queryParams(proxy ? queryParams : null);
            return this;
        }

        private HttpDataSource.Builder defaultHttpSource() {
            return HttpDataSource.Builder.newInstance()
                    .httpClient(HTTP_CLIENT)
                    .monitor(MONITOR)
                    .retryPolicy(RETRY_POLICY);
        }
    }
}
