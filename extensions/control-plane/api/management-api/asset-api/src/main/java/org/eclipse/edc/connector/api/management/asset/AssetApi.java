/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.api.management.asset;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestDto;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Asset")
@Deprecated(since = "milestone9")
public interface AssetApi {

    @Operation(description = "Creates a new asset together with a data address",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was created successfully. Returns the asset Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = IdResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create asset, because an asset with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) },
            deprecated = true
    )
    @Deprecated(since = "milestone9")
    IdResponseDto createAsset(@Valid AssetEntryDto assetEntryDto);

    @Operation(description = "Gets all assets according to a particular query",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetResponseDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }, deprecated = true
    )
    @Deprecated(since = "milestone9")
    List<AssetResponseDto> getAllAssets(@Valid QuerySpecDto querySpecDto);

    @Operation(description = " all assets according to a particular query",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetResponseDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }, deprecated = true
    )
    @Deprecated(since = "milestone9")
    List<AssetResponseDto> requestAssets(@Valid QuerySpecDto querySpecDto);

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
    AssetResponseDto getAsset(String id);

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
            }, deprecated = true
    )
    @Deprecated(since = "milestone9")
    void removeAsset(String id);

    @Operation(description = "Updates an asset with the given ID if it exists. If the asset is not found, no further action is taken. " +
            "DANGER ZONE: Note that updating assets can have unexpected results, especially for contract offers that have been sent out or are ongoing in contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Asset could not be updated, because it does not exist."),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            }, deprecated = true
    )
    @Deprecated(since = "milestone9")
    void updateAsset(String assetId, @Valid AssetUpdateRequestDto asset);

    @Operation(description = "Updates a DataAddress for an asset with the given ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was updated successfully"),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            }, deprecated = true
    )
    @Deprecated(since = "milestone9")
    void updateDataAddress(String assetId, @Valid DataAddressDto dataAddress);


    @Operation(description = "Gets a data address of an asset with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The data address",
                            content = @Content(schema = @Schema(implementation = DataAddressDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }, deprecated = true
    )
    @Deprecated(since = "milestone9")
    DataAddressDto getAssetDataAddress(String id);
}

