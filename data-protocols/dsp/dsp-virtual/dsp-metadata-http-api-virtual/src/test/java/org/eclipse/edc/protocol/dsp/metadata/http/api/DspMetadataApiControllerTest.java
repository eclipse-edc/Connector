/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.metadata.http.api;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class DspMetadataApiControllerTest extends RestControllerTestBase {

    private final DataspaceProfileContextRegistry profileContextRegistry = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final ParticipantContextService participantContextService = mock();

    @Test
    void shouldInvokeRequestHandler() {
        var participantContextId = "participantcontextId";

        when(participantContextService.getParticipantContext(participantContextId))
                .thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().identity("identity")
                        .participantContextId("participantcontextId").build()));
        var protocolVersion = new ProtocolVersion("version", "/1.0", "binding");
        var output = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", protocolVersion.version())
                                .add("path", protocolVersion.path())
                                .add("binding", protocolVersion.binding())).build()).build();

        var profile = new DataspaceProfileContext("profileId", protocolVersion, mock(), mock());

        when(profileContextRegistry.getProfiles()).thenReturn(List.of(profile));
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(output));


        baseRequest()
                .get("participantcontextId/.well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(APPLICATION_JSON)
                .body("protocolVersions[0].version", is(protocolVersion.version()))
                .body("protocolVersions[0].path", is(protocolVersion.path()))
                .body("protocolVersions[0].binding", is(protocolVersion.binding()));

    }

    @Test
    void shouldReturnNotFound_whenParticipantContextDoesNotExists() {
        var participantContextId = "participantcontextId";

        when(participantContextService.getParticipantContext(participantContextId))
                .thenReturn(ServiceResult.notFound("not found"));

        when(transformerRegistry.transform(isA(VersionsError.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));

        baseRequest()
                .get("participantcontextId/.well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(404);

    }

    @Override
    protected Object controller() {
        return new DspMetadataApiController(participantContextService, profileContextRegistry, transformerRegistry);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
