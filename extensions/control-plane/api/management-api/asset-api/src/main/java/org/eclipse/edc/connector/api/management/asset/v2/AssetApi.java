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

package org.eclipse.edc.connector.api.management.asset.v2;

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
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestDto;
import org.eclipse.edc.web.spi.ApiErrorDetail;

@OpenAPIDefinition(info = @Info(description = "This contains both the current and the new Asset API, which accepts JSON-LD and will become the standard API once the Dataspace Protocol is stable. " +
        "The new Asset API is prefixed with /v2, and the old endpoints have been deprecated. At that time of switching, the old API will be removed, and this API will be available without the /v2 prefix.", title = "Asset API"))
@Tag(name = "Asset")
@Deprecated(since = "0.1.3")
public interface AssetApi {

    @Operation(description = "Creates a new asset together with a data address",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = AssetEntryNewDto.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was created successfully. Returns the asset Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = IdResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create asset, because an asset with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    JsonObject createAsset(JsonObject assetEntryDto);

    @Operation(description = " all assets according to a particular query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpecDto.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The assets matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetResponseDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonArray requestAssets(JsonObject querySpecDto);

    @Operation(description = "Gets an asset with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The asset",
                            content = @Content(schema = @Schema(implementation = AssetResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getAsset(String id);

    @Operation(description = "Removes an asset with the given ID if possible. Deleting an asset is only possible if that asset is not yet referenced " +
            "by a contract agreement, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting assets can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "The asset cannot be deleted, because it is referenced by a contract agreement",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void removeAsset(String id);

    @Operation(description = "Updates an asset with the given ID if it exists. If the asset is not found, no further action is taken. " +
            "DANGER ZONE: Note that updating assets can have unexpected results, especially for contract offers that have been sent out or are ongoing in contract negotiations.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = AssetUpdateRequestDto.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Asset could not be updated, because it does not exist."),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    void updateAsset(JsonObject asset);

    @Operation(description = "Updates a DataAddress for an asset with the given ID.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = AssetUpdateRequestDto.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was updated successfully"),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    void updateDataAddress(String assetId, JsonObject dataAddress);


    @Operation(description = "Gets a data address of an asset with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The data address",
                            content = @Content(schema = @Schema(implementation = DataAddressDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonObject getAssetDataAddress(String id);
}
