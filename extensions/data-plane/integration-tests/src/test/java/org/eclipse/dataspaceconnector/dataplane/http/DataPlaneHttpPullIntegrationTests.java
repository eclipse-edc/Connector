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
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
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
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
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

    private static Stream<Arguments> providerTestInstances() {
        var get = new TestInstance(HttpMethod.GET.name());
        var path = FAKER.lorem().word() + "/" + FAKER.lorem().word();
        var body = FAKER.lorem().sentence();

        var getWithQueryParams = new TestInstance(HttpMethod.GET.name())
                .queryParam(FAKER.lorem().word(), FAKER.lorem().word())
                .queryParam(FAKER.lorem().word(), FAKER.lorem().word());

        var getWithPath = new TestInstance(HttpMethod.GET.name())
                .path(path);

        var post = new TestInstance(HttpMethod.POST.name())
                .requestBody(body);

        var postWithPath = new TestInstance(HttpMethod.POST.name())
                .path(path)
                .requestBody(body);

        return Stream.of(
                Arguments.of("POST", post),
                Arguments.of("GET", get),
                Arguments.of("GET WITH QUERY PARAMS", getWithQueryParams),
                Arguments.of("GET WITH PATH", getWithPath),
                Arguments.of("POST WITH PATH", postWithPath)
        );
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
        // prepare server of the data source
        httpSourceClientAndServer.when(instance.expectedSourceRequest, once())
                .respond(withResponse(HttpStatusCode.OK_200, OBJECT_MAPPER.writeValueAsString(instance.sourceResponse)));

        // prepare validation server of the control plane
        var claimToken = ClaimToken.Builder.newInstance()
                .claim(DataPlaneConstants.DATA_ADDRESS, OBJECT_MAPPER.writeValueAsString(instance.sourceAddress))
                .build();
        var validationRequest = request().withMethod(HttpMethod.GET.name()).withHeader(AUTH_HEADER_KEY, instance.token);
        validationClientAndServer.when(validationRequest, once())
                .respond(withResponse(HttpStatusCode.OK_200, OBJECT_MAPPER.writeValueAsString(claimToken)));

        instance.dataplaneRequest.request(instance.method).then().assertThat()
                .statusCode(200)
                .body("some", CoreMatchers.equalTo("info"));

        httpSourceClientAndServer.verify(instance.expectedSourceRequest, VerificationTimes.once());
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
     * One test instance for the parameterized test.
     */
    private static final class TestInstance {
        private final Map<String, String> sourceResponse = Map.of("some", "info");
        private final String method;
        private final String token;
        private final RequestSpecification dataplaneRequest;
        private final HttpRequest expectedSourceRequest;
        private final DataAddress sourceAddress;

        TestInstance(String method) {
            this.method = method;
            token = FAKER.internet().uuid();
            dataplaneRequest = givenDpfRequest(token);
            sourceAddress = testHttpSource();
            expectedSourceRequest = new HttpRequest();
        }

        public TestInstance requestBody(String requestBody) {
            dataplaneRequest.body(requestBody);
            expectedSourceRequest.withBody(requestBody);
            return this;
        }

        public TestInstance path(String path) {
            dataplaneRequest.basePath(DPF_DATA_PATH + path);
            expectedSourceRequest.withPath(path.startsWith("/") ? path : "/" + path);
            return this;
        }

        public TestInstance queryParam(String key, String value) {
            dataplaneRequest.queryParam(key, value);
            expectedSourceRequest.withQueryStringParameter(key, value);
            return this;
        }

        private RequestSpecification givenDpfRequest(String token) {
            return given()
                    .baseUri(DPF_PUBLIC_API_HOST)
                    .basePath(DPF_DATA_PATH)
                    .header(AUTH_HEADER_KEY, token);
        }

        /**
         * Create a minimal http address composed of an endpoint only.
         */
        private DataAddress testHttpSource() {
            return HttpDataAddress.Builder.newInstance()
                    .baseUrl(HTTP_SOURCE_API_HOST)
                    .proxyBody("true")
                    .proxyMethod("true")
                    .proxyPath("true")
                    .proxyQueryParams("true")
                    .build();
        }
    }
}
