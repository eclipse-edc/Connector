/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.model.CatalogRequestDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;

@OpenAPIDefinition
@Tag(name = "Catalog")
public interface CatalogApi {

    String PROVIDER_URL_NOT_NULL_MESSAGE = "providerUrl must not be null";

    @Operation(responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Catalog.class)), description = "Gets contract offers (=catalog) of a single connector")
    }, deprecated = true)
    void getCatalog(@NotNull(message = PROVIDER_URL_NOT_NULL_MESSAGE) String providerUrl, @Valid QuerySpecDto querySpec, AsyncResponse response);

    @Operation(responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Catalog.class)), description = "Gets contract offers (=catalog) of a single connector")
    })
    void requestCatalog(@Valid @NotNull CatalogRequestDto requestDto, AsyncResponse response);
}
