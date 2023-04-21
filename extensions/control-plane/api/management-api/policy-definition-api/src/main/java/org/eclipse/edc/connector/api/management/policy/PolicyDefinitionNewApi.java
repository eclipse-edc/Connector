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

package org.eclipse.edc.connector.api.management.policy;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewRequestDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewResponseDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewUpdateDto;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Policy Definition")
public interface PolicyDefinitionNewApi {

    @Operation(description = "Returns all policy definitions according to a query",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PolicyDefinitionNewResponseDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    List<PolicyDefinitionNewResponseDto> queryPolicyDefinitions(@Valid QuerySpecDto querySpecDto);

    @Operation(description = "Gets a policy definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  policy definition",
                            content = @Content(schema = @Schema(implementation = PolicyDefinitionNewResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An  policy definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    PolicyDefinitionNewResponseDto getPolicyDefinition(String id);

    @Operation(description = "Creates a new policy definition",
            responses = {
                    @ApiResponse(responseCode = "200", description = "policy definition was created successfully. Returns the Policy Definition Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = IdResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create policy definition, because a contract definition with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    IdResponseDto createPolicyDefinition(@Valid PolicyDefinitionNewRequestDto policyDefinition);

    @Operation(description = "Removes a policy definition with the given ID if possible. Deleting a policy definition is only possible if that policy definition is not yet referenced " +
            "by a contract definition, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting policy definitions can have unexpected results, do this at your own risk!",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Policy definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An policy definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "The policy definition cannot be deleted, because it is referenced by a contract definition",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    void deletePolicyDefinition(String id);

    @Operation(description = "Updates an existing Policy, If the Policy is not found, an error is reported",
            responses = {
                    @ApiResponse(responseCode = "200", description = "policy definition was updated successfully. Returns the Policy Definition Id and updated timestamp"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "policy definition could not be updated, because it does not exists",
                            content = @Content(schema = @Schema(implementation = ApiErrorDetail.class)))
            }
    )
    void updatePolicyDefinition(String id, @Valid PolicyDefinitionNewUpdateDto policyDefinition);
}
