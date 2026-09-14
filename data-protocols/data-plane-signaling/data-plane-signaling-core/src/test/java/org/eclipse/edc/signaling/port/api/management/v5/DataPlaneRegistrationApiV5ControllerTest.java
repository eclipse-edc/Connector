/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api.management.v5;

import io.restassured.http.ContentType;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.signaling.domain.DataPlaneRegistrationMessage;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneRegistrationApiV5ControllerTest extends RestControllerTestBase {

    protected final AuthorizationService authorizationService = mock();
    private final DataPlaneSelectorService dataPlaneSelectorService = mock();
    private final String participantContextId = "test-participant-context-id";

    @Override
    protected Object controller() {
        return new org.eclipse.edc.signaling.port.api.management.v5.DataPlaneRegistrationApiV5Controller(dataPlaneSelectorService, authorizationService);
    }

    @BeforeEach
    public void setup() {
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
        when(dataPlaneSelectorService.findById(any())).thenReturn(ServiceResult.notFound("not found"));
    }

    @Nested
    class Register {

        @Test
        void shouldRegisterDataPlane() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.success());
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of("HttpData-PUSH"), Set.of("label"), null);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5/participants/%s/dataplanes".formatted(participantContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(dataPlaneSelectorService).register(argThat(instance ->
                    instance.getId().equals("dp-id") &&
                            instance.getUrl().toString().equals("http://dataplane/endpoint") &&
                            instance.getAllowedTransferTypes().contains("HttpData-PUSH") &&
                            instance.getLabels().contains("label") &&
                            instance.getAuthorizationProfile() == null
            ));
        }

        @Test
        void shouldRegisterDataPlane_withAuthorizationProfile() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.success());
            var authProfile = Map.<String, Object>of("type", "oauth2", "tokenUrl", "http://token-url");
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint",
                    Set.of("HttpData-PUSH"), Set.of(), authProfile);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5/participants/%s/dataplanes".formatted(participantContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(dataPlaneSelectorService).register(argThat(instance ->
                    instance.getAuthorizationProfile() != null &&
                            instance.getAuthorizationProfile().type().equals("oauth2")
            ));
        }

        @Test
        void shouldRegisterDataPlane_whenAlreadyRegisteredBySameParticipantContext() {
            var existing = DataPlaneInstance.Builder.newInstance().id("dp-id").url("http://old/endpoint")
                    .participantContextId(participantContextId).build();
            when(dataPlaneSelectorService.findById("dp-id")).thenReturn(ServiceResult.success(existing));
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.success());
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of("HttpData-PUSH"), Set.of(), null);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5/participants/%s/dataplanes".formatted(participantContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(dataPlaneSelectorService).register(argThat(instance -> instance.getId().equals("dp-id")));
        }

        @Test
        void shouldReturnConflict_whenAlreadyRegisteredByAnotherParticipantContext() {
            var existing = DataPlaneInstance.Builder.newInstance().id("dp-id").url("http://other/endpoint")
                    .participantContextId("another-participant-context-id").build();
            when(dataPlaneSelectorService.findById("dp-id")).thenReturn(ServiceResult.success(existing));
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of("HttpData-PUSH"), Set.of(), null);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5/participants/%s/dataplanes".formatted(participantContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);

            verify(dataPlaneSelectorService, never()).register(any());
        }

        @Test
        void shouldReturnError_whenServiceCallFails() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.conflict("already exists"));
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of(), Set.of(), null);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5/participants/{participantContextId}/dataplanes", participantContextId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteDataPlane() {
            when(dataPlaneSelectorService.delete(any())).thenReturn(ServiceResult.success());

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .delete("/v5/participants/{participantContextId}/dataplanes/{dataplaneId}", participantContextId, "dp-id")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(dataPlaneSelectorService).delete("dp-id");
        }

        @Test
        void shouldReturnNotFound_whenDataPlaneDoesNotExist() {
            when(dataPlaneSelectorService.delete(any())).thenReturn(ServiceResult.notFound("not found"));

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .delete("/v5/participants/{participantContextId}/dataplanes/{dataplaneId}", participantContextId, "dp-id")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }
    }
}
