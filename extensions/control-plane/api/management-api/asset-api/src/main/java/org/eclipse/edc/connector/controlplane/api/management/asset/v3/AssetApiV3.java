/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.asset.v3;

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
import org.eclipse.edc.api.management.schema.ManagementApiSchema;
import org.eclipse.edc.api.model.ApiCoreSchema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition(
        info = @Info(description = "This contains both the current and the new Asset API, which accepts JSON-LD and will " +
                "become the standard API once the Dataspace Protocol is stable.", title = "Asset API", version = "v3"))
@Tag(name = "Asset V3")
public interface AssetApiV3 {
    @Operation(description = "Creates a new asset together with a data address",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = AssetInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was created successfully. Returns the asset Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create asset, because an asset with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))}
    )
    JsonObject createAssetV3(JsonObject asset);

    @Operation(description = "Request all assets according to a particular query",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The assets matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetOutputSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    JsonArray requestAssetsV3(JsonObject querySpecJson);

    @Operation(description = "Gets an asset with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The asset",
                            content = @Content(schema = @Schema(implementation = AssetOutputSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getAssetV3(String id);

    @Operation(description = "Removes an asset with the given ID if possible. Deleting an asset is only possible if that asset is not yet referenced " +
            "by a contract agreement, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting assets can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Asset was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "The asset cannot be deleted, because it is referenced by a contract agreement",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void removeAssetV3(String id);

    @Operation(description = "Updates an asset with the given ID if it exists. If the asset is not found, no further action is taken. " +
            "DANGER ZONE: Note that updating assets can have unexpected results, especially for contract offers that have been sent out or are ongoing in contract negotiations.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = AssetInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Asset was updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Asset could not be updated, because it does not exist."),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
            })
    void updateAssetV3(JsonObject asset);

    @Schema(name = "AssetInput", example = AssetInputSchema.ASSET_INPUT_EXAMPLE)
    record AssetInputSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = EDC_ASSET_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            ManagementApiSchema.FreeFormPropertiesSchema properties,
            ManagementApiSchema.FreeFormPropertiesSchema privateProperties,
            @Schema(requiredMode = REQUIRED)
            ApiCoreSchema.DataAddressSchema dataAddress
    ) {
        public static final String ASSET_INPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "asset-id",
                    "properties": {
                        "key": "value"
                    },
                    "privateProperties": {
                        "privateKey": "privateValue"
                    },
                    "dataAddress": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos"
                    }
                }
                """;
    }

    @ArraySchema()
    @Schema(name = "AssetOutput", example = AssetOutputSchema.ASSET_OUTPUT_EXAMPLE)
    record AssetOutputSchema(
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = EDC_ASSET_TYPE)
            String type,
            ManagementApiSchema.FreeFormPropertiesSchema properties,
            ManagementApiSchema.FreeFormPropertiesSchema privateProperties,
            ApiCoreSchema.DataAddressSchema dataAddress,
            long createdAt
    ) {
        public static final String ASSET_OUTPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "asset-id",
                    "properties": {
                        "key": "value"
                    },
                    "privateProperties": {
                        "privateKey": "privateValue"
                    },
                    "dataAddress": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos"
                    },
                    "createdAt": 1688465655
                }
                """;
    }

}
