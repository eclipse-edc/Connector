/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

@OpenAPIDefinition
@Tag(name = "Token Validation")
public interface DataPlaneTokenValidationApi {

    @Operation(description = "Checks that the provided token has been signed by the present entity and asserts its validity. " +
            "If token is valid, then the data address contained in its claims is decrypted and returned back to the caller.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token is valid"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed"),
                    @ApiResponse(responseCode = "403", description = "Token is invalid") }
    )
    DataAddress validate(@NotNull String token);
}
