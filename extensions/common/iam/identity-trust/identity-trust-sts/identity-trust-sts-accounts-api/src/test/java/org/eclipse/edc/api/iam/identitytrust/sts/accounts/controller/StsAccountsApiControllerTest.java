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

package org.eclipse.edc.api.iam.identitytrust.sts.accounts.controller;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.StsAccountCreation;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.UpdateClientSecret;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StsAccountsApiControllerTest extends RestControllerTestBase {

    private final StsAccountService accountServiceMock = mock();

    private static StsAccount.Builder createAccount() {
        return StsAccount.Builder.newInstance()
                .id("account-id")
                .did("did:web:test")
                .name("test-name")
                .clientId("test-client-id")
                .secretAlias("test-alias")
                .privateKeyAlias("test-private-key")
                .publicKeyReference("test-public-key");
    }

    @Test
    void createAccount_withSecret() {
        var secret = "sup3r$ecr3t";
        var account = createAccount().build();
        var accountCreate = new StsAccountCreation(account, secret);
        baseRequest()
                .contentType("application/json")
                .body(accountCreate)
                .post()
                .then()
                .statusCode(200)
                .body(".clientSecret", equalTo(secret));
    }


    @Test
    void createAccount_withoutSecret() {
        var account = createAccount().build();
        var accountCreate = new StsAccountCreation(account, null);
        baseRequest()
                .contentType("application/json")
                .body(accountCreate)
                .post()
                .then()
                .statusCode(200)
                .body(".clientSecret", notNullValue());
    }

    @Test
    void updateAccount() {
    }

    @Test
    void updateAccount_whenNotFound_expect404() {
        var account = createAccount().build();
        baseRequest()
                .contentType("application/json")
                .body(account)
                .put()
                .then()
                .statusCode(404);
    }

    @Test
    void getAccount() {
        var account = createAccount().build();
        when(accountServiceMock.findByClientId(eq(account.getId()))).thenReturn(ServiceResult.success(account));

        baseRequest()
                .contentType("application/json")
                .get("/" + account.getId())
                .then()
                .statusCode(200)
                .body(equalTo(account));

    }

    @Test
    void getAccount_whenNotFound_expect404() {
        baseRequest()
                .contentType("application/json")
                .get("/notexist")
                .then()
                .statusCode(404);
    }

    @Test
    void queryAccounts() {
        var query = QuerySpec.max();

        var acc1 = createAccount().build();
        var acc2 = createAccount().build();
        // todo: implement service method
        // when(accountServiceMock.query(any(QuerySpec.class))).thenReturn(List.of(createAccount().build()));

        var result = baseRequest()
                .contentType("application/json")
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .extract()
                .body().as(StsAccount[].class);

        assertThat(result).isNotNull().containsExactlyInAnyOrder(acc1, acc2);
    }

    @Test
    void queryAccounts_whenNoResult() {
        var query = QuerySpec.max();

        var result = baseRequest()
                .contentType("application/json")
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .extract()
                .body().as(StsAccount[].class);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void updateClientSecret_newAlias_noSecret() {

        var request = new UpdateClientSecret("new-alias", null);
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/account-id/secret")
                .then()
                .statusCode(200)
                .body(equalTo("new-alias"));
    }

    @Test
    void updateClientSecret_newAlias_withSecret() {

        var request = new UpdateClientSecret("new-alias", "new-secret");
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/account-id/secret")
                .then()
                .statusCode(200)
                .body(equalTo("new-alias"));
    }

    @Test
    void updateClientSecret_sameAlias_noSecret() {

        var request = new UpdateClientSecret("same-alias", null);
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/account-id/secret")
                .then()
                .statusCode(200)
                .body(equalTo("same-alias"));
    }


    @Test
    void updateClientSecret_whenNotFound_expect404() {

        var request = new UpdateClientSecret("new-alias", null);
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/not-exists-id/secret")
                .then()
                .statusCode(404);
    }


    @Test
    void deleteAccount() {
        baseRequest()
                .contentType("application/json")
                .delete("/account-id")
                .then()
                .statusCode(200);

        // todo: implement
        // verify(accountServiceMock).deleteById(eq("account-id"));
    }

    @Test
    void deleteAccount_whenNotFound_expect404() {
        baseRequest()
                .contentType("application/json")
                .delete("/account-id")
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new StsAccountsApiController(accountServiceMock);
    }

    private RequestSpecification baseRequest() {
        return given()
                .port(port)
                .baseUri("http://localhost:" + port + "/v1/accounts");
    }
}