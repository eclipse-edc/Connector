/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.iam.identitytrust.sts.controller;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.iam.identitytrust.sts.exception.StsTokenExceptionMapper;
import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenRequest;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class SecureServiceTokenApiControllerTest extends RestControllerTestBase {

    private static final String GRANT_TYPE = "client_credentials";
    private final StsAccountService clientService = mock();
    private final StsClientTokenGeneratorService tokenService = mock();
    private final Validator<StsTokenRequest> validator = mock();

    @Test
    void token() {
        var id = "id";
        var clientId = "clientId";
        var clientSecret = "client_secret";
        var clientKeyAlias = "secretAlias";
        var privateKeyAlias = "secretAlias";
        var did = "did:example:subject";
        var audience = "audience";
        var token = "token";
        var publicKeyReference = "publicKeyReference";
        var name = "Name";
        var expiresIn = 3600;

        var client = StsAccount.Builder.newInstance()
                .id(id)
                .clientId(clientId)
                .name(name)
                .secretAlias(clientKeyAlias)
                .privateKeyAlias(privateKeyAlias)
                .publicKeyReference(publicKeyReference)
                .did(did)
                .build();

        when(validator.validate(any())).thenReturn(ValidationResult.success());
        when(clientService.findByClientId(eq(clientId))).thenReturn(ServiceResult.success(client));
        when(clientService.authenticate(client, clientSecret)).thenReturn(ServiceResult.success(client));
        when(tokenService.tokenFor(eq(client), any())).thenReturn(ServiceResult.success(TokenRepresentation.Builder.newInstance()
                .token(token)
                .expiresIn((long) expiresIn)
                .build()));

        baseRequest()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", GRANT_TYPE)
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("audience", audience)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("access_token", is(token))
                .body("expires_in", is(expiresIn));
    }

    @Test
    void token_invalidClient_whenNotFound() {
        var id = "id";
        var clientId = "clientId";
        var clientSecret = "client_secret";
        var audience = "audience";
        var errorCode = "invalid_client";


        when(validator.validate(any())).thenReturn(ValidationResult.success());
        when(clientService.findByClientId(eq(clientId))).thenReturn(ServiceResult.notFound("Not found"));

        baseRequest()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", GRANT_TYPE)
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("audience", audience)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .contentType(JSON)
                .body("error", is(errorCode));
    }

    @Test
    void token_invalidClient_authenticationFails() {
        var id = "id";
        var clientId = "clientId";
        var clientSecret = "client_secret";
        var audience = "audience";
        var errorCode = "invalid_client";
        var client = createClient(id);

        when(validator.validate(any())).thenReturn(ValidationResult.success());
        when(clientService.findByClientId(eq(clientId))).thenReturn(ServiceResult.success(client));
        when(clientService.authenticate(client, clientSecret)).thenReturn(ServiceResult.unauthorized("failure"));


        baseRequest()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", GRANT_TYPE)
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("audience", audience)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .contentType(JSON)
                .body("error", is(errorCode));
    }

    @Override
    protected Object controller() {
        return new SecureTokenServiceApiController(clientService, tokenService, validator);
    }

    @Override
    protected Object additionalResource() {
        return new StsTokenExceptionMapper();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
