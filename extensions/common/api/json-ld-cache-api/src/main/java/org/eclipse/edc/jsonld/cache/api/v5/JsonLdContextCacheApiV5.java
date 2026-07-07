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

package org.eclipse.edc.jsonld.cache.api.v5;

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

@OpenAPIDefinition(info = @Info(version = "v5beta", title = "JSON-LD Context Cache API",
        description = "Endpoints to dynamically manage the JSON-LD context documents cached by the connector."))
@Tag(name = "JSON-LD Context Cache v5beta")
public interface JsonLdContextCacheApiV5 {

    @Operation(description = "Returns all cached JSON-LD contexts.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The cached contexts",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V5.CACHED_JSON_LD_CONTEXT)))),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            })
    JsonArray getAll();

    @Operation(description = "Returns a cached JSON-LD context by id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The cached context",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.CACHED_JSON_LD_CONTEXT))),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The cached context does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            })
    JsonObject get(String id);

    @Operation(description = "Adds a JSON-LD context to the cache. Depending on the 'pullStrategy' " +
            "(NEVER, IF_NOT_PRESENT, ALWAYS) the document is either supplied inline via 'content' or fetched from 'url'.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.CACHED_JSON_LD_CONTEXT), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The created context",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            })
    JsonObject create(JsonObject request);

    @Operation(description = "Updates a cached JSON-LD context.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.CACHED_JSON_LD_CONTEXT), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The updated context",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.CACHED_JSON_LD_CONTEXT))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The cached context does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            })
    void update(String id, JsonObject request);

    @Operation(description = "Removes a JSON-LD context from the cache.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The context was removed"),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The cached context does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            })
    void delete(String id);

    @Operation(description = "Forces a refresh of a cached JSON-LD context by re-fetching it from its url (no-op for pull strategy 'NEVER').",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The refreshed context",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V5.CACHED_JSON_LD_CONTEXT))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The cached context does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)), mediaType = "application/json"))
            })
    JsonObject refresh(String id);
}
