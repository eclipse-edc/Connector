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
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.StsAccountCreation;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.UpdateClientSecret;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        when(accountServiceMock.create(any(StsAccount.class), eq(secret))).thenReturn(ServiceResult.success(secret));

        baseRequest()
                .contentType("application/json")
                .body(accountCreate)
                .post()
                .then()
                .statusCode(200)
                .body("clientSecret", equalTo(secret));
    }


    @Test
    void createAccount_withoutSecret() {
        var account = createAccount().build();
        var accountCreate = new StsAccountCreation(account, null);

        when(accountServiceMock.create(any(StsAccount.class), isNull())).thenReturn(ServiceResult.success(UUID.randomUUID().toString()));

        baseRequest()
                .contentType("application/json")
                .body(accountCreate)
                .post()
                .then()
                .statusCode(200)
                .body("clientSecret", notNullValue());
    }

    @Test
    void updateAccount() {
        when(accountServiceMock.update(any())).thenReturn(ServiceResult.success());
        var account = createAccount().build();
        baseRequest()
                .contentType("application/json")
                .body(account)
                .put()
                .then()
                .statusCode(204);

        verify(accountServiceMock).update(any(StsAccount.class));
    }

    @Test
    void updateAccount_whenNotFound_expect404() {
        when(accountServiceMock.update(any())).thenReturn(ServiceResult.notFound("foo"));

        var account = createAccount().build();
        baseRequest()
                .contentType("application/json")
                .body(account)
                .put()
                .then()
                .statusCode(404)
                .body(containsString("Object of type StsAccount with ID=account-id was not found"));
    }

    @Test
    void getAccount() {
        var account = createAccount().build();
        when(accountServiceMock.findById(eq(account.getId()))).thenReturn(ServiceResult.success(account));

        var result = baseRequest()
                .contentType("application/json")
                .get("/" + account.getId())
                .then()
                .statusCode(200)
                .extract().body().as(StsAccount.class);

        var config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields("clock")
                .build();
        assertThat(result).usingRecursiveComparison(config).isEqualTo(account);

    }

    @Test
    void getAccount_whenNotFound_expect404() {
        when(accountServiceMock.findById(anyString())).thenReturn(ServiceResult.notFound("foo"));
        baseRequest()
                .contentType("application/json")
                .get("/notexist")
                .then()
                .statusCode(404);
    }

    @Test
    void queryAccounts() {
        var query = QuerySpec.max();

        var acc1 = createAccount().id("id1").build();
        var acc2 = createAccount().id("id2").build();
        // todo: implement service method
        when(accountServiceMock.queryAccounts(any(QuerySpec.class))).thenReturn(List.of(acc1, acc2));

        var result = baseRequest()
                .contentType("application/json")
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .extract()
                .body().as(StsAccount[].class);

        assertThat(result).isNotNull().extracting(StsAccount::getId).containsExactlyInAnyOrder(acc1.getId(), acc2.getId());
    }

    @Test
    void queryAccounts_whenNoResult() {
        var query = QuerySpec.max();
        when(accountServiceMock.queryAccounts(any(QuerySpec.class))).thenReturn(Collections.emptyList());

        var result = baseRequest()
                .contentType("application/json")
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .extract()
                .body().as(StsAccount[].class);

        assertThat(result).isNotNull().isEmpty();
        verify(accountServiceMock).queryAccounts(eq(query));
    }

    @Test
    void updateClientSecret_newAlias_noSecret() {

        when(accountServiceMock.updateSecret(anyString(), eq("new-alias"), isNull())).thenReturn(ServiceResult.success("new-alias"));

        var request = new UpdateClientSecret("new-alias", null);
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/account-id/secret")
                .then()
                .statusCode(200)
                .body(equalTo("new-alias"));
        verify(accountServiceMock).updateSecret(eq("account-id"), eq("new-alias"), isNull());
    }

    @Test
    void updateClientSecret_newAlias_withSecret() {

        when(accountServiceMock.updateSecret(anyString(), eq("new-alias"), eq("new-secret"))).thenReturn(ServiceResult.success("new-alias"));

        var request = new UpdateClientSecret("new-alias", "new-secret");
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/account-id/secret")
                .then()
                .statusCode(200)
                .body(equalTo("new-alias"));
        verify(accountServiceMock).updateSecret(eq("account-id"), eq("new-alias"), eq("new-secret"));
    }

    @Test
    void updateClientSecret_whenNotFound_expect404() {

        when(accountServiceMock.updateSecret(eq("not-exists-id"), eq("new-alias"), isNull())).thenReturn(ServiceResult.notFound("foo"));
        var request = new UpdateClientSecret("new-alias", null);
        baseRequest()
                .contentType("application/json")
                .body(request)
                .put("/not-exists-id/secret")
                .then()
                .statusCode(404)
                .body(containsString("Object of type StsAccount with ID=not-exists-id was not found"));
    }


    @Test
    void deleteAccount() {
        when(accountServiceMock.deleteById(eq("account-id"))).thenReturn(ServiceResult.success());
        baseRequest()
                .contentType("application/json")
                .delete("/account-id")
                .then()
                .statusCode(204);

        verify(accountServiceMock).deleteById(eq("account-id"));
    }

    @Test
    void deleteAccount_whenNotFound_expect404() {
        when(accountServiceMock.deleteById(eq("account-id"))).thenReturn(ServiceResult.notFound("foo"));

        baseRequest()
                .contentType("application/json")
                .delete("/account-id")
                .then()
                .statusCode(404);
        verify(accountServiceMock).deleteById(eq("account-id"));
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