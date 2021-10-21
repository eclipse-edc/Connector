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
package org.eclipse.dataspaceconnector.transfer.functions.core.flow.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.easymock.EasyMock.createNiceMock;
import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.FATAL_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies HTTP transfer function flow.
 */
class HttpFunctionDataFlowControllerTest {
    private final TypeManager typeManager = new TypeManager();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9090));
    private final HttpFunctionConfiguration configuration = HttpFunctionConfiguration.Builder.newInstance()
            .transferEndpoint("http://localhost:9090/check")
            .clientSupplier(() -> httpClient)
            .monitor(createNiceMock(Monitor.class))
            .typeManager(typeManager)
            .build();

    private final HttpFunctionDataFlowController flowController = new HttpFunctionDataFlowController(configuration);

    @BeforeEach
    void setUp() {
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void verifyOkResponse() {
        wireMockServer.stubFor(post("/check").willReturn(ok()));
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).build();

        DataFlowInitiateResponse response = flowController.initiateFlow(dataRequest);

        assertEquals(DataFlowInitiateResponse.OK, response);
    }

    @Test
    void verifyRetryErrorResponse() {
        wireMockServer.stubFor(post("/check").willReturn(serverError()));
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).build();

        DataFlowInitiateResponse response = flowController.initiateFlow(dataRequest);

        assertEquals(ERROR_RETRY, response.getStatus());
    }

    @Test
    void verifyFatalErrorResponse() {
        wireMockServer.stubFor(post("/check").willReturn(badRequest()));
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).build();

        DataFlowInitiateResponse response = flowController.initiateFlow(dataRequest);

        assertEquals(FATAL_ERROR, response.getStatus());
    }

}
