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
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class DspVersionApiControllerTest extends RestControllerTestBase {

    private final VersionProtocolService service = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    @Test
    void shouldInvokeRequestHandler() {
        var versions = new ProtocolVersions(List.of(new ProtocolVersion("version", "/1.0", "binding")));
        var output = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "version")
                                .add("path", "/1.0")
                                .add("binding", "binding")).build()).build();

        when(service.getAll()).thenReturn(ServiceResult.success(versions));
        when(transformerRegistry.transform(eq(versions), eq(JsonObject.class))).thenReturn(Result.success(output));


        baseRequest()
                .get(".well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(APPLICATION_JSON)
                .body("protocolVersions[0].version", is("version"))
                .body("protocolVersions[0].path", is("/1.0"))
                .body("protocolVersions[0].binding", is("binding"));

    }

    @Override
    protected Object controller() {
        return new DspVersionApiController(service, transformerRegistry);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
