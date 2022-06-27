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

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Policy")
public interface PolicyDefinitionApi {

    @Operation(description = "Returns all policy definitions according to a query",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PolicyDefinition.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed") }
    )
    List<PolicyDefinition> getAllPolicies(@Valid QuerySpecDto querySpecDto);

    @Operation(description = "Gets a policy definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  policy definition", content = @Content(schema = @Schema(implementation = PolicyDefinition.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An  policy definition with the given ID does not exist")
            }
    )
    PolicyDefinition getPolicy(String id);

    @Operation(description = "Creates a new policy definition",
            responses = {
                    @ApiResponse(responseCode = "200", description = "policy definition was created successfully"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed"),
                    @ApiResponse(responseCode = "409", description = "Could not create policy definition, because a contract definition with that ID already exists") }
    )
    void createPolicy(PolicyDefinition policy);

    @Operation(description = "Removes a policy definition with the given ID if possible. Deleting a policy definition is only possible if that policy definition is not yet referenced " +
            "by a contract definition, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting policy definitions can have unexpected results, do this at your own risk!",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Policy definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An policy definition with the given ID does not exist"),
                    @ApiResponse(responseCode = "409", description = "The policy definition cannot be deleted, because it is referenced by a contract definition")
            }
    )
    void deletePolicy(String id);

}
