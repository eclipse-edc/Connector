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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferId;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferState;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Transfer Process")
public interface TransferProcessApi {
    @Operation(description = "Returns all transfer process according to a query",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransferProcessDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed") }
    )
    List<TransferProcessDto> getAllTransferProcesses(@Valid QuerySpecDto querySpecDto);

    @Operation(description = "Gets an transfer process with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer process", content = @Content(schema = @Schema(implementation = TransferProcessDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist")
            }
    )
    TransferProcessDto getTransferProcess(String id);

    @Operation(description = "Gets the state of a transfer process with the given ID",
            operationId = "getTransferProcessState",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  transfer process's state", content = @Content(schema = @Schema(implementation = TransferState.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "An  transfer process with the given ID does not exist")
            }
    )
    TransferState getTransferProcessState(String id);

    @Operation(description = "Requests aborting the transfer process. Due to the asynchronous nature of transfers, a successful " +
            "response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request to cancel the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist")
            })
    void cancelTransferProcess(String id);

    @Operation(description = "Requests the deprovisioning of resources associated with a transfer process. Due to the asynchronous nature of transfers, a successful " +
            "response only indicates that the request was successfully received. This may take a long time, so clients must poll the /{id}/state endpoint to track the state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request to deprovision the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null"),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist")
            })
    void deprovisionTransferProcess(String id);

    @Operation(description = "Initiates a data transfer with the given parameters. Please note that successfully invoking this endpoint " +
            "only means that the transfer was initiated. Clients must poll the /{id}/state endpoint to track the state",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer was successfully initiated. Returns the transfer process ID", content = @Content(schema = @Schema(implementation = TransferId.class)),
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed"),
            })
    TransferId initiateTransfer(@Valid TransferRequestDto transferRequest);
}
