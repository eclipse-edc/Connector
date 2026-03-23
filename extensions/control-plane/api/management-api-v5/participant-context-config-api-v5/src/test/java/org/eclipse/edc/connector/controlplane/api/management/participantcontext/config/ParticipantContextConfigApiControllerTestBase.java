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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.config;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public abstract class ParticipantContextConfigApiControllerTestBase extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final ParticipantContextConfigService service = mock();

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath())
                .when();
    }

    protected abstract String versionPath();

    private ParticipantContextConfiguration createParticipantContextConfig() {
        return createParticipantContextBuilder()
                .participantContextId(UUID.randomUUID().toString())
                .entries(Map.of())
                .build();
    }

    private ParticipantContextConfiguration.Builder createParticipantContextBuilder() {
        return ParticipantContextConfiguration.Builder.newInstance();
    }

    @Nested
    class Get {
        @Test
        void get() {
            var participantContext = createParticipantContextConfig();
            var expandedBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(service.get(any())).thenReturn(ServiceResult.success(participantContext));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

            baseRequest()
                    .get("/participants/" + participantContext.getParticipantContextId() + "/config")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is("id"))
                    .body("createdAt", is(1234));
            verify(service).get(participantContext.getParticipantContextId());
            verify(transformerRegistry).transform(participantContext, JsonObject.class);
        }

        @Test
        void get_shouldReturnNotFound_whenNotFound() {
            when(service.get(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .get("/participants/id/config")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
            verifyNoInteractions(transformerRegistry);
        }

    }

    @Nested
    class Set {
        @Test
        void set() {
            var contextConfig = createParticipantContextConfig();
            when(transformerRegistry.transform(any(), eq(ParticipantContextConfiguration.class))).thenReturn(Result.success(contextConfig));

            when(service.save(any())).thenReturn(ServiceResult.success());
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/participants/" + contextConfig.getParticipantContextId() + "/config")
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(ParticipantContextConfiguration.class));
            verify(service).save(argThat(ctx ->
                    ctx.getParticipantContextId().equals(contextConfig.getParticipantContextId()) &&
                            ctx.getEntries().equals(contextConfig.getEntries())));
        }

        @Test
        void set_shouldReturnBadRequest_whenTransformationFails() {
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/participants/id/config")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }
    }

}
