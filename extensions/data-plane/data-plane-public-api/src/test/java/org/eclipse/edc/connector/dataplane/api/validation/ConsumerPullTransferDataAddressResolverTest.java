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

package org.eclipse.edc.connector.dataplane.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.stop.Stop.stopQuietly;

class ConsumerPullTransferDataAddressResolverTest {

    private static final ObjectMapper MAPPER = new TypeManager().getMapper();
    private static final int PORT = getFreePort();
    private static final String TOKEN_VALIDATION_SERVER_URL = "http://localhost:" + PORT;
    private static ClientAndServer validationClientAndServer;

    private ConsumerPullTransferDataAddressResolver resolver;

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
        resolver = new ConsumerPullTransferDataAddressResolver(testHttpClient(), TOKEN_VALIDATION_SERVER_URL, MAPPER);
    }

    @AfterEach
    public void tearDown() {
        validationClientAndServer.reset();
    }

    @Test
    void verifySuccessTokenValidation() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var address = DataAddress.Builder.newInstance()
                .type("test-type")
                .build();

        validationClientAndServer.when(new HttpRequest().withHeader(HttpHeaders.AUTHORIZATION, token), once())
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(MAPPER.writeValueAsString(address))
                        .withContentType(MediaType.APPLICATION_JSON));

        var result = resolver.resolve(token);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getType()).isEqualTo(address.getType());
    }

    @Test
    void verifyFailedResultReturnedIfServerResponseIsUnsuccessful() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var address = DataAddress.Builder.newInstance()
                .type("test-type")
                .build();

        validationClientAndServer.when(new HttpRequest().withHeader(HttpHeaders.AUTHORIZATION, token), once())
                .respond(HttpResponse.response()
                        .withStatusCode(400)
                        .withBody(MAPPER.writeValueAsString(address))
                        .withContentType(MediaType.APPLICATION_JSON));

        var result = resolver.resolve(token);

        assertThat(result.failed()).isTrue();
    }
}
