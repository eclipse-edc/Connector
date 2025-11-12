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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v4;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(info = @Info(version = "v4beta"))
@Tag(name = "Catalog v4beta")
public interface CatalogApiV4 {

    @Operation(
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CATALOG_REQUEST))),
            responses = {@ApiResponse(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "https://w3id.org/dspace/2025/1/catalog/catalog-schema.json")
                    ),
                    description = "Gets contract offers (=catalog) of a single connector")}
    )
    void requestCatalogV4(JsonObject request, @Suspended AsyncResponse response);

    @Operation(
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.CATALOG_REQUEST))),
            responses = {@ApiResponse(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "https://w3id.org/dspace/2025/1/catalog/dataset-schema.json")
                    ),
                    description = "Gets single dataset from a connector")}
    )
    void getDatasetV4(JsonObject request, @Suspended AsyncResponse response);

}
