/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.http;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.apache.http.HttpStatus;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER;
import static io.restassured.RestAssured.given;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.NOTIFIED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ComponentTest
public class DataPlaneHttpIntegrationTests {

    private static final int PUBLIC_API_PORT = getFreePort();
    private static final int CONTROL_API_PORT = getFreePort();
    private static final int HTTP_SOURCE_API_PORT = getFreePort();
    private static final int HTTP_SINK_API_PORT = getFreePort();
    private static final String HTTP_SOURCE_API_HOST = "http://localhost:" + HTTP_SOURCE_API_PORT;
    private static final String HTTP_SINK_API_HOST = "http://localhost:" + HTTP_SINK_API_PORT;
    private static final String CONTROL_PATH = "/control";
    private static final String PUBLIC_PATH = "/public";
    private static final String PUBLIC_API_HOST = "http://localhost:" + PUBLIC_API_PORT;
    private static final String AUTH_HEADER_KEY = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";

    private static final String SOURCE_AUTH_VALUE = "source-auth-key";
    private static final String SINK_AUTH_VALUE = "sink-auth-key";
    private static final DataPlaneAuthorizationService DATA_PLANE_AUTHORIZATION_SERVICE = mock();
    private static final EmbeddedRuntime RUNTIME = new EmbeddedRuntime(
            "data-plane-server",
            ":core:data-plane:data-plane-core",
            ":extensions:common:api:control-api-configuration",
            ":extensions:common:http",
            ":extensions:common:json-ld",
            ":extensions:common:api:api-core",
            ":extensions:common:configuration:configuration-filesystem",
            ":extensions:control-plane:api:control-plane-api-client",
            ":extensions:data-plane:data-plane-http",
            ":extensions:data-plane:data-plane-public-api-v2",
            ":extensions:data-plane:data-plane-signaling:data-plane-signaling-api")
            .registerServiceMock(DataPlaneAuthorizationService.class, DATA_PLANE_AUTHORIZATION_SERVICE)
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "web.http.public.port", valueOf(PUBLIC_API_PORT),
                    "web.http.public.path", PUBLIC_PATH,
                    "web.http.control.port", valueOf(CONTROL_API_PORT),
                    "web.http.control.path", CONTROL_PATH,
                    "edc.core.retry.retries.max", "0"
            )));
    @RegisterExtension
    static WireMockExtension httpSourceMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(HTTP_SOURCE_API_PORT))
            .build();
    @RegisterExtension
    static WireMockExtension httpSinkMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(HTTP_SINK_API_PORT))
            .build();
    @RegisterExtension
    static WireMockExtension fakeControlPlane = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();
    private final Duration timeout = Duration.ofSeconds(30);

    @BeforeEach
    void beforeEach() {
        fakeControlPlane.stubFor(any(anyUrl()).willReturn(ok()));
    }

    @Nested
    @Deprecated(since = "0.12.0")
    class Pull {

        private static final String HTTP_API_PATH = "sample";

        @RegisterExtension
        static RuntimeExtension dataPlane = new RuntimePerClassExtension(RUNTIME);

        @Test
        void transfer_pull_withSourceQueryParamsAndPath_success(DataPlaneAuthorizationService dataPlaneAuthorizationService) {
            var token = UUID.randomUUID().toString();
            var responseBody = "some info";
            var queryParams = Map.of(
                    "param1", "foo",
                    "param2", "bar"
            );
            var sourceDataAddress = sourceDataAddress();

            httpSourceMockServer.stubFor(get(anyUrl()).willReturn(okForContentType(TEXT_PLAIN, responseBody)));

            when(dataPlaneAuthorizationService.authorize(any(), any())).thenReturn(Result.success(sourceDataAddress));

            given()
                    .baseUri(PUBLIC_API_HOST)
                    .contentType(ContentType.JSON)
                    .when()
                    .queryParams(queryParams)
                    .header(AUTH_HEADER_KEY, token)
                    .get(format("%s/%s", PUBLIC_PATH, HTTP_API_PATH))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body(CoreMatchers.equalTo(responseBody));

            httpSourceMockServer.verify(getRequestedFor(urlMatching("/" + HTTP_API_PATH + "?.*"))
                    .withQueryParam("param1", equalTo("foo"))
                    .withQueryParam("param2", equalTo("bar"))
                    .withHeader(AUTH_HEADER_KEY, equalTo(SOURCE_AUTH_VALUE)));
        }

        @Test
        void shouldProxyMethodAndBody_whenSet(DataPlaneAuthorizationService dataPlaneAuthorizationService) {
            var sourceAddress = HttpDataAddress.Builder.newInstance()
                    .baseUrl(HTTP_SOURCE_API_HOST)
                    .proxyMethod(TRUE.toString())
                    .proxyPath(TRUE.toString())
                    .proxyBody(TRUE.toString())
                    .build();

            httpSourceMockServer.stubFor(put(anyUrl()).willReturn(okForContentType(TEXT_PLAIN, "any")));
            when(dataPlaneAuthorizationService.authorize(any(), any())).thenReturn(Result.success(sourceAddress));

            given()
                    .baseUri(PUBLIC_API_HOST)
                    .contentType(ContentType.JSON)
                    .when()
                    .header(AUTH_HEADER_KEY, "any")
                    .body("body")
                    .put(format("%s/%s", PUBLIC_PATH, HTTP_API_PATH))
                    .then()
                    .log().ifError()
                    .statusCode(HttpStatus.SC_OK);

            httpSourceMockServer.verify(putRequestedFor(anyUrl()).withRequestBody(equalTo("body")));
        }

        private HttpDataAddress sourceDataAddress() {
            return HttpDataAddress.Builder.newInstance()
                    .baseUrl(HTTP_SOURCE_API_HOST)
                    .proxyPath(TRUE.toString())
                    .proxyQueryParams(TRUE.toString())
                    .authKey(AUTH_HEADER_KEY)
                    .authCode(SOURCE_AUTH_VALUE)
                    .build();
        }
    }

    @Nested
    class Push {

        @RegisterExtension
        static RuntimeExtension dataPlane = new RuntimePerClassExtension(RUNTIME);

        @Test
        void transfer_toHttpSink_success() {
            var body = "{}";
            var processId = UUID.randomUUID().toString();

            httpSourceMockServer.stubFor(getRequest("/").willReturn(okForContentType(APPLICATION_JSON, body)));
            httpSinkMockServer.stubFor(post(anyUrl()).willReturn(ok()));

            initiate(transferRequestPayload(processId))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));

            httpSourceMockServer.verify(getRequestedFor(anyUrl()));
            httpSinkMockServer.verify(postRequestedFor(anyUrl()).withRequestBody(equalToJson(body)));
        }

        @Test
        void transfer_toHttpSink_withSourcePathAndQueryParams_success() {
            var body = UUID.randomUUID().toString();
            var processId = UUID.randomUUID().toString();
            var queryParams = Map.of(
                    "param1", "any value",
                    "param2", "any other value"
            );

            httpSourceMockServer.stubFor(getRequest(queryParams, urlMatching("/path.*"))
                    .willReturn(okForContentType(APPLICATION_OCTET_STREAM, body)));
            httpSinkMockServer.stubFor(post(anyUrl()).withRequestBody(binaryEqualTo(body.getBytes()))
                    .withHeader(CONTENT_TYPE_HEADER, equalTo(APPLICATION_OCTET_STREAM))
                    .willReturn(ok()));

            var sourceDataAddress = Json.createObjectBuilder()
                    .add("@type", "dspace:DataAddress")
                    .add("dspace:endpointType", "HttpData")
                    .add("dspace:endpointProperties", sourceAddressProperties()
                            .add(dspaceProperty(EDC_NAMESPACE + "path", "path"))
                            .add(dspaceProperty(EDC_NAMESPACE + "queryParams", "param1=any value&param2=any other value"))
                    );

            initiate(transferRequestPayload(processId, sourceDataAddress))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));
            httpSourceMockServer.verify(getRequested(queryParams, urlMatching("/path.*")));
            httpSinkMockServer.verify(postRequested(body, APPLICATION_OCTET_STREAM));
        }

        @Test
        void transfer_toHttpSink_sourceNotAvailable_noInteractionWithSink() {
            var processId = UUID.randomUUID().toString();

            httpSourceMockServer.stubFor(getRequest("/").willReturn(aResponse().withFault(CONNECTION_RESET_BY_PEER)));

            initiate(transferRequestPayload(processId))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));
            httpSourceMockServer.verify(moreThanOrExactly(1), getRequested("/"));
            httpSinkMockServer.verify(0, anyRequestedFor(anyUrl()));
        }

        /**
         * Validate if intermittently source is dropping connection than DPF server retries to fetch data.
         */
        @Test
        void transfer_toHttpSink_sourceTemporaryDropConnection_success() {
            var processId = UUID.randomUUID().toString();
            var body = UUID.randomUUID().toString();

            httpSourceMockServer.stubFor(getRequest("/")
                    .willReturn(aResponse().withFault(CONNECTION_RESET_BY_PEER))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Started")
                    .willSetStateTo("First Retry"));

            httpSourceMockServer.stubFor(getRequest("/")
                    .willReturn(aResponse().withFault(CONNECTION_RESET_BY_PEER))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("First Retry")
                    .willSetStateTo("Second Retry"));

            httpSourceMockServer.stubFor(getRequest("/")
                    .willReturn(okForContentType(TEXT_PLAIN, body))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Second Retry"));


            httpSinkMockServer.stubFor(postRequest(body, APPLICATION_OCTET_STREAM).willReturn(ok()));

            initiate(transferRequestPayload(processId))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));
            httpSourceMockServer.verify(3, getRequested("/"));
            httpSinkMockServer.verify(postRequested(body, TEXT_PLAIN));
        }

        @Test
        void transfer_invalidInput_failure() {
            var processId = UUID.randomUUID().toString();
            var validRequest = transferRequestPayload(processId);
            var invalidRequest = Json.createObjectBuilder(validRequest).remove("transferTypeDestination").build();

            initiate(invalidRequest)
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        private Response initiate(JsonObject payload) {
            return given()
                    .port(CONTROL_API_PORT)
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/control/v1/dataflows");
        }

        private void expectState(String processId, DataFlowStates expectedState) {
            given()
                    .port(CONTROL_API_PORT)
                    .when()
                    .pathParam("id", processId)
                    .get("/control/v1/dataflows/{id}/state")
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK)
                    .body(containsString(expectedState.name()));
        }

        private JsonObject transferRequestPayload(String processId) {
            return transferRequestPayload(processId, sourceAddress());
        }

        private JsonObject transferRequestPayload(String processId, JsonObjectBuilder sourceDataAddress) {
            return Json.createObjectBuilder()
                    .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                    .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                    .add("@id", UUID.randomUUID().toString())
                    .add("processId", processId)
                    .add("sourceDataAddress", sourceDataAddress)
                    .add("destinationDataAddress", Json.createObjectBuilder()
                            .add("dspace:endpointType", "HttpData")
                            .add("dspace:endpointProperties", Json.createArrayBuilder()
                                    .add(dspaceProperty(EDC_NAMESPACE + "baseUrl", HTTP_SINK_API_HOST))
                                    .add(dspaceProperty(EDC_NAMESPACE + "authKey", AUTH_HEADER_KEY))
                                    .add(dspaceProperty(EDC_NAMESPACE + "authCode", SINK_AUTH_VALUE))
                            )
                    )
                    .add("flowType", "PUSH")
                    .add("transferTypeDestination", "HttpData-PUSH")
                    .add("callbackAddress", "http://localhost:" + fakeControlPlane.getPort())
                    .build();
        }

        private JsonObjectBuilder sourceAddress() {
            return Json.createObjectBuilder()
                    .add("@type", "dspace:DataAddress")
                    .add("dspace:endpointType", "HttpData")
                    .add("dspace:endpointProperties", sourceAddressProperties());
        }

        private JsonArrayBuilder sourceAddressProperties() {
            return Json.createArrayBuilder()
                    .add(dspaceProperty(EDC_NAMESPACE + "baseUrl", HTTP_SOURCE_API_HOST))
                    .add(dspaceProperty(EDC_NAMESPACE + "authKey", AUTH_HEADER_KEY))
                    .add(dspaceProperty(EDC_NAMESPACE + "authCode", SOURCE_AUTH_VALUE));
        }

        private JsonObjectBuilder dspaceProperty(String name, String value) {
            return Json.createObjectBuilder()
                    .add("dspace:name", name)
                    .add("dspace:value", value);
        }

    }

    /**
     * Mock HTTP GET request for source.
     *
     * @return see {@link MappingBuilder}
     */
    private MappingBuilder getRequest(String path) {
        return getRequest(emptyMap(), path);
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link MappingBuilder}
     */
    private MappingBuilder getRequest(Map<String, String> queryParams, String path) {
        return getRequest(queryParams, urlEqualTo(path));
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link MappingBuilder}
     */
    private MappingBuilder getRequest(Map<String, String> queryParams, UrlPattern pattern) {
        var request = get(pattern);
        request.withQueryParams(toQueryParams(queryParams));
        return request
                .withHeader(AUTH_HEADER_KEY, equalTo(SOURCE_AUTH_VALUE));
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link RequestPatternBuilder}
     */

    private RequestPatternBuilder getRequested(String path) {
        return getRequested(emptyMap(), path);
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link RequestPatternBuilder}
     */
    private RequestPatternBuilder getRequested(Map<String, String> queryParams, String path) {
        return getRequested(queryParams, urlEqualTo(path));
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link RequestPatternBuilder}
     */
    private RequestPatternBuilder getRequested(Map<String, String> queryParams, UrlPattern pattern) {
        var request = getRequestedFor(pattern);
        var params = toQueryParams(queryParams);
        params.forEach(request::withQueryParam);
        return request
                .withHeader(AUTH_HEADER_KEY, equalTo(SOURCE_AUTH_VALUE));
    }

    private Map<String, StringValuePattern> toQueryParams(Map<String, String> queryParams) {
        return queryParams.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> equalTo(entry.getValue())));
    }

    private RequestPatternBuilder postRequested(String responseBody, String contentType) {
        return postRequestedFor(anyUrl())
                .withHeader(AUTH_HEADER_KEY, equalTo(SINK_AUTH_VALUE))
                .withHeader("Content-Type", equalTo(contentType))
                .withRequestBody(binaryEqualTo(responseBody.getBytes(StandardCharsets.UTF_8)));
    }

    private MappingBuilder postRequest(String responseBody, String contentType) {
        return post("/")
                .withHeader(AUTH_HEADER_KEY, equalTo(SINK_AUTH_VALUE))
                .withHeader("Content-Type", equalTo(contentType))
                .withRequestBody(binaryEqualTo(responseBody.getBytes(StandardCharsets.UTF_8)));
    }

}
