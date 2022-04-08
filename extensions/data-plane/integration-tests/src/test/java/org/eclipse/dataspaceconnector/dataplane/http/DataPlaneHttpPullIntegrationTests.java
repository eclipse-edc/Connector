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
 *       Mercedes Benz Tech Innovation - add toggles for proxy behavior
 */

package org.eclipse.dataspaceconnector.dataplane.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.common.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * System Test for Data Plane HTTP extension.
 */
@ExtendWith(EdcExtension.class)
@ComponentTest
public class DataPlaneHttpPullIntegrationTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Faker FAKER = new Faker();
    private static final String AUTH_HEADER_KEY = HttpHeaderNames.AUTHORIZATION.toString();

    // Validation API
    private static final int VALIDATION_API_PORT = getFreePort();
    private static final String VALIDATION_API_HOST = "http://localhost:" + VALIDATION_API_PORT;

    // DPF API
    private static final int DPF_CONTROL_API_PORT = getFreePort();
    private static final int DPF_PUBLIC_API_PORT = getFreePort();
    private static final String DPF_PUBLIC_API_HOST = "http://localhost:" + DPF_PUBLIC_API_PORT;
    private static final String DPF_PUBLIC_PATH = "/public";
    private static final String DPF_DATA_PATH = String.format("%s/", DPF_PUBLIC_PATH);

    // Data source API
    private static final int HTTP_SOURCE_API_PORT = getFreePort();
    private static final String HTTP_SOURCE_API_HOST = "http://localhost:" + HTTP_SOURCE_API_PORT;

    /**
     * HTTP Source mock server.
     */
    private static ClientAndServer httpSourceClientAndServer;

    /**
     * Validation API mock server.
     */
    private static ClientAndServer validationClientAndServer;

    private final Map<String, String> props = Map.of(
            "web.http.control.port", String.valueOf(DPF_CONTROL_API_PORT),
            "web.http.public.port", String.valueOf(DPF_PUBLIC_API_PORT),
            "web.http.public.path", DPF_PUBLIC_PATH,
            "edc.controlplane.validation-endpoint", VALIDATION_API_HOST
    );

    @BeforeAll
    public static void setUp() {
        httpSourceClientAndServer = startClientAndServer(HTTP_SOURCE_API_PORT);
        validationClientAndServer = startClientAndServer(VALIDATION_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(httpSourceClientAndServer);
        stopQuietly(validationClientAndServer);
    }

    @BeforeEach
    void setProperties(EdcExtension extension) {
        extension.registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) () -> ConfigFactory.fromMap(props));
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        httpSourceClientAndServer.reset();
        validationClientAndServer.reset();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("providerTestInstances")
    void test(String name, TestInstance instance) throws JsonProcessingException {
        instance.execute();
    }


    private static Stream<Arguments> providerTestInstances() {
        var get = new TestInstance(HttpMethod.GET.name());

        var getWithQueryParams = new TestInstance(HttpMethod.GET.name())
                .queryParam("foo", "bar")
                .queryParam("hello", "world");


        var getWithPathParams = new TestInstance(HttpMethod.GET.name())
                .basePath("hello/world");

        var post = new TestInstance(HttpMethod.POST.name())
                .requestBody("hello world!");

        var postWithPathParams = new TestInstance(HttpMethod.POST.name())
                .basePath("hello/world")
                .requestBody("hello world!");

        return Stream.of(
                Arguments.of("POST", post),
                Arguments.of("GET", get),
                Arguments.of("GET WITH QUERY PARAMS", getWithQueryParams),
                Arguments.of("GET WITH PATH PARAMS", getWithPathParams),
                Arguments.of("POST WITH PATH PARAMS", postWithPathParams)
        );
    }

    /**
     * One test instance for the parameterized test.
     */
    private static final class TestInstance {
        private final String token = FAKER.internet().uuid();
        private DataAddress dataSource = testHttpSource();
        private final Map<String, String> sourceResponse = Map.of("some", "info");

        private final String method;
        private final RequestSpecification dataplaneRequest;
        private final HttpRequest expectedSourceRequest;

        public TestInstance(String method) {
            this.method = method;
            dataplaneRequest = givenDpfRequest(token);
            expectedSourceRequest = new HttpRequest();
        }

        public TestInstance dataSource(DataAddress dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public TestInstance requestBody(String requestBody) {
            dataplaneRequest.body(requestBody);
            expectedSourceRequest.withBody(requestBody);
            return this;
        }

        public TestInstance basePath(String basePath) {
            dataplaneRequest.basePath(DPF_DATA_PATH + basePath);
            expectedSourceRequest.withPath(basePath.startsWith("/") ? basePath : String.format("/%s", basePath));
            return this;
        }

        public TestInstance queryParam(String key, String value) {
            dataplaneRequest.queryParam(key, value);
            expectedSourceRequest.withQueryStringParameter(key, value);
            return this;
        }

        public void execute() throws JsonProcessingException {
            Objects.requireNonNull(method);
            setUpValidationServer();
            setUpDataSourceServer();

            dataplaneRequest.request(method).then().assertThat().statusCode(200).body("some", CoreMatchers.equalTo("info"));

            httpSourceClientAndServer.verify(expectedSourceRequest, VerificationTimes.once());
        }

        private RequestSpecification givenDpfRequest(String token) {
            return given()
                    .baseUri(DPF_PUBLIC_API_HOST)
                    .basePath(DPF_DATA_PATH)
                    .header(AUTH_HEADER_KEY, token);
        }

        /**
         * Prepare data source server so that it returns the desired response if the expected request is received.
         */
        private void setUpDataSourceServer() throws JsonProcessingException {
            httpSourceClientAndServer.when(expectedSourceRequest, once())
                    .respond(withResponse(HttpStatusCode.OK_200, OBJECT_MAPPER.writeValueAsString(sourceResponse)));
        }

        /**
         * Prepare the validation server to make it return the desired source data address embedded within a {@link ClaimToken}
         * in exchange for the input token that is used in input of the DPF public API request.
         */
        private void setUpValidationServer() throws JsonProcessingException {
            var claimToken = ClaimToken.Builder.newInstance()
                    .claim(DataPlaneConstants.DATA_ADDRESS, OBJECT_MAPPER.writeValueAsString(dataSource))
                    .build();

            var validationRequest = request().withMethod(HttpMethod.GET.name()).withHeader(AUTH_HEADER_KEY, token);

            validationClientAndServer.when(validationRequest, once())
                    .respond(withResponse(HttpStatusCode.OK_200, OBJECT_MAPPER.writeValueAsString(claimToken)));
        }

        /**
         * Mock plain text response from source.
         *
         * @param statusCode   Response status code.
         * @param responseBody Response body.
         * @return see {@link HttpResponse}
         */
        private HttpResponse withResponse(HttpStatusCode statusCode, String responseBody) {
            var response = response()
                    .withStatusCode(statusCode.code());

            if (responseBody != null) {
                response.withHeader(
                                new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                        MediaType.APPLICATION_JSON.toString())
                        )
                        .withBody(responseBody);
            }

            return response;
        }

        /**
         * Create a minimal http address composed of an endpoint only.
         */
        private DataAddress testHttpSource() {
            return DataAddress.Builder.newInstance()
                    .type(HttpDataAddressSchema.TYPE)
                    .property(HttpDataAddressSchema.ENDPOINT, HTTP_SOURCE_API_HOST)
                    .property(HttpDataAddressSchema.PROXY_BODY, "true")
                    .property(HttpDataAddressSchema.PROXY_METHOD, "true")
                    .property(HttpDataAddressSchema.PROXY_PATH, "true")
                    .property(HttpDataAddressSchema.PROXY_QUERY_PARAMS, "true")
                    .build();
        }
    }
}
