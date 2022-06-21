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
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.client.DataPlaneTransferClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.transfer.dataplane.TestFixtures.createDataFlowRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

class RemoteDataPlaneTransferClientTest {

    private static final Faker FAKER = new Faker();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Data Plane API
    private static final int DATA_PLANE_API_PORT = getFreePort();
    private static final String DATA_PLANE_PATH = "/transfer";
    private static final String DATA_PLANE_API_URI = "http://localhost:" + DATA_PLANE_API_PORT + DATA_PLANE_PATH;
    /**
     * Data plane mock server.
     */
    private static ClientAndServer dataPlaneClientAndServer;
    private DataPlaneSelectorClient selectorClientMock;
    private DataPlaneTransferClient transferClient;

    @BeforeAll
    public static void setUp() {
        dataPlaneClientAndServer = startClientAndServer(DATA_PLANE_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(dataPlaneClientAndServer);
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        dataPlaneClientAndServer.reset();
    }

    @BeforeEach
    public void init() {
        var okHttpClient = testOkHttpClient();
        selectorClientMock = mock(DataPlaneSelectorClient.class);
        var selectionStrategy = FAKER.internet().uuid();
        transferClient = new RemoteDataPlaneTransferClient(okHttpClient, selectorClientMock, selectionStrategy, new RetryPolicy<>(), MAPPER);
    }

    @Test
    void noDpfInstance_shouldReturnFailedResultWithFatalError() {
        var flowRequest = createDataFlowRequest();
        when(selectorClientMock.find(any(), any(), any())).thenReturn(null);

        var result = transferClient.transfer(flowRequest);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s).contains("Failed to find data plane instance supporting request: " + flowRequest.getId()));
    }

    @Test
    void transferFailureWithNoBodyInResponse_shouldReturnFailedResultWithFatalError() throws MalformedURLException, JsonProcessingException {
        var flowRequest = createDataFlowRequest();

        // mock data plane selector
        var instance = mock(DataPlaneInstance.class);
        when(instance.getUrl()).thenReturn(new URL(DATA_PLANE_API_URI));
        when(selectorClientMock.find(any(), any(), any())).thenReturn(instance);

        // config data plane mock server
        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(flowRequest));
        dataPlaneClientAndServer.when(httpRequest, once()).respond(response().withStatusCode(HttpStatusCode.BAD_REQUEST_400.code()));

        var result = transferClient.transfer(flowRequest);

        dataPlaneClientAndServer.verify(httpRequest, VerificationTimes.once());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .isEqualTo("Transfer request failed with status code 400 for request %s: failed to read response body", flowRequest.getId())
                );
    }

    @Test
    void transferFailure_shouldReturnFailedResultWithErrors() throws MalformedURLException, JsonProcessingException {
        var flowRequest = createDataFlowRequest();

        // mock data plane selector
        var instance = mock(DataPlaneInstance.class);
        when(instance.getUrl()).thenReturn(new URL(DATA_PLANE_API_URI));
        when(selectorClientMock.find(any(), any(), any())).thenReturn(instance);

        // config data plane mock server
        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(flowRequest));
        var errorMsg = FAKER.internet().uuid();
        dataPlaneClientAndServer.when(httpRequest, once()).respond(withResponse(errorMsg));

        var result = transferClient.transfer(flowRequest);

        dataPlaneClientAndServer.verify(httpRequest, VerificationTimes.once());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .isEqualTo(format("Transfer request failed with status code 400 for request %s: %s", flowRequest.getId(), errorMsg))
                );
    }

    @Test
    void successTransfer() throws JsonProcessingException, MalformedURLException {
        var flowRequest = createDataFlowRequest();

        // mock data plane selector
        var instance = mock(DataPlaneInstance.class);
        when(instance.getUrl()).thenReturn(new URL(DATA_PLANE_API_URI));
        when(selectorClientMock.find(any(), any(), any())).thenReturn(instance);

        // config data plane mock server
        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(flowRequest));
        dataPlaneClientAndServer.when(httpRequest, once()).respond(response().withStatusCode(HttpStatusCode.OK_200.code()));

        var result = transferClient.transfer(flowRequest);

        dataPlaneClientAndServer.verify(httpRequest, VerificationTimes.once());

        assertThat(result.succeeded()).isTrue();
    }

    private HttpResponse withResponse(String errorMsg) throws JsonProcessingException {
        return response().withStatusCode(HttpStatusCode.BAD_REQUEST_400.code())
                .withBody(MAPPER.writeValueAsString(new TransferErrorResponse(List.of(errorMsg))), MediaType.APPLICATION_JSON);
    }
}
