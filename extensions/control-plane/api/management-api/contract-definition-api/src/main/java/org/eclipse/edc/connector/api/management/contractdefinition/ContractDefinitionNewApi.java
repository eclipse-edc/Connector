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

package org.eclipse.edc.connector.api.management.contractdefinition;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Definition")
public interface ContractDefinitionNewApi {

    @Operation(description = "Returns all contract definitions according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpecDto.class))),
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContractDefinitionResponseDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    List<JsonObject> queryAllContractDefinitions(JsonObject querySpecDto);

    @Operation(description = "Gets an contract definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract definition",
                            content = @Content(schema = @Schema(implementation = ContractDefinitionResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getContractDefinition(String id);

    @Operation(description = "Creates a new contract definition",
            responses = {
                    @ApiResponse(responseCode = "200", description = "contract definition was created successfully. Returns the Contract Definition Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = IdResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create contract definition, because a contract definition with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    JsonObject createContractDefinition(@Schema(implementation = ContractDefinitionRequestDto.class) JsonObject createObject);

    @Operation(description = "Removes a contract definition with the given ID if possible. " +
            "DANGER ZONE: Note that deleting contract definitions can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contract definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    void deleteContractDefinition(String id);

    @Operation(description = "Updated a contract definition with the given ID. The supplied JSON structure must be a valid JSON-LD object",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contract definition was updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    void updateContractDefinition(@Schema(implementation = ContractDefinitionRequestDto.class) JsonObject updateObject);

}
