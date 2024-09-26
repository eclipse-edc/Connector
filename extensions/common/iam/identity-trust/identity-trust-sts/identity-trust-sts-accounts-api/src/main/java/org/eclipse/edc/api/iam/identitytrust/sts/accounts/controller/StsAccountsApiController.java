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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.StsAccountsApi;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.StsAccountCreation;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.UpdateClientSecret;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Collection;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/v1alpha/accounts")
public class StsAccountsApiController implements StsAccountsApi {

    private final StsAccountService accountService;

    public StsAccountsApiController(StsAccountService accountService) {
        this.accountService = accountService;
    }

    @Path("/")
    @POST
    @Override
    public StsAccountCreation createAccount(StsAccountCreation request) {
        var secret = accountService.create(request.account(), request.clientSecret()).orElseThrow(exceptionMapper(StsAccount.class));
        return new StsAccountCreation(request.account(), secret);
    }

    @Path("/")
    @PUT
    @Override
    public void updateAccount(StsAccount updatedAccount) {
        accountService.update(updatedAccount).orElseThrow(exceptionMapper(StsAccount.class, updatedAccount.getId()));
    }

    @GET
    @Path("/{id}")
    @Override
    public StsAccount getAccount(@PathParam("id") String accountId) {
        return accountService.findById(accountId).orElseThrow(exceptionMapper(StsAccount.class, accountId));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<StsAccount> queryAccounts(QuerySpec querySpec) {
        return accountService.queryAccounts(querySpec);
    }

    @PUT
    @Path("/{id}/secret")
    @Override
    public String updateClientSecret(@PathParam("id") String accountId, UpdateClientSecret request) {
        return accountService.updateSecret(accountId, request.newAlias(), request.newSecret()).orElseThrow(exceptionMapper(StsAccount.class, accountId));
    }

    @DELETE
    @Path("/{id}")
    @Override
    public void deleteAccount(@PathParam("id") String accountId) {
        accountService.deleteById(accountId).orElseThrow(exceptionMapper(StsAccount.class, accountId));
    }
}
