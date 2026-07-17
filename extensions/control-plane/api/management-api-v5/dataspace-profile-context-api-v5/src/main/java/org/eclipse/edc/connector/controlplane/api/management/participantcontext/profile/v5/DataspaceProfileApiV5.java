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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile.v5;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(info = @Info(title = "Dataspace Profile Management API", version = "v5beta"))
@Tag(name = "Dataspace Profile v5beta")
public interface DataspaceProfileApiV5 {

    @Operation(description = "Creates a new dataspace profile. On success the profile is persisted and registered into " +
            "the running connector's dataspace profile context registry.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The dataspace profile was created successfully",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.DATASPACE_PROFILE_CONTEXT))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "A dataspace profile with the given name already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    JsonObject createProfileV5(JsonObject request);

    @Operation(description = "Queries the dataspace profiles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The dataspace profiles matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V5.DATASPACE_PROFILE_CONTEXT)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    JsonArray queryProfilesV5(JsonObject querySpecJson);

    @Operation(description = "Gets a dataspace profile by its name.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The dataspace profile",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.DATASPACE_PROFILE_CONTEXT))),
                    @ApiResponse(responseCode = "404", description = "A dataspace profile with the given name does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    JsonObject getProfileV5(String name);

    @Operation(description = "Deletes a dataspace profile by its name. The profile is removed from the store; the change " +
            "takes effect on the running registry after the next boot.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The dataspace profile was deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "A dataspace profile with the given name does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            }
    )
    void deleteProfileV5(String name);
}
