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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.easymock.EasyMock.createNiceMock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verfies HTTP status checking.
 */
class HttpStatusCheckerTest {
    private final TypeManager typeManager = new TypeManager();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final HttpFunctionConfiguration configuration = HttpFunctionConfiguration.Builder.newInstance()
            .checkEndpoint("http://localhost:9090/check")
            .clientSupplier(() -> httpClient)
            .monitor(createNiceMock(Monitor.class))
            .typeManager(typeManager)
            .build();
    private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9090));

    private final HttpStatusChecker checker = new HttpStatusChecker(configuration);

    @BeforeEach
    void setUp() {
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void verifyCompleted() {
        wireMockServer.stubFor(get("/check").willReturn(okForJson(true)));
        TransferProcess transferProcess = TransferProcess.Builder.newInstance().id("123").build();

        boolean isComplete = checker.isComplete(transferProcess, Collections.emptyList());

        assertTrue(isComplete);
    }

    @Test
    void verifyNotCompleted() {
        wireMockServer.stubFor(get("/check").willReturn(okForJson(false)));
        TransferProcess transferProcess = TransferProcess.Builder.newInstance().id("123").build();

        boolean isComplete = checker.isComplete(transferProcess, Collections.emptyList());

        assertFalse(isComplete);
    }

    @Test
    void verifyServerError() {
        wireMockServer.stubFor(get("/check").willReturn(serverError()));
        TransferProcess transferProcess = TransferProcess.Builder.newInstance().id("123").build();

        boolean isComplete = checker.isComplete(transferProcess, Collections.emptyList());

        assertFalse(isComplete);
    }

}
