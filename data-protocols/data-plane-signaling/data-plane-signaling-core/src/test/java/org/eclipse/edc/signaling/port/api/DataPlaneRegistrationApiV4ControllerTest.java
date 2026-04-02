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

package org.eclipse.edc.signaling.port.api;

import io.restassured.http.ContentType;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.signaling.domain.DataPlaneRegistrationMessage;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneRegistrationApiV4ControllerTest extends RestControllerTestBase {

    private final DataPlaneSelectorService dataPlaneSelectorService = mock();

    @Nested
    class Register {

        @Test
        void shouldRegisterDataPlane() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.success());
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of("HttpData-PUSH"), Set.of(), null);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v4beta/dataplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(dataPlaneSelectorService).register(argThat(instance ->
                    instance.getId().equals("dp-id") &&
                    instance.getUrl().toString().equals("http://dataplane/endpoint") &&
                    instance.getAllowedTransferTypes().contains("HttpData-PUSH") &&
                    instance.getAuthorizationProfiles().isEmpty()
            ));
        }

        @Test
        void shouldRegisterDataPlane_withAuthorizationProfiles() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.success());
            var authProfile = Map.<String, Object>of("type", "oauth2", "tokenUrl", "http://token-url");
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of("HttpData-PUSH"), Set.of(), List.of(authProfile));

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v4beta/dataplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            verify(dataPlaneSelectorService).register(argThat(instance ->
                    instance.getAuthorizationProfiles().size() == 1 &&
                    instance.getAuthorizationProfiles().get(0).type().equals("oauth2")
            ));
        }

        @Test
        void shouldReturnError_whenServiceCallFails() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.conflict("already exists"));
            var message = new DataPlaneRegistrationMessage("dp-id", "http://dataplane/endpoint", Set.of(), Set.of(), null);

            given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v4beta/dataplanes")
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
                    .delete("/v4beta/dataplanes/{dataplaneId}", "dp-id")
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
                    .delete("/v4beta/dataplanes/{dataplaneId}", "dp-id")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }
    }

    @Override
    protected Object controller() {
        return new DataPlaneRegistrationApiV4Controller(dataPlaneSelectorService);
    }
}
