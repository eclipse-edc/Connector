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

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Asset")
public interface AssetApi {

    @Operation(description = "Creates a new asset together with a data address",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was created successfully"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed"),
                    @ApiResponse(responseCode = "409", description = "Could not create asset, because an asset with that ID already exists") }
    )
    void createAsset(@Valid AssetEntryDto assetEntryDto);

    @Operation(description = "Gets all assets according to a particular query",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed")
            })
    List<AssetDto> getAllAssets(@Valid QuerySpecDto querySpecDto);

    @Operation(description = "Gets an asset with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The asset", content = @Content(schema = @Schema(implementation = AssetDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist")
            })
    AssetDto getAsset(String id);

    @Operation(description = "Removes an asset with the given ID if possible. Deleting an asset is only possible if that asset is not yet referenced " +
            "by a contract agreement, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting assets can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An asset with the given ID does not exist"),
                    @ApiResponse(responseCode = "409", description = "The asset cannot be deleted, because it is referenced by a contract agreement")
            })
    void removeAsset(String id);

}
