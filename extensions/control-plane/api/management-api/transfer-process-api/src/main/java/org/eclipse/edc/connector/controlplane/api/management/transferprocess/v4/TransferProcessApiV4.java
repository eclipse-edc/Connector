/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v4;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(info = @Info(version = "v4beta"))
@Tag(name = "Transfer Process v4beta")
public interface TransferProcessApiV4 {

    String ASYNC_WARNING = "Due to the asynchronous nature of transfers, a successful response only indicates that the " +
            "request was successfully received. This may take a long time, so clients must poll the /{id}/state " +
            "endpoint to track the state.";

    @Operation(description = "Returns all transfer process according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer processes matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.TRANSFER_PROCESS)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))}
    )
    JsonArray queryTransferProcessesV4(JsonObject querySpecJson);

    @Operation(description = "Gets an transfer process with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer process",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.TRANSFER_PROCESS))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getTransferProcessV4(String id);

    @Operation(description = "Gets the state of a transfer process with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  transfer process's state",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.TRANSFER_PROCESS_STATE))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "An  transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            }
    )
    JsonObject getTransferProcessStateV4(String id);

    @Operation(description = "Initiates a data transfer with the given parameters. " + ASYNC_WARNING,
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.TRANSFER_REQUEST))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer was successfully initiated. Returns the transfer process ID and created timestamp",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE)),
                            links = @Link(name = "poll-state", operationId = "getTransferProcessStateV3", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
            })
    JsonObject initiateTransferProcessV4(JsonObject transferRequest);

    @Operation(description = "Requests the termination of a transfer process. " + ASYNC_WARNING,
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.TERMINATE_TRANSFER))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to terminate the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "terminateTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "Could not terminate transfer process, because it is already completed or terminated.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    void terminateTransferProcessV4(String id, JsonObject terminateTransfer);

    @Operation(description = "Requests the suspension of a transfer process. " + ASYNC_WARNING,
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.SUSPEND_TRANSFER))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to suspend the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "suspendTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "409", description = "Could not suspend the transfer process, because it is already completed or terminated.",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    void suspendTransferProcessV4(String id, JsonObject suspendTransfer);

    @Operation(description = "Requests the resumption of a suspended transfer process. " + ASYNC_WARNING,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to resume the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "resumeTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    void resumeTransferProcessV4(String id);
}
