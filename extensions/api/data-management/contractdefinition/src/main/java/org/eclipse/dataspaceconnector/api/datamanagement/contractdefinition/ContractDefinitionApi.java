/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Definition")
public interface ContractDefinitionApi {

    @Operation(description = "Returns all contract definitions according to a query",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContractDefinitionDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed") }
    )
    List<ContractDefinitionDto> getAllContractDefinitions(@Valid QuerySpecDto querySpecDto);

    @Operation(description = "Gets an contract definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract definition", content = @Content(schema = @Schema(implementation = ContractDefinitionDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist")
            }
    )
    ContractDefinitionDto getContractDefinition(String id);

    @Operation(description = "Creates a new contract definition",
            responses = {
                    @ApiResponse(responseCode = "200", description = "contract definition was created successfully"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed"),
                    @ApiResponse(responseCode = "409", description = "Could not create contract definition, because a contract definition with that ID already exists") }
    )
    void createContractDefinition(@Valid ContractDefinitionDto dto);

    @Operation(description = "Removes a contract definition with the given ID if possible. " +
            "DANGER ZONE: Note that deleting contract definitions can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contract definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist")
            })
    void deleteContractDefinition(String id);
}
