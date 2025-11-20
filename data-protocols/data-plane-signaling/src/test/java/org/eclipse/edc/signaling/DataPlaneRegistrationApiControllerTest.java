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

package org.eclipse.edc.signaling;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.signaling.port.DataPlaneRegistrationApiController;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataPlaneRegistrationApiControllerTest extends RestControllerTestBase {

    private final DataPlaneSelectorService dataPlaneSelectorService = mock();

    @Nested
    class Register {
        @Test
        void shouldRegisterNewDataplane() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.success());
            var requestBody = Map.of(
                    "dataplaneId", "dataplane-id",
                    "name", "dataplane-name",
                    "description", "dataplane-description",
                    "endpoint", "http://dataplane-endpoint",
                    "transferTypes", List.of("transferType-PUSH", "transferType-PULL"),
                    "labels", List.of("label-1", "label-2")
            );

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .post("/dataplanes/register")
                    .then()
                    .statusCode(200);

            var captor = ArgumentCaptor.forClass(DataPlaneInstance.class);
            verify(dataPlaneSelectorService).register(captor.capture());
            var instance = captor.getValue();
            assertThat(instance.getId()).isEqualTo("dataplane-id");
            assertThat(instance.getUrl().toString()).isEqualTo("http://dataplane-endpoint");
            assertThat(instance.getAllowedTransferTypes()).containsExactly("transferType-PUSH", "transferType-PULL");
        }

        @Test
        void shouldReturnError_whenRegistrationFails() {
            when(dataPlaneSelectorService.register(any())).thenReturn(ServiceResult.conflict("error"));

            var requestBody = Map.of(
                    "dataplaneId", "dataplane-id",
                    "name", "dataplane-name",
                    "description", "dataplane-description",
                    "endpoint", "http://dataplane-endpoint",
                    "transferTypes", List.of("transferType-PUSH", "transferType-PULL"),
                    "labels", List.of("label-1", "label-2")
            );

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .post("/dataplanes/register")
                    .then()
                    .statusCode(409);
        }
    }

    @Nested
    class Update {
        @Test
        void shouldUpdateExistingDataPlan() {
            when(dataPlaneSelectorService.update(any())).thenReturn(ServiceResult.success());
            var requestBody = Map.of(
                    "dataplaneId", "dataplane-id",
                    "name", "dataplane-name",
                    "description", "dataplane-description",
                    "endpoint", "http://dataplane-endpoint",
                    "transferTypes", List.of("transferType-PUSH", "transferType-PULL"),
                    "labels", List.of("label-1", "label-2")
            );

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .put("/dataplanes/dataplane-id")
                    .then()
                    .statusCode(200);

            var captor = ArgumentCaptor.forClass(DataPlaneInstance.class);
            verify(dataPlaneSelectorService).update(captor.capture());
            var instance = captor.getValue();
            assertThat(instance.getId()).isEqualTo("dataplane-id");
            assertThat(instance.getUrl().toString()).isEqualTo("http://dataplane-endpoint");
            assertThat(instance.getAllowedTransferTypes()).containsExactly("transferType-PUSH", "transferType-PULL");
        }

        @Test
        void shouldReturnError_whenUpdateFails() {
            when(dataPlaneSelectorService.update(any())).thenReturn(ServiceResult.conflict("error"));

            var requestBody = Map.of(
                    "dataplaneId", "dataplane-id",
                    "name", "dataplane-name",
                    "description", "dataplane-description",
                    "endpoint", "http://dataplane-endpoint",
                    "transferTypes", List.of("transferType-PUSH", "transferType-PULL"),
                    "labels", List.of("label-1", "label-2")
            );

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .put("/dataplanes/dataplane-id")
                    .then()
                    .statusCode(409);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteDataPlane() {
            when(dataPlaneSelectorService.delete(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .contentType(ContentType.JSON)
                    .delete("/dataplanes/dataplane-id")
                    .then()
                    .statusCode(200);

            verify(dataPlaneSelectorService).delete("dataplane-id");
        }

        @Test
        void shouldReturnError_whenDeletionFails() {
            when(dataPlaneSelectorService.delete(any())).thenReturn(ServiceResult.conflict("error"));

            baseRequest()
                    .contentType(ContentType.JSON)
                    .delete("/dataplanes/dataplane-id")
                    .then()
                    .statusCode(409);
        }
    }

    @Override
    protected Object controller() {
        return new DataPlaneRegistrationApiController(dataPlaneSelectorService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
