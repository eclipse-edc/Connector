/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.extension.jersey.CorsFilterConfiguration;
import org.eclipse.dataspaceconnector.extension.jersey.JerseyRestService;
import org.eclipse.dataspaceconnector.extension.jetty.JettyConfiguration;
import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.extension.jetty.PortMapping;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.Mockito.mock;

class ContractNegotiationApiControllerIntegrationTest {
    private static int port;
    private OkHttpClient client;

    @BeforeAll
    static void prepareWebserver() {
        port = TestUtils.getFreePort();
        var monitor = mock(Monitor.class);
        var config = new JettyConfiguration(null, null);
        config.portMapping(new PortMapping("data", port, "/api/v1/data"));
        var jetty = new JettyService(config, monitor);

        var controller = new ContractNegotiationController(monitor);
        var jerseyService = new JerseyRestService(jetty, new TypeManager(), mock(CorsFilterConfiguration.class), monitor);
        jetty.start();
        jerseyService.registerResource("data", controller);
        jerseyService.start();
    }

    @BeforeEach
    void setup() {
        client = testOkHttpClient();
    }

    @Test
    void getAllContractNegotiations() throws IOException {
        var response = get(basePath());
        assertThat(response.code()).isEqualTo(200);
    }

    @Test
    void getAllContractNegotiations_withPaging() throws IOException {
        try (var response = get(basePath() + "?offset=10&limit=15&sort=ASC")) {
            assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    void getSingleContractNegotation() throws IOException {
        var id = "test-id";
        try (var response = get(basePath() + "/" + id)) {
            //assertThat(response.code()).isEqualTo(200);
        }

    }

    @Test
    void getSingleContractNegotation_notFound() throws IOException {
        try (var response = get(basePath() + "/not-exist")) {
            // assertThat(response.code()).isEqualTo(404);
        }
    }

    @NotNull
    private String basePath() {
        return "http://localhost:" + port + "/api/v1/data/contractnegotiations";
    }

    @NotNull
    private Response get(String url) throws IOException {
        return client.newCall(new Request.Builder().get().url(url).build()).execute();
    }
}
