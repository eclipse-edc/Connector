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

package org.eclipse.edc.connector.api.management.catalog;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;

@OpenAPIDefinition
@Tag(name = "Catalog (deprecated)")
public interface CatalogApi {

    String PROVIDER_URL_NOT_NULL_MESSAGE = "providerUrl must not be null";

    @Operation(responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Catalog.class)), description = "Gets contract offers (=catalog) of a single connector")
    }, deprecated = true)
    @Deprecated
    void getCatalog(@NotNull(message = PROVIDER_URL_NOT_NULL_MESSAGE) String providerUrl, @Valid QuerySpecDto querySpec, AsyncResponse response);

    @Operation(responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Catalog.class)), description = "Gets contract offers (=catalog) of a single connector")
    })
    void requestCatalog(@Valid @NotNull CatalogRequestDto requestDto, @Suspended AsyncResponse response);
}
