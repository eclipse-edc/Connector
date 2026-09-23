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

package org.eclipse.edc.connector.controlplane.api.management.partner.v5;

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

@OpenAPIDefinition(info = @Info(version = "v5"))
@Tag(name = "Partner Group v5")
public interface PartnerGroupApiV5 {

    @Operation(description = "Returns all partner groups of the participant context matching a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The partner groups matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER_GROUP)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonArray queryPartnerGroupsV5(String participantContextId, JsonObject querySpecJson, SecurityContext securityContext);

    @Operation(description = "Gets a partner group with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The partner group",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER_GROUP))),
                    @ApiResponse(responseCode = "404", description = "A partner group with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getPartnerGroupV5(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Creates a new partner group",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER_GROUP))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The partner group was created. Returns the group id and created timestamp",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "A partner group with that id already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonObject createPartnerGroupV5(String participantContextId, JsonObject partnerGroup, SecurityContext securityContext);

    @Operation(description = "Removes a partner group with the given ID. A group that is still referenced by a partner cannot be deleted.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The partner group was deleted"),
                    @ApiResponse(responseCode = "404", description = "A partner group with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "The partner group is still referenced by at least one partner",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    void deletePartnerGroupV5(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Updates an existing partner group",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER_GROUP))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The partner group was updated"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "A partner group with the given ID does not exist",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))
            }
    )
    void updatePartnerGroupV5(String participantContextId, String id, JsonObject partnerGroup, SecurityContext securityContext);

}
