/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;

import static jakarta.ws.rs.HttpMethod.POST;

@OpenAPIDefinition
@Tag(name = "Data Plane Transfer events")
public interface DataPlaneTransferApi {

    @Operation(
            method = POST,
            description = "Notify a prepared transfer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Prepared notification delivered correctly"),
                    @ApiResponse(responseCode = "404", description = "Transfer process does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    Response prepared(String transferId, DataFlowResponseMessage message);

    @Operation(
            method = POST,
            description = "Notify a completed transfer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Completed notification delivered correctly"),
                    @ApiResponse(responseCode = "404", description = "Transfer process does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    Response completed(String transferId);


}
