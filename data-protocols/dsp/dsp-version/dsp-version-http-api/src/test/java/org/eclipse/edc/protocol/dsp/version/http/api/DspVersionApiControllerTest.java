/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Cofinity-X - unauthenticated DSP version endpoint
 *
 */

package org.eclipse.edc.protocol.dsp.version.http.api;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DspVersionApiControllerTest extends RestControllerTestBase {

    private final VersionProtocolService service = mock();
    private final DspRequestHandler requestHandler = mock();

    @Test
    void shouldInvokeRequestHandler() {
        var output = Json.createObjectBuilder().add("protocolVersions", Json.createArrayBuilder().add(Json.createObjectBuilder().add("version", "1.0")).build()).build();
        when(requestHandler.getResource(any())).thenReturn(Response.ok(output).build());

        baseRequest()
                .get(".well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(APPLICATION_JSON)
                .body("protocolVersions[0].version", is("1.0"));

        var captor = ArgumentCaptor.forClass(GetDspRequest.class);
        verify(requestHandler).getResource(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("no-auth-required");
    }
    
    @Test
    void whenAuthorizationHeaderSet_shouldIgnoreToken() {
        var output = Json.createObjectBuilder().add("protocolVersions", Json.createArrayBuilder().add(Json.createObjectBuilder().add("version", "1.0")).build()).build();
        when(requestHandler.getResource(any())).thenReturn(Response.ok(output).build());
        
        baseRequest()
                .header(AUTHORIZATION, "token")
                .get(".well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(APPLICATION_JSON)
                .body("protocolVersions[0].version", is("1.0"));
        
        var captor = ArgumentCaptor.forClass(GetDspRequest.class);
        verify(requestHandler).getResource(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("no-auth-required");
    }

    @Override
    protected Object controller() {
        return new DspVersionApiController(requestHandler, service);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
