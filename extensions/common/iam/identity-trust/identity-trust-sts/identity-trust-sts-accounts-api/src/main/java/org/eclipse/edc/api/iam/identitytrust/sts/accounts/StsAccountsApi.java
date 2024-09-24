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

package org.eclipse.edc.api.iam.identitytrust.sts.accounts;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.StsAccountCreation;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.model.UpdateClientSecret;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition
@Tag(name = "Secure Token Service Api")
public interface StsAccountsApi {

    @Operation(description = "Creates a new STS Account with the given parameters",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = StsAccountCreation.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The newly created STS Account including the client_secret.",
                            content = @Content(schema = @Schema(implementation = StsAccountCreation.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated: principal could not be identified",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    StsAccountCreation createAccount(StsAccountCreation request);

    @Operation(description = "Updates an existing STS account with new values. To update the client secret, please use the relevant API endpoint.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = StsAccount.class))),
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated: principal could not be identified",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An account with the given ID was not found")
            })
    void updateAccount(StsAccount updatedAccount);

    @Operation(description = "Gets the STS Account for the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The STS Account.",
                            content = @Content(schema = @Schema(implementation = StsAccount.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated: principal could not be identified",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An account with the given ID was not found")
            })
    StsAccount getAccount(String accountId);

    @Operation(description = "Queries for STS Account conforming to the given query object",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The STS Accounts.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StsAccount.class)))),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated: principal could not be identified",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    Collection<StsAccount> queryAccounts(QuerySpec querySpec);

    @Operation(description = "Updates the client secret for an account. If the secret is null, then one will be generated.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The secret alias that is now used by the account."),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated: principal could not be identified",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An account with the given ID was not found")
            })
    String updateClientSecret(String accountId, UpdateClientSecret request);


    @Operation(description = "Deletes an STS Account",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "400", description = "Invalid Request",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated: principal could not be identified",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An account with the given ID was not found")
            })
    void deleteAccount(String accountId);
}
