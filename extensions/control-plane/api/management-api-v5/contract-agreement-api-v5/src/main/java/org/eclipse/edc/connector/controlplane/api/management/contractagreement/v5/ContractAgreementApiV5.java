/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v5;

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
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(info = @Info(version = "v5alpha"))
@Tag(name = "Contract Agreement v5alpha")
public interface ContractAgreementApiV5 {

    @Operation(description = "Gets all contract agreements according to a particular query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreements matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_AGREEMENT)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonArray queryAgreementsV5(String participantContextId, JsonObject querySpecJson, SecurityContext securityContext);

    @Operation(description = "Gets an contract agreement with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreement",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_AGREEMENT))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getAgreementByIdV5(String participantContextId, String id, SecurityContext securityContext);


    @Operation(description = "Gets a contract negotiation with the given contract agreement ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiation",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_AGREEMENT))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getNegotiationByAgreementIdV5(String participantContextId, String id, SecurityContext securityContext);

}
