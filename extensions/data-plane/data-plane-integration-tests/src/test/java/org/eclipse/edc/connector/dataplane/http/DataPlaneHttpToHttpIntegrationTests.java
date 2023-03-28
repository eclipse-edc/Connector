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
 *       sovity GmbH - binary data transfer test
 */

package org.eclipse.edc.connector.dataplane.http;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore.State;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.http.testfixtures.HttpTestFixtures.createRequest;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
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
public class DataPlaneHttpToHttpIntegrationTests {

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

    private static final String DPF_HTTP_API_PATH = "sample";
    private static final String PROXY_PATH_TOGGLE = "true";
    private static final String PROXY_QUERY_PARAMS_TOGGLE = "true";
    private static final String AUTH_HEADER_KEY = HttpHeaderNames.AUTHORIZATION.toString();
    private static final String SOURCE_AUTH_VALUE = "source-auth-key";
    private static final String SINK_AUTH_VALUE = "sink-auth-key";

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":launchers:data-plane-server",
            "data-plane-server",
            Map.of(
                    "web.http.public.port", valueOf(DPF_PUBLIC_API_PORT),
                    "web.http.control.port", valueOf(DPF_CONTROL_API_PORT),
                    "web.http.control.path", CONTROL_PATH,
                    "edc.dataplane.token.validation.endpoint", "http://not.used/endpoint"
            ));

    private static ClientAndServer httpSourceMockServer;
    private static ClientAndServer httpSinkMockServer;

    @BeforeAll
    public static void setUp() {
        httpSourceMockServer = startClientAndServer(DPF_HTTP_SOURCE_API_PORT);
        httpSinkMockServer = startClientAndServer(DPF_HTTP_SINK_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(httpSourceMockServer);
        stopQuietly(httpSinkMockServer);
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        httpSourceMockServer.reset();
        httpSinkMockServer.reset();
    }

    @Test
    void transfer_success(TypeManager typeManager) {
        var body = UUID.randomUUID().toString();
        var processId = UUID.randomUUID().toString();
        httpSourceMockServer.when(getRequest(), once())
                .respond(successfulResponse(body));

        // HTTP Sink Request & Response
        httpSinkMockServer.when(postRequest(body), once())
                .respond(successfulResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, typeManager));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceMockServer.verify(
                getRequest(),
                VerificationTimes.once()
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkMockServer.verify(
                postRequest(body),
                VerificationTimes.once()
        );
    }

    @Test
    void transfer_binary_success(TypeManager typeManager) {
        var binaryDataBody = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryDataBody[i] = (byte) i;
        }

        var processId = UUID.randomUUID().toString();
        httpSourceMockServer.when(getRequest(), once())
                .respond(successfulBinaryResponse(binaryDataBody));

        // HTTP Sink Request & Response
        httpSinkMockServer.when(postBinaryRequest(binaryDataBody), once())
                .respond(successfulBinaryResponse(binaryDataBody));

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, typeManager));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceMockServer.verify(
                getRequest(),
                VerificationTimes.once()
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkMockServer.verify(
                postBinaryRequest(binaryDataBody),
                VerificationTimes.once()
        );
    }

    @Test
    void transfer_WithSourceQueryParams_Success(TypeManager typeManager) {
        // HTTP Source Request & Response
        var body = UUID.randomUUID().toString();
        var processId = UUID.randomUUID().toString();
        var queryParams = Map.of(
                "param1", "any value",
                "param2", "any other value"
        );

        httpSourceMockServer.when(getRequest(queryParams), once())
                .respond(successfulResponse(body));

        // HTTP Sink Request & Response
        httpSinkMockServer.when(postRequest(body), once())
                .respond(successfulResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, queryParams, typeManager));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceMockServer.verify(
                getRequest(),
                VerificationTimes.once()
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkMockServer.verify(
                postRequest(body),
                VerificationTimes.once()
        );
    }

    /**
     * Verify DPF transfer api layer validation is working as expected.
     */
    @Test
    void transfer_invalidInput_failure(TypeManager typeManager) {
        // Request without processId to initiate transfer.
        var processId = UUID.randomUUID().toString();
        var invalidRequest = transferRequestPayload(processId, typeManager).remove("processId");

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
    void transfer_sourceNotAvailable_noInteractionWithSink(TypeManager typeManager) {
        var processId = UUID.randomUUID().toString();
        // HTTP Source Request & Error Response
        httpSourceMockServer.when(getRequest())
                .error(withDropConnection());

        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, typeManager));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );
        // Verify HTTP Source server called at lest once.
        httpSourceMockServer.verify(
                getRequest(),
                VerificationTimes.atLeast(1)
        );
        // Verify zero interaction with HTTP Sink.
        httpSinkMockServer.verifyZeroInteractions();
    }

    /**
     * Validate if intermittently source is dropping connection than DPF server retries to fetch data.
     */
    @Test
    void transfer_sourceTemporaryDropConnection_success(TypeManager typeManager) {
        var processId = UUID.randomUUID().toString();
        // First two calls to HTTP Source returns a failure response.
        httpSourceMockServer.when(getRequest(), exactly(2))
                .error(withDropConnection());

        // Next call to HTTP Source returns a valid response.
        var body = UUID.randomUUID().toString();
        httpSourceMockServer.when(getRequest(), once())
                .respond(successfulResponse(body));

        // HTTP Sink Request & Response
        httpSinkMockServer.when(postRequest(body), once())
                .respond(successfulResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, typeManager));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );
        // Verify HTTP Source server called exactly 3 times.
        httpSourceMockServer.verify(
                getRequest(),
                VerificationTimes.exactly(3)
        );
        // Verify HTTP Sink server called exactly once.
        httpSinkMockServer.verify(
                postRequest(body),
                VerificationTimes.once()
        );
    }

    /**
     * Validate if intermittently sink is dropping connection than DPF server will retry to deliver data.
     */
    @Test
    void transfer_sinkTemporaryDropsConnection_retry(TypeManager typeManager) {
        // Arrange
        // HTTP Source Request & Response
        var body = UUID.randomUUID().toString();
        var processId = UUID.randomUUID().toString();
        httpSourceMockServer.when(getRequest(), once())
                .respond(successfulResponse(body));

        // First two calls to HTTP sink drops the connection.
        httpSinkMockServer.when(postRequest(body), exactly(2))
                .error(withDropConnection());
        // Next call to HTTP sink returns a valid response.
        httpSinkMockServer.when(postRequest(body), once())
                .respond(successfulResponse());

        // Act & Assert
        // Initiate transfer
        initiateTransfer(transferRequestPayload(processId, typeManager));

        // Wait for transfer to be completed.
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferState(processId)
        );

        // Verify HTTP Source server called exactly once.
        httpSourceMockServer.verify(
                getRequest(),
                VerificationTimes.once()
        );

        // Verify HTTP Sink server called exactly three times.
        // One extra call to sink is done because internally okhttp client by default retires on connection failure.
        httpSinkMockServer.verify(
                postRequest(body),
                VerificationTimes.exactly(3)
        );
    }

    /**
     * Request payload to initiate DPF transfer.
     *
     * @param processId ProcessID of transfer.See {@link DataFlowRequest}
     * @return JSON object. see {@link ObjectNode}.
     */
    private ObjectNode transferRequestPayload(String processId, TypeManager typeManager) {
        return transferRequestPayload(processId, Collections.emptyMap(), typeManager);
    }

    /**
     * Request payload with query params to initiate DPF transfer.
     *
     * @param processId   ProcessID of transfer.See {@link DataFlowRequest}
     * @param queryParams Query params name and value as key-value entries.
     * @return JSON object. see {@link ObjectNode}.
     */
    private ObjectNode transferRequestPayload(String processId, Map<String, String> queryParams, TypeManager typeManager) {
        var requestProperties = new HashMap<String, String>();
        requestProperties.put(DataFlowRequestSchema.METHOD, HttpMethod.GET.name());
        requestProperties.put(DataFlowRequestSchema.PATH, DPF_HTTP_API_PATH);

        if (!queryParams.isEmpty()) {
            requestProperties.put(DataFlowRequestSchema.QUERY_PARAMS, queryParams.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&")));
        }

        var sourceDataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl(DPF_HTTP_SOURCE_API_HOST)
                .proxyPath(PROXY_PATH_TOGGLE)
                .proxyQueryParams(PROXY_QUERY_PARAMS_TOGGLE)
                .authKey(AUTH_HEADER_KEY)
                .authCode(SOURCE_AUTH_VALUE)
                .build();

        var destinationDataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl(DPF_HTTP_SINK_API_HOST)
                .authKey(AUTH_HEADER_KEY)
                .authCode(SINK_AUTH_VALUE)
                .build();

        // Create valid dataflow request instance.
        var request = createRequest(requestProperties, sourceDataAddress, destinationDataAddress)
                .processId(processId)
                .build();

        return typeManager.getMapper().convertValue(request, ObjectNode.class);
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
                .withPath("/" + DPF_HTTP_API_PATH);
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
                .withHeader(AUTH_HEADER_KEY, SINK_AUTH_VALUE)
                .withContentType(MediaType.APPLICATION_OCTET_STREAM)
                .withBody(binary(responseBody.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Mock binary HTTP POST request for sink.
     *
     * @param byteResponseBody Request body.
     * @return see {@link HttpRequest}
     */
    private HttpRequest postBinaryRequest(byte[] byteResponseBody) {
        return request()
                .withMethod(HttpMethod.POST.name())
                .withHeader(AUTH_HEADER_KEY, SINK_AUTH_VALUE)
                .withContentType(MediaType.APPLICATION_OCTET_STREAM)
                .withBody(byteResponseBody);
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

    /**
     * Mock plain text response from source.
     *
     * @param responseBody Response body.
     * @return see {@link HttpResponse}
     */
    private HttpResponse successfulResponse(String responseBody) {
        return successfulResponse()
                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.PLAIN_TEXT_UTF_8.toString())
                .withBody(responseBody);
    }

    /**
     * Mock binary response from source.
     *
     * @param byteResponseBody Response body.
     * @return see {@link HttpResponse}
     */
    private HttpResponse successfulBinaryResponse(byte[] byteResponseBody) {
        return successfulResponse()
                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_OCTET_STREAM.toString())
                .withBody(byteResponseBody);
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
