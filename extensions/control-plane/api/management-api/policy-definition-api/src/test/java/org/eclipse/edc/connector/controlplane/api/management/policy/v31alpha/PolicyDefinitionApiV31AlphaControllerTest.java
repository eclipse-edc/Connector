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
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.policy.v31alpha;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.policy.BasePolicyDefinitionApiControllerTest;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PolicyDefinitionApiV31AlphaControllerTest extends BasePolicyDefinitionApiControllerTest {


    @Test
    void validate_shouldReturnNotFound_whenNotFound() {
        when(service.findById(any())).thenReturn(null);

        baseRequest()
                .contentType(JSON)
                .post("/id/validate")
                .then()
                .statusCode(404);
    }


    @Test
    void validate_shouldReturnValid_whenValidationSucceed() {

        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

        when(service.findById(any())).thenReturn(policyDefinition);
        when(service.validate(policyDefinition.getPolicy())).thenReturn(Result.success());
        when(transformerRegistry.transform(any(PolicyValidationResult.class), eq(JsonObject.class))).then(answer -> {
            PolicyValidationResult result = answer.getArgument(0);
            var response = Json.createObjectBuilder()
                    .add("isValid", result.isValid())
                    .add("errors", Json.createArrayBuilder(result.errors()))
                    .build();
            return Result.success(response);
        });

        baseRequest()
                .contentType(JSON)
                .post("/id/validate")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("isValid", is(true))
                .body("errors.size()", is(0));
    }

    @Test
    void validate_shouldReturnInvalidValid_whenValidationFails() {

        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();


        when(service.findById(any())).thenReturn(policyDefinition);
        when(service.validate(policyDefinition.getPolicy())).thenReturn(Result.failure(List.of("error1", "error2")));
        when(transformerRegistry.transform(any(PolicyValidationResult.class), eq(JsonObject.class))).then(answer -> {
            PolicyValidationResult result = answer.getArgument(0);
            var response = Json.createObjectBuilder()
                    .add("isValid", result.isValid())
                    .add("errors", Json.createArrayBuilder(result.errors()))
                    .build();
            return Result.success(response);
        });

        baseRequest()
                .contentType(JSON)
                .post("/id/validate")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("isValid", is(false))
                .body("errors.size()", is(2));
    }

    @Override
    protected Object controller() {
        return new PolicyDefinitionApiV31AlphaController(monitor, transformerRegistry, service, validatorRegistry);
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:%d/v3.1alpha/policydefinitions".formatted(port))
                .port(port);
    }

}
