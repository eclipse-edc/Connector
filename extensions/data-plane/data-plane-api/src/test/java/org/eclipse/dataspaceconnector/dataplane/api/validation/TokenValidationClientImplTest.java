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

package org.eclipse.dataspaceconnector.dataplane.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.stop.Stop.stopQuietly;

class TokenValidationClientImplTest {

    private static final Faker FAKER = new Faker();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int PORT = getFreePort();
    private static final String TOKEN_VALIDATION_SERVER_URL = "http://localhost:" + PORT;
    private static ClientAndServer validationClientAndServer;

    private TokenValidationClientImpl client;

    @BeforeAll
    public static void startServer() {
        validationClientAndServer = startClientAndServer(PORT);
    }

    @AfterAll
    public static void stopServer() {
        stopQuietly(validationClientAndServer);
    }

    @BeforeEach
    public void setUp() {
        var httpClient = testOkHttpClient();
        client = new TokenValidationClientImpl(httpClient, TOKEN_VALIDATION_SERVER_URL, MAPPER, mock(Monitor.class));
    }

    @AfterEach
    public void tearDown() {
        validationClientAndServer.reset();
    }

    @Test
    void verifySuccessTokenValidation() throws JsonProcessingException {
        var token = FAKER.internet().uuid();
        var address = DataAddress.Builder.newInstance()
                .type(FAKER.lorem().word())
                .build();

        validationClientAndServer.when(new HttpRequest().withHeader(HttpHeaders.AUTHORIZATION, token), once())
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(MAPPER.writeValueAsString(address))
                        .withContentType(MediaType.APPLICATION_JSON));

        var result = client.call(token);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getType()).isEqualTo(address.getType());
    }

    @Test
    void verifyFailedResultReturnedIfServerResponseIsUnsuccessful() throws JsonProcessingException {
        var token = FAKER.internet().uuid();
        var address = DataAddress.Builder.newInstance()
                .type(FAKER.lorem().word())
                .build();

        validationClientAndServer.when(new HttpRequest().withHeader(HttpHeaders.AUTHORIZATION, token), once())
                .respond(HttpResponse.response()
                        .withStatusCode(400)
                        .withBody(MAPPER.writeValueAsString(address))
                        .withContentType(MediaType.APPLICATION_JSON));

        var result = client.call(token);

        assertThat(result.failed()).isTrue();
    }
}