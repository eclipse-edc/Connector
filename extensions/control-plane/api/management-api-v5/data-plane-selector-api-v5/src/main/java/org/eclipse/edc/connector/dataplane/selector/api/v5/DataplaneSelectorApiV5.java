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

package org.eclipse.edc.connector.dataplane.selector.api.v5;

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
import org.eclipse.edc.api.model.ApiCoreSchema;

@OpenAPIDefinition(info = @Info(description = "Management API to query the data plane instances registered for a participant context.",
        title = "Dataplane Selector API", version = "v5"))
@Tag(name = "Dataplane Selector v5")
public interface DataplaneSelectorApiV5 {

    @Operation(description = "Returns all data plane instances registered for the participant context according to a particular query",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A (potentially empty) list of data plane instances matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.DATAPLANE_INSTANCE)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    JsonArray queryDataPlaneInstancesV5(String participantContextId, JsonObject querySpecJson, SecurityContext securityContext);

}
