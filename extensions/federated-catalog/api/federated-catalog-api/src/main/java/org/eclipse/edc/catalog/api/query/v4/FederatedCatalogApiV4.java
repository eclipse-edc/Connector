/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.api.query.v4;

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
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(
        info = @Info(description = "This represents the Federated Catalog API. It serves the cached Catalogs fetched from the data providers.",
                title = "Federated Catalog API", version = "v4beta"))
@Tag(name = "Federated Catalog v4beta")
public interface FederatedCatalogApiV4 {
    @Operation(description = "Obtains all Catalog currently held by this cache instance",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            parameters = @Parameter(name = "flatten", description = "Whether the resulting root catalog should be 'flattened' or contain a hierarchy of catalogs"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of Catalog is returned, potentially empty",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = "https://w3id.org/dspace/2025/1/catalog/catalog-schema.json")))),
                    @ApiResponse(responseCode = "500", description = "A Query could not be completed due to an internal error")
            }

    )
    JsonArray getCachedCatalogV4(JsonObject querySpec, boolean flatten);
}
