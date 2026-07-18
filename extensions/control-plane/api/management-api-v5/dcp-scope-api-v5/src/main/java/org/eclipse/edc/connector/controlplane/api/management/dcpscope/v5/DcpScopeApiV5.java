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

package org.eclipse.edc.connector.controlplane.api.management.dcpscope.v5;

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
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(info = @Info(title = "DcpScope Management API", version = "v5beta"))
@Tag(name = "DcpScope v5beta")
public interface DcpScopeApiV5 {

    @Operation(description = "Creates a new DcpScope object.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.DCP_SCOPE), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DcpScope was created successfully, its id is returned in the response body.",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't create the DcpScope, because an object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    JsonObject createDcpScopeV5(JsonObject request);

    @Operation(description = "Updates an existing DcpScope object.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.DCP_SCOPE), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The DcpScope was updated successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A DcpScope with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    void updateDcpScopeV5(String id, JsonObject request);

    @Operation(description = "Deletes a DcpScope.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The DcpScope was deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A DcpScope with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    void deleteDcpScopeV5(String id);

    @Operation(description = "Returns all DcpScope objects according to a query.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of DcpScopes.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V5.DCP_SCOPE)))),
                    @ApiResponse(responseCode = "400", description = "The query was malformed or was not understood by the server.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    JsonArray queryDcpScopesV5(JsonObject querySpecJson);
}
