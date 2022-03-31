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

package org.eclipse.dataspaceconnector.dataplane.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.common.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore.State;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameters;
import org.mockserver.verify.VerificationTimes;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createDataAddress;
import static org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;
import static org.hamcrest.Matchers.containsString;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.KeyMatchStyle.MATCHING_KEY;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * System Test for Data Plane HTTP extension.
 */
@ComponentTest
public class DataPlaneHttpIntegrationTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Faker FAKER = new Faker();

    private static final int DPF_PUBLIC_API_PORT = getFreePort();
    private static final int DPF_CONTROL_API_PORT = getFreePort();
    private static final int DPF_HTTP_SOURCE_API_PORT = getFreePort();
    private static final int DPF_HTTP_SINK_API_PORT = getFreePort();
    private static final String DPF_CONTROL_API_HOST = "http://localhost:" + DPF_CONTROL_API_PORT;
    private static final String DPF_HTTP_SOURCE_API_HOST = "http://localhost:" + DPF_HTTP_SOURCE_API_PORT;
    private static final String DPF_HTTP_SINK_API_HOST = "http://localhost:" + DPF_HTTP_SINK_API_PORT;
    private static final String CONTROL_PATH = "/control";
    private static final String TRANSFER_PATH = format("%s/transfer", CONTROL_PATH);
    private static final String TRANSFER_RESULT_PATH = format("%s/transfer/{processId}", CONTROL_PATH);
    private static final String PROCESS_ID = "processId";
    private static final String EDC_TYPE = "edctype";
    private static final String DATA_FLOW_REQUEST_EDC_TYPE = "dataspaceconnector:dataflowrequest";
    private static final String DPF_HTTP_API_PART_NAME = "sample";
    private static final String AUTH_HEADER_KEY = HttpHeaderNames.AUTHORIZATION.toString();
    private static final String SOURCE_AUTH_VALUE = FAKER.lorem().word();
    private static final String SINK_AUTH_VALUE = FAKER.lorem().word();

    /**
     * HTTP Source mock server.
     */
    private static ClientAndServer httpSourceClientAndServer;
    /**
     * HTTP Sink mock server.
     */
    private static ClientAndServer httpSinkClientAndServer;

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":launchers:data-plane-server",
            "data-plane-server",
            Map.of(
                    "web.http.public.port", valueOf(DPF_PUBLIC_API_PORT),
                    "web.http.control.port", valueOf(DPF_CONTROL_API_PORT),
                    "web.http.control.path", CONTROL_PATH
            ));

    @BeforeAll
    public static void setUp() {
        httpSourceClientAndServer = startClientAndServer(DPF_HTTP_SOURCE_API_PORT);
        httpSinkClientAndServer = startClientAndServer(DPF_HTTP_SINK_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(httpSourceClientAndServer);
        stopQuietly(httpSinkClientAndServer);
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        httpSourceClientAndServer.reset();
        httpSinkClientAndServer.reset();
    }

    @Test
    void transfer_success() {
        // Arrange
        var body = FAKER.internet().uuid();
        var processId = FAKER.internet().uuid();
        httpSourceClientAndServer.when(getRequest(), once())
                .respond(withResponse(HttpStatusCode.OK_200, body));

        // HTTP Sink Request & Response
        httpSinkClientAndServer.when(postRequest(body), once())
                .respond(withResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceClientAndServer.verify(
                getRequest(),
                VerificationTimes.once()
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkClientAndServer.verify(
                postRequest(body),
                VerificationTimes.once()
        );
    }

    @Test
    void transfer_WithSourceQueryParams_Success() {
        // Arrange
        // HTTP Source Request & Response
        var body = FAKER.internet().uuid();
        var processId = FAKER.internet().uuid();
        var queryParams = Map.of(
                FAKER.lorem().word(), FAKER.internet().url(),
                FAKER.lorem().word(), FAKER.lorem().word()
        );

        httpSourceClientAndServer.when(getRequest(queryParams), once())
                .respond(withResponse(HttpStatusCode.OK_200, body));

        // HTTP Sink Request & Response
        httpSinkClientAndServer.when(postRequest(body), once())
                .respond(withResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, queryParams));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceClientAndServer.verify(
                getRequest(),
                VerificationTimes.once()
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkClientAndServer.verify(
                postRequest(body),
                VerificationTimes.once()
        );
    }

    /**
     * Verify DPF transfer api layer validation is working as expected.
     */
    @Test
    void transfer_invalidInput_failure() {
        // Arrange
        // Request without processId to initiate transfer.
        var processId = FAKER.internet().uuid();
        var invalidRequest = transferRequestPayload(processId).remove("processId");

        // Act & Assert
        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void transfer_sourceNotAvailable_noInteractionWithSink() {
        // Arrange
        var processId = FAKER.internet().uuid();
        // HTTP Source Request & Error Response
        httpSourceClientAndServer.when(getRequest())
                .error(withDropConnection());

        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );
        // Verify HTTP Source server called at lest once.
        httpSourceClientAndServer.verify(
                getRequest(),
                VerificationTimes.atLeast(1)
        );
        // Verify zero interaction with HTTP Sink.
        httpSinkClientAndServer.verifyZeroInteractions();
    }

    /**
     * Validate if intermittently source is dropping connection than DPF server retries to fetch data.
     */
    @Test
    void transfer_sourceTemporaryDropConnection_success() {
        // Arrange
        var processId = FAKER.internet().uuid();
        // First two calls to HTTP Source returns a failure response.
        httpSourceClientAndServer.when(getRequest(), exactly(2))
                .error(withDropConnection());

        // Next call to HTTP Source returns a valid response.
        var body = FAKER.internet().uuid();
        httpSourceClientAndServer.when(getRequest(), once())
                .respond(withResponse(HttpStatusCode.OK_200, body));

        // HTTP Sink Request & Response
        httpSinkClientAndServer.when(postRequest(body), once())
                .respond(withResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );
        // Verify HTTP Source server called exactly 3 times.
        httpSourceClientAndServer.verify(
                getRequest(),
                VerificationTimes.exactly(3)
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkClientAndServer.verify(
                postRequest(body),
                VerificationTimes.once()
        );
    }

    /**
     * Validate if intermittently sink is dropping connection than DPF server doesn't retry to deliver data.
     */
    @Test
    void transfer_sinkTemporaryDropsConnection_noRetry() {
        // Arrange
        // HTTP Source Request & Response
        var body = FAKER.internet().uuid();
        var processId = FAKER.internet().uuid();
        httpSourceClientAndServer.when(getRequest(), once())
                .respond(withResponse(HttpStatusCode.OK_200, body));

        // First two calls to HTTP sink drops the connection.
        httpSinkClientAndServer.when(postRequest(body), exactly(2))
                .error(withDropConnection());
        // Next call to HTTP sink returns a valid response.
        httpSinkClientAndServer.when(postRequest(body), once())
                .respond(withResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceClientAndServer.verify(
                getRequest(),
                VerificationTimes.once()
        );

        // Verify HTTP Sink server called exactly twice.
        // One extra call to sink is done because internally okhttp client by default retires on connection failure.
        httpSinkClientAndServer.verify(
                postRequest(body),
                VerificationTimes.exactly(2)
        );
    }

    /**
     * Request payload to initiate DPF transfer.
     *
     * @param processId ProcessID of transfer.See {@link DataFlowRequest}
     * @return JSON object. see {@link ObjectNode}.
     */
    private ObjectNode transferRequestPayload(String processId) {
        return transferRequestPayload(processId, Collections.emptyMap());
    }

    /**
     * Request payload with query params to initiate DPF transfer.
     *
     * @param processId   ProcessID of transfer.See {@link DataFlowRequest}
     * @param queryParams Query params name and value as key-value entries.
     * @return JSON object. see {@link ObjectNode}.
     */
    private ObjectNode transferRequestPayload(String processId, Map<String, String> queryParams) {

        var requestProperties = new HashMap<String, String>();
        requestProperties.put(DataFlowRequestSchema.METHOD, HttpMethod.GET.name());

        if (!queryParams.isEmpty()) {
            requestProperties.put(DataFlowRequestSchema.QUERY_PARAMS, queryParams.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&")));
        }

        var sourceDataAddress = createDataAddress(
                TYPE,
                Map.of(
                        ENDPOINT, DPF_HTTP_SOURCE_API_HOST,
                        NAME, DPF_HTTP_API_PART_NAME,
                        AUTHENTICATION_KEY, AUTH_HEADER_KEY,
                        AUTHENTICATION_CODE, SOURCE_AUTH_VALUE
                )).build();

        var destinationDataAddress = createDataAddress(
                TYPE,
                Map.of(
                        ENDPOINT, DPF_HTTP_SINK_API_HOST,
                        AUTHENTICATION_KEY, AUTH_HEADER_KEY,
                        AUTHENTICATION_CODE, SINK_AUTH_VALUE
                )).build();

        // Create valid dataflow request instance.
        var request = createRequest(requestProperties, sourceDataAddress, destinationDataAddress)
                .processId(processId)
                .build();

        // Add edctype to request
        var requestJsonNode = OBJECT_MAPPER.convertValue(request, ObjectNode.class);
        requestJsonNode.put(EDC_TYPE, DATA_FLOW_REQUEST_EDC_TYPE);

        return requestJsonNode;
    }

    /**
     * RestAssured request specification with DPF API host as base URI.
     *
     * @return see {@link RequestSpecification}
     */
    private RequestSpecification givenDpfRequest() {
        return given()
                .baseUri(DPF_CONTROL_API_HOST);
    }

    /**
     * Initiate a transfer and assert if response is HTTP OK.
     *
     * @param payload Request payload.
     */
    private void initiateTransfer(Object payload) {
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    /**
     * Fetch transfer state and assert if transfer is completed.
     *
     * @param processId ProcessID of transfer. See {@link DataFlowRequest}
     */
    private void fetchTransferState(String processId) {
        givenDpfRequest()
                .pathParam(PROCESS_ID, processId)
                .when()
                .get(TRANSFER_RESULT_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .body(containsString(State.COMPLETED.name()));
    }

    /**
     * Mock HTTP GET request for source.
     *
     * @return see {@link HttpRequest}
     */
    private HttpRequest getRequest() {
        return getRequest(Collections.emptyMap());
    }

    /**
     * Mock HTTP GET request with query params for source.
     *
     * @return see {@link HttpRequest}
     */
    private HttpRequest getRequest(Map<String, String> queryParams) {

        var request = request();

        var paramsList = queryParams.entrySet()
                .stream()
                .map(entry -> param(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        request.withQueryStringParameters(new Parameters(paramsList).withKeyMatchStyle(MATCHING_KEY));

        return request
                .withMethod(HttpMethod.GET.name())
                .withHeader(AUTH_HEADER_KEY, SOURCE_AUTH_VALUE)
                .withPath("/" + DPF_HTTP_API_PART_NAME);
    }

    /**
     * Mock HTTP POST request for sink.
     *
     * @param responseBody Request body.
     * @return see {@link HttpRequest}
     */
    private HttpRequest postRequest(String responseBody) {
        return request()
                .withMethod(HttpMethod.POST.name())
                .withPath("/" + DPF_HTTP_API_PART_NAME)
                .withHeaders(
                        new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                MediaType.APPLICATION_OCTET_STREAM.toString()),
                        new Header(AUTH_HEADER_KEY, SINK_AUTH_VALUE)
                )
                .withBody(binary(responseBody.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Mock http OK response from sink.
     *
     * @return see {@link HttpResponse}
     */
    private HttpResponse withResponse() {
        return withResponse(HttpStatusCode.OK_200, null);
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
                                    MediaType.PLAIN_TEXT_UTF_8.toString())
                    )
                    .withBody(responseBody);
        }

        return response;
    }

    /**
     * Mock error response which to force the connection to be dropped without any response being returned.
     *
     * @return see {@link HttpError}
     */
    private HttpError withDropConnection() {
        return error()
                .withDropConnection(true);
    }
}
