/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.catalog.api.query.v5;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@OpenAPIDefinition(
        info = @Info(description = "This represents the Catalogs API. It serves the cached catalogs fetched from data providers, scoped to a single participant context.",
                title = "Catalogs API", version = "v5"))
@Tag(name = "Catalogs v5")
public interface CatalogsApiV5 {
    @Operation(description = "Obtains all catalogs currently held by this cache instance for the given participant context",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            parameters = @Parameter(name = "flatten", description = "Whether the resulting root catalog should be 'flattened' or contain a hierarchy of catalogs"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of catalogs is returned, potentially empty",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = "https://w3id.org/dspace/2025/1/catalog/catalog-schema.json")))),
                    @ApiResponse(responseCode = "500", description = "A query could not be completed due to an internal error")
            }

    )
    JsonArray requestCatalogsV5(String participantContextId, JsonObject querySpec, boolean flatten, SecurityContext securityContext);
}
