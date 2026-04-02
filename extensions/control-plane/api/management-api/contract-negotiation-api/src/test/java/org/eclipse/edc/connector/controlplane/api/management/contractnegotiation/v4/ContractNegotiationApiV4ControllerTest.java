/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v4;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.BaseContractNegotiationApiControllerTest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation.TERMINATE_NEGOTIATION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class ContractNegotiationApiV4ControllerTest extends BaseContractNegotiationApiControllerTest {

    @Override
    protected Object controller() {
        return new ContractNegotiationApiV4Controller(service, transformerRegistry, monitor, validatorRegistry, participantContextSupplier);
    }

    @Test
    void terminate_shouldCallService() {
        var command = new TerminateNegotiation("reason");
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiation.class))).thenReturn(Result.success(command));
        when(service.terminate(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(204);

        verify(service).terminate(refEq(new TerminateNegotiationCommand("cn1", command.reason())));
    }

    @Test
    void terminate_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiation.class))).thenReturn(Result.failure("error"));

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(400);

        verifyNoInteractions(service);
    }

    @Test
    void terminate_shouldReturnError_whenServiceFails() {
        var command = new TerminateNegotiation("reason");
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiation.class))).thenReturn(Result.success(command));
        when(service.terminate(any())).thenReturn(ServiceResult.conflict("conflict"));

        baseRequest()
                .body(Json.createObjectBuilder().build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(409);
    }

    @Test
    void terminate_shouldReturnBadRequest_whenTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(400);

        verify(validatorRegistry).validate(eq("v4:" + TERMINATE_NEGOTIATION_TYPE), any());
        verifyNoInteractions(transformerRegistry, service);
    }

    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v4beta/contractnegotiations")
                .when();
    }
}
