/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.signaling.domain.DataPlaneRegistrationMessage;

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;

@OpenAPIDefinition
@Tag(name = "Dataplane Signaling Registration")
public interface DataPlaneRegistrationApi {

    @Operation(
            method = POST,
            description = "Register a new Dataplane instance",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataPlaneRegistrationMessage.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataplane instance correctly registered"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    Response register(DataPlaneRegistrationMessage registration);

    @Operation(
            method = PUT,
            description = "Update a Dataplane instance",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataPlaneRegistrationMessage.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataplane instance correctly updated"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    Response update(String dataplaneId, DataPlaneRegistrationMessage registration);


    @Operation(
            method = DELETE,
            description = "Update a Dataplane instance",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataplane instance correctly deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    Response delete(String dataplaneId);

}
