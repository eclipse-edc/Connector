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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameters;
import org.mockserver.verify.VerificationTimes;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.KeyMatchStyle.MATCHING_KEY;
import static org.mockserver.model.MediaType.APPLICATION_JSON;
import static org.mockserver.model.MediaType.APPLICATION_OCTET_STREAM;
import static org.mockserver.model.MediaType.PLAIN_TEXT_UTF_8;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.stop.Stop.stopQuietly;


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
    private static final String AUTH_HEADER_KEY = AUTHORIZATION.toString();
    private static final String SOURCE_AUTH_VALUE = "source-auth-key";
    private static final String SINK_AUTH_VALUE = "sink-auth-key";
    private static ClientAndServer httpSourceMockServer;
    private static ClientAndServer httpSinkMockServer;
    private final Duration timeout = Duration.ofSeconds(30);

    private static final EmbeddedRuntime RUNTIME = new EmbeddedRuntime(
            "data-plane-server",
            Map.of(
                    "web.http.public.port", valueOf(PUBLIC_API_PORT),
                    "web.http.public.path", PUBLIC_PATH,
                    "web.http.control.port", valueOf(CONTROL_API_PORT),
                    "web.http.control.path", CONTROL_PATH,
                    "edc.core.retry.retries.max", "0"
            ),
            ":core:data-plane:data-plane-core",
            ":extensions:common:api:control-api-configuration",
            ":extensions:common:http",
            ":extensions:common:json-ld",
            ":extensions:common:configuration:configuration-filesystem",
            ":extensions:control-plane:api:control-plane-api-client",
            ":extensions:data-plane:data-plane-http",
            ":extensions:data-plane:data-plane-public-api-v2",
            ":extensions:data-plane:data-plane-signaling:data-plane-signaling-api"
    ).registerServiceMock(DataPlaneAuthorizationService.class, mock());

    @BeforeAll
    public static void setUp() {
        httpSourceMockServer = startClientAndServer(HTTP_SOURCE_API_PORT);
        httpSinkMockServer = startClientAndServer(HTTP_SINK_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(httpSourceMockServer);
        stopQuietly(httpSinkMockServer);
    }

    @AfterEach
    public void resetMockServer() {
        httpSourceMockServer.reset();
        httpSinkMockServer.reset();
    }

    @Nested
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

            var sourceRequest = getRequest(queryParams, HTTP_API_PATH);
            httpSourceMockServer.when(sourceRequest, once()).respond(successfulResponse(responseBody, PLAIN_TEXT_UTF_8));
            when(dataPlaneAuthorizationService.authorize(any(), any())).thenReturn(Result.success(sourceDataAddress));

            given()
                    .baseUri(PUBLIC_API_HOST)
                    .contentType(ContentType.JSON)
                    .when()
                    .queryParams(queryParams)
                    .header(AUTHORIZATION.toString(), token)
                    .get(format("%s/%s", PUBLIC_PATH, HTTP_API_PATH))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body(equalTo(responseBody));

            httpSourceMockServer.verify(sourceRequest, VerificationTimes.once());
        }

        @Test
        void shouldProxyMethodAndBody_whenSet(DataPlaneAuthorizationService dataPlaneAuthorizationService) {
            var sourceAddress = HttpDataAddress.Builder.newInstance()
                    .baseUrl(HTTP_SOURCE_API_HOST)
                    .proxyMethod(TRUE.toString())
                    .proxyPath(TRUE.toString())
                    .proxyBody(TRUE.toString())
                    .build();
            httpSourceMockServer.when(request(), once()).respond(successfulResponse("any", PLAIN_TEXT_UTF_8));
            when(dataPlaneAuthorizationService.authorize(any(), any())).thenReturn(Result.success(sourceAddress));

            given()
                    .baseUri(PUBLIC_API_HOST)
                    .contentType(ContentType.JSON)
                    .when()
                    .header(AUTHORIZATION.toString(), "any")
                    .body("body")
                    .put(format("%s/%s", PUBLIC_PATH, HTTP_API_PATH))
                    .then()
                    .log().ifError()
                    .statusCode(HttpStatus.SC_OK);

            httpSourceMockServer.verify(request().withMethod("PUT").withBody("body"), VerificationTimes.once());
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
            var body = UUID.randomUUID().toString();
            var processId = UUID.randomUUID().toString();
            httpSourceMockServer.when(getRequest(""), once()).respond(successfulResponse(body, APPLICATION_JSON));
            httpSinkMockServer.when(request(), once()).respond(successfulResponse());

            initiate(transferRequestPayload(processId))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));
            httpSourceMockServer.verify(getRequest(""), VerificationTimes.once());
            httpSinkMockServer.verify(postRequest(body, APPLICATION_JSON), VerificationTimes.once());
        }

        @Test
        void transfer_toHttpSink_withSourcePathAndQueryParams_success() {
            var body = UUID.randomUUID().toString();
            var processId = UUID.randomUUID().toString();
            var queryParams = Map.of(
                    "param1", "any value",
                    "param2", "any other value"
            );

            httpSourceMockServer.when(getRequest(queryParams, "path"), once()).respond(successfulResponse(body, APPLICATION_OCTET_STREAM));
            httpSinkMockServer.when(postRequest(body, APPLICATION_OCTET_STREAM), once()).respond(successfulResponse());

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
            httpSourceMockServer.verify(getRequest(queryParams, "path"), VerificationTimes.once());
            httpSinkMockServer.verify(postRequest(body, APPLICATION_OCTET_STREAM), VerificationTimes.once());
        }

        @Test
        void transfer_toHttpSink_sourceNotAvailable_noInteractionWithSink() {
            var processId = UUID.randomUUID().toString();
            httpSourceMockServer.when(getRequest("")).error(withDropConnection());

            initiate(transferRequestPayload(processId))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));
            httpSourceMockServer.verify(getRequest(""), VerificationTimes.atLeast(1));
            httpSinkMockServer.verifyZeroInteractions();
        }

        /**
         * Validate if intermittently source is dropping connection than DPF server retries to fetch data.
         */
        @Test
        void transfer_toHttpSink_sourceTemporaryDropConnection_success() {
            var processId = UUID.randomUUID().toString();
            httpSourceMockServer.when(getRequest(""), exactly(2)).error(withDropConnection());

            var body = UUID.randomUUID().toString();
            httpSourceMockServer.when(getRequest(""), once()).respond(successfulResponse(body, PLAIN_TEXT_UTF_8));

            httpSinkMockServer.when(postRequest(body, APPLICATION_OCTET_STREAM), once()).respond(successfulResponse());

            initiate(transferRequestPayload(processId))
                    .then()
                    .assertThat().statusCode(HttpStatus.SC_OK);

            await().atMost(timeout).untilAsserted(() -> expectState(processId, NOTIFIED));
            httpSourceMockServer.verify(getRequest(""), VerificationTimes.exactly(3));
            httpSinkMockServer.verify(postRequest(body, PLAIN_TEXT_UTF_8), VerificationTimes.once());
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

        private HttpError withDropConnection() {
            return error()
                    .withDropConnection(true);
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
     * @return see {@link HttpRequest}
     */
    private HttpRequest getRequest(String path) {
        return getRequest(emptyMap(), path);
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link HttpRequest}
     */
    private HttpRequest getRequest(Map<String, String> queryParams, String path) {
        var request = request();

        var paramsList = queryParams.entrySet()
                .stream()
                .map(entry -> param(entry.getKey(), entry.getValue()))
                .toList();

        request.withQueryStringParameters(new Parameters(paramsList).withKeyMatchStyle(MATCHING_KEY));

        return request
                .withMethod(HttpMethod.GET.name())
                .withHeader(AUTH_HEADER_KEY, SOURCE_AUTH_VALUE)
                .withPath("/" + path);
    }

    private HttpRequest postRequest(String responseBody, MediaType contentType) {
        return request()
                .withMethod(HttpMethod.POST.name())
                .withHeader(AUTH_HEADER_KEY, SINK_AUTH_VALUE)
                .withContentType(contentType)
                .withBody(binary(responseBody.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Mock http OK response from sink.
     *
     * @return see {@link HttpResponse}
     */
    private HttpResponse successfulResponse() {
        return response()
                .withStatusCode(HttpStatusCode.OK_200.code());
    }

    private HttpResponse successfulResponse(String responseBody, MediaType contentType) {
        return successfulResponse()
                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType.toString())
                .withBody(responseBody);
    }
}
