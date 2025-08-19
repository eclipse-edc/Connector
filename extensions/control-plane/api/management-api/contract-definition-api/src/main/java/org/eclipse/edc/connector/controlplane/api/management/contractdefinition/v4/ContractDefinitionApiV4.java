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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v4;

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
import org.eclipse.edc.api.model.ApiCoreSchema;

@OpenAPIDefinition(info = @Info(version = "v4alpha"))
@Tag(name = "Contract Definition v4alpha")
public interface ContractDefinitionApiV4 {

    @Operation(description = "Returns all contract definitions according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract definitions matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_DEFINITION)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonArray queryContractDefinitionsV4(JsonObject querySpecJson);

    @Operation(description = "Gets an contract definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract definition",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_DEFINITION))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getContractDefinitionV4(String id);

    @Operation(description = "Creates a new contract definition",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_DEFINITION))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "contract definition was created successfully. Returns the Contract Definition Id and created timestamp",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create contract definition, because a contract definition with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))}
    )
    JsonObject createContractDefinitionV4(JsonObject createObject);

    @Operation(description = "Removes a contract definition with the given ID if possible. " +
            "DANGER ZONE: Note that deleting contract definitions can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contract definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void deleteContractDefinitionV4(String id);

    @Operation(description = "Updated a contract definition with the given ID. The supplied JSON structure must be a valid JSON-LD object",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CONTRACT_DEFINITION))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contract definition was updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void updateContractDefinitionV4(JsonObject updateObject);

}
