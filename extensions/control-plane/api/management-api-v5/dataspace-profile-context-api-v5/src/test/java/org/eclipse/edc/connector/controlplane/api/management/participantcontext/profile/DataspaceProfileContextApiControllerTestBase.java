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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class DataspaceProfileContextApiControllerTestBase extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final ParticipantContextService service = mock();
    protected final ParticipantProfileService profileResolver = mock();
    protected final AuthorizationService authorizationService = mock();

    @BeforeEach
    void setup() {
        when(transformerRegistry.transform(isA(DataspaceProfileContext.class), eq(JsonObject.class))).thenReturn(Result.success(createObjectBuilder().build()));
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath())
                .when();
    }

    protected abstract String versionPath();


    protected DataspaceProfileContext createProfile(String name) {
        var namespace = new JsonLdNamespace("http://test.com/");
        var protocol = new ProtocolVersion("test-protocol", "1.0", "https");
        return new DataspaceProfileContext(name, protocol, mock(), mock(), namespace, List.of("http://test.com/context"), List.of());
    }

    @Nested
    class GetParticipantProfiles {
        @Test
        void get() {
            var profile = createProfile("test-profile");
            when(profileResolver.resolveAll("participantContextId")).thenReturn(List.of(profile));
            baseRequest()
                    .get("/participants/participantContextId/profiles")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1));
            verify(transformerRegistry).transform(isA(DataspaceProfileContext.class), eq(JsonObject.class));
        }
    }

    @Nested
    class AssociateProfiles {

        @Test
        void associate() {
            var associated = new AssociateDataspaceProfileContext(List.of("a", "b"));
            when(transformerRegistry.transform(isA(JsonObject.class), eq(AssociateDataspaceProfileContext.class)))
                    .thenReturn(Result.success(associated));
            when(profileResolver.associateProfiles(eq("participantContextId"), eq(List.of("a", "b"))))
                    .thenReturn(ServiceResult.success());

            baseRequest()
                    .contentType(JSON)
                    .body(associateRequestBody())
                    .put("/participants/participantContextId/profiles")
                    .then()
                    .statusCode(204);

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(AssociateDataspaceProfileContext.class));
            verify(profileResolver).associateProfiles("participantContextId", List.of("a", "b"));
        }

        @Test
        void associate_shouldReturnBadRequest_whenTransformationFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(AssociateDataspaceProfileContext.class)))
                    .thenReturn(Result.failure("malformed"));

            baseRequest()
                    .contentType(JSON)
                    .body(associateRequestBody())
                    .put("/participants/participantContextId/profiles")
                    .then()
                    .statusCode(400);

            verify(profileResolver, never()).associateProfiles(any(), any());
        }

        @Test
        void associate_shouldReturnBadRequest_whenAssociationFails() {
            var associated = new AssociateDataspaceProfileContext(List.of("unknown"));
            when(transformerRegistry.transform(isA(JsonObject.class), eq(AssociateDataspaceProfileContext.class)))
                    .thenReturn(Result.success(associated));
            when(profileResolver.associateProfiles(eq("participantContextId"), eq(List.of("unknown"))))
                    .thenReturn(ServiceResult.badRequest("Profile unknown does not exist"));

            baseRequest()
                    .contentType(JSON)
                    .body(associateRequestBody())
                    .put("/participants/participantContextId/profiles")
                    .then()
                    .statusCode(400);

            verify(profileResolver).associateProfiles("participantContextId", List.of("unknown"));
        }

        private String associateRequestBody() {
            return Json.createObjectBuilder()
                    .add("@context", Json.createObjectBuilder().add("@vocab", "https://w3id.org/edc/v0.0.1/ns/"))
                    .add("@type", "AssociateDataspaceProfile")
                    .add("profiles", Json.createArrayBuilder(List.of("a", "b")))
                    .build()
                    .toString();
        }
    }
}
