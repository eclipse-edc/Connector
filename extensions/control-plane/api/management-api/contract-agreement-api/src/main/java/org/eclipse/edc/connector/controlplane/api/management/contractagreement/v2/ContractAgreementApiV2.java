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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v2;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiSchema;
import org.eclipse.edc.api.model.ApiCoreSchema;

@OpenAPIDefinition(info = @Info(version = "v2"))
@Tag(name = "Contract Agreement V2")
public interface ContractAgreementApiV2 {

    @Operation(description = "Gets all contract agreements according to a particular query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            operationId = "queryAgreementsV2",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreements matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ManagementApiSchema.ContractAgreementSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            },
            deprecated = true
    )
    @Deprecated(since = "0.7.0")
    JsonArray queryAgreements(JsonObject querySpecJson);

    @Operation(description = "Gets an contract agreement with the given ID",
            operationId = "getAgreementByIdV2",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreement",
                            content = @Content(schema = @Schema(implementation = ManagementApiSchema.ContractAgreementSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            },
            deprecated = true
    )
    @Deprecated(since = "0.7.0")
    JsonObject getAgreementById(String id);


    @Operation(description = "Gets a contract negotiation with the given contract agreement ID",
            operationId = "getNegotiationByAgreementIdV2",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiation",
                            content = @Content(schema = @Schema(implementation = ManagementApiSchema.ContractNegotiationSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            },
            deprecated = true
    )
    @Deprecated(since = "0.7.0")
    JsonObject getNegotiationByAgreementId(String id);

}
