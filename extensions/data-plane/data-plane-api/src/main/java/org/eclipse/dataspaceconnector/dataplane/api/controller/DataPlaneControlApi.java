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

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

@OpenAPIDefinition
@Tag(name = "Data Plane control API",
        description = "Api targeted by the Control Plane to delegate a data transfer (Provider Push or Streaming) " +
                "to the Data Plane after the contract has been successfully negotiated and agreed between the two participants. ")
public interface DataPlaneControlApi {

    @Operation(description = "Initiates a data transfer for the given request. The transfer will be performed asynchronously.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Failed to validate request"),
                    @ApiResponse(responseCode = "200", description = "Data transfer initiated"),
            }
    )
    void initiateTransfer(DataFlowRequest request, AsyncResponse response);

    @Operation(description = "Get the current state of a data transfer.",
            responses = @ApiResponse(responseCode = "200", description = "Missing access token")
    )
    DataPlaneStore.State getTransferState(String processId);
}
