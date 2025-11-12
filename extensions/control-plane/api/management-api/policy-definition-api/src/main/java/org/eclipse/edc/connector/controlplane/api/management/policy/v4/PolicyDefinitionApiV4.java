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

package org.eclipse.edc.connector.controlplane.api.management.policy.v4;

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

@OpenAPIDefinition(info = @Info(version = "v4beta"))
@Tag(name = "Policy Definition v4beta")
public interface PolicyDefinitionApiV4 {

    @Operation(description = "Returns all policy definitions according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The policy definitions matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_DEFINITION)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonArray queryPolicyDefinitionsV4(JsonObject querySpecJson);

    @Operation(description = "Gets a policy definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  policy definition",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_DEFINITION))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "An  policy definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getPolicyDefinitionV4(String id);

    @Operation(description = "Creates a new policy definition",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_DEFINITION))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "policy definition was created successfully. Returns the Policy Definition Id and created timestamp",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "Could not create policy definition, because a contract definition with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonObject createPolicyDefinitionV4(JsonObject policyDefinition);

    @Operation(description = "Removes a policy definition with the given ID if possible. Deleting a policy definition is " +
            "only possible if that policy definition is not yet referenced by a contract definition, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting policy definitions can have unexpected results, do this at your own risk!",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Policy definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "An policy definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "The policy definition cannot be deleted, because it is referenced by a contract definition",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    void deletePolicyDefinitionV4(String id);

    @Operation(description = "Updates an existing Policy, If the Policy is not found, an error is reported",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_DEFINITION))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "policy definition was updated successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "policy definition could not be updated, because it does not exists",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))
            }
    )
    void updatePolicyDefinitionV4(String id, JsonObject policyDefinition);

    @Operation(description = "Validates an existing Policy, If the Policy is not found, an error is reported",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Returns the validation result", content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_VALIDATION_RESULT))),
                    @ApiResponse(responseCode = "404", description = "policy definition could not be validated, because it does not exists",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))
            }
    )
    JsonObject validatePolicyDefinitionV4(String id);


    @Operation(description = "Creates an execution plane for an existing Policy, If the Policy is not found, an error is reported",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_EVALUATION_REQUEST))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Returns the evaluation plan", content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.POLICY_EVALUATION_PLAN))),
                    @ApiResponse(responseCode = "404", description = "An evaluation plan could not be created, because the policy definition does not exists",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))
            }
    )
    JsonObject createExecutionPlanV4(String id, JsonObject input);


}
