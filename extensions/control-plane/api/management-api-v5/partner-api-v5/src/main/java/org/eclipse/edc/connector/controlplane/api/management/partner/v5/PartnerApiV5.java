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
@Tag(name = "Partner v5")
public interface PartnerApiV5 {

    @Operation(description = "Returns all partners of the participant context matching a query. Partner properties can be " +
            "filtered with nested criteria, e.g. `properties.businessId = BID-0001`, group membership with `groupIds contains <groupId>`",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The partners matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonArray queryPartnersV5(String participantContextId, JsonObject querySpecJson, SecurityContext securityContext);

    @Operation(description = "Gets a partner with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The partner",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER))),
                    @ApiResponse(responseCode = "404", description = "A partner with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getPartnerV5(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Creates a new partner. Every referenced partner group must exist in the participant context.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The partner was created. Returns the partner id and created timestamp",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed or a referenced group does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "A partner with that id or identity already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonObject createPartnerV5(String participantContextId, JsonObject partner, SecurityContext securityContext);

    @Operation(description = "Removes a partner with the given ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The partner was deleted"),
                    @ApiResponse(responseCode = "404", description = "A partner with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    void deletePartnerV5(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Updates an existing partner. Every referenced partner group must exist in the participant context.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.PARTNER))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The partner was updated"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed or a referenced group does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "A partner with the given ID does not exist",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))),
                    @ApiResponse(responseCode = "409", description = "Another partner already has the requested identity",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    void updatePartnerV5(String participantContextId, String id, JsonObject partner, SecurityContext securityContext);

}
