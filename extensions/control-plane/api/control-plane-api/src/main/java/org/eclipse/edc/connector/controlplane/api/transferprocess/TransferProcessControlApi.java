/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.transferprocess;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.connector.controlplane.api.transferprocess.model.TransferProcessFailStateDto;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.ApiErrorDetail;


@OpenAPIDefinition
@Tag(name = "Transfer Process Control Api")
public interface TransferProcessControlApi {

    @Operation(description = "Requests completion of the transfer process. Due to the asynchronous nature of transfers, a successful response " +
            "only indicates that the request was successfully received",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void complete(String processId);

    @Operation(description = "Requests completion of the transfer process. Due to the asynchronous nature of transfers, a successful response " +
            "only indicates that the request was successfully received",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void fail(String processId, TransferProcessFailStateDto request);

    @Operation(description = "Notify provisioning completion.",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void provisioned(String processId, DataAddress newDataAddress);


}
