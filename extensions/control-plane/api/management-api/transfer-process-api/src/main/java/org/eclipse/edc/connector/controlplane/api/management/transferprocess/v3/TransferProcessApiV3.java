/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3;

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
import org.eclipse.edc.api.management.schema.ManagementApiSchema;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.TransferState;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition(info = @Info(version = "v3"))
@Tag(name = "Transfer Process V3")
public interface TransferProcessApiV3 {

    String ASYNC_WARNING = "Due to the asynchronous nature of transfers, a successful response only indicates that the " +
            "request was successfully received. This may take a long time, so clients must poll the /{id}/state " +
            "endpoint to track the state.";

    @Operation(description = "Returns all transfer process according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer processes matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransferProcessSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))) }
    )
    JsonArray queryTransferProcessesV3(JsonObject querySpecJson);

    @Operation(description = "Gets an transfer process with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer process",
                            content = @Content(schema = @Schema(implementation = TransferProcessSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getTransferProcessV3(String id);

    @Operation(description = "Gets the state of a transfer process with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  transfer process's state",
                            content = @Content(schema = @Schema(implementation = TransferStateSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An  transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getTransferProcessStateV3(String id);

    @Operation(description = "Initiates a data transfer with the given parameters. " + ASYNC_WARNING,
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TransferRequestSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The transfer was successfully initiated. Returns the transfer process ID and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class)),
                            links = @Link(name = "poll-state", operationId = "getTransferProcessStateV3", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
            })
    JsonObject initiateTransferProcessV3(JsonObject transferRequest);

    @Operation(description = "Requests the deprovisioning of resources associated with a transfer process. " + ASYNC_WARNING,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to deprovision the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "deprovisionTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void deprovisionTransferProcessV3(String id);

    @Operation(description = "Requests the termination of a transfer process. " + ASYNC_WARNING,
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TerminateTransferSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to terminate the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "terminateTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not terminate transfer process, because it is already completed or terminated.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void terminateTransferProcessV3(String id, JsonObject terminateTransfer);

    @Operation(description = "Requests the suspension of a transfer process. " + ASYNC_WARNING,
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = SuspendTransferSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to suspend the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "suspendTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not suspend the transfer process, because it is already completed or terminated.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void suspendTransferProcessV3(String id, JsonObject suspendTransfer);

    @Operation(description = "Requests the resumption of a suspended transfer process. " + ASYNC_WARNING,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request to resume the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "resumeTransferProcessV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void resumeTransferProcessV3(String id);

    @Schema(name = "TransferRequest", example = TransferRequestSchema.TRANSFER_REQUEST_EXAMPLE)
    record TransferRequestSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, example = TRANSFER_REQUEST_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String protocol,
            @Schema(requiredMode = REQUIRED)
            String counterPartyAddress,
            @Schema(requiredMode = REQUIRED)
            String contractId,
            @Schema(deprecated = true)
            String assetId,
            @Schema(requiredMode = REQUIRED)
            String transferType,
            ApiCoreSchema.DataAddressSchema dataDestination,
            @Schema(additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
            ManagementApiSchema.FreeFormPropertiesSchema privateProperties,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses) {

        public static final String TRANSFER_REQUEST_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferRequest",
                    "protocol": "dataspace-protocol-http",
                    "counterPartyAddress": "http://provider-address",
                    "contractId": "contract-id",
                    "transferType": "transferType",
                    "dataDestination": {
                        "type": "data-destination-type"
                    },
                    "privateProperties": {
                        "private-key": "private-value"
                    },
                    "callbackAddresses": [{
                        "transactional": false,
                        "uri": "http://callback/url",
                        "events": ["contract.negotiation", "transfer.process"],
                        "authKey": "auth-key",
                        "authCodeId": "auth-code-id"
                    }]
                }
                """;
    }

    @Schema(name = "TransferProcess", example = TransferProcessSchema.TRANSFER_PROCESS_EXAMPLE)
    record TransferProcessSchema(
            @Schema(name = TYPE, example = TRANSFER_PROCESS_TYPE)
            String ldType,
            @Schema(name = ID)
            String id,
            TransferProcess.Type type,
            String protocol,
            String counterPartyId,
            String counterPartyAddress,
            String state,
            String contractAgreementId,
            String errorDetail,
            ApiCoreSchema.DataAddressSchema dataDestination,
            ManagementApiSchema.FreeFormPropertiesSchema privateProperties,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses
    ) {
        public static final String TRANSFER_PROCESS_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferProcess",
                    "@id": "process-id",
                    "correlationId": "correlation-id",
                    "type": "PROVIDER",
                    "state": "STARTED",
                    "stateTimestamp": 1688465655,
                    "assetId": "asset-id",
                    "contractId": "contractId",
                    "dataDestination": {
                        "type": "data-destination-type"
                    },
                    "privateProperties": {
                        "private-key": "private-value"
                    },
                    "errorDetail": "eventual-error-detail",
                    "createdAt": 1688465655,
                    "callbackAddresses": [{
                        "transactional": false,
                        "uri": "http://callback/url",
                        "events": ["contract.negotiation", "transfer.process"],
                        "authKey": "auth-key",
                        "authCodeId": "auth-code-id"
                    }]
                }
                """;
    }

    @Schema(name = "TransferState", example = TransferStateSchema.TRANSFER_STATE_EXAMPLE)
    record TransferStateSchema(
            @Schema(name = TYPE, example = TransferState.TRANSFER_STATE_TYPE)
            String ldType,
            String state
    ) {
        public static final String TRANSFER_STATE_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferState",
                    "state": "STARTED"
                }
                """;
    }

    @Schema(name = "TerminateTransfer", example = TerminateTransferSchema.TERMINATE_TRANSFER_EXAMPLE)
    record TerminateTransferSchema(
            @Schema(name = TYPE, example = TerminateTransfer.TERMINATE_TRANSFER_TYPE)
            String ldType,
            String state
    ) {
        public static final String TERMINATE_TRANSFER_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TerminateTransfer",
                    "reason": "a reason to terminate"
                }
                """;
    }

    @Schema(name = "SuspendTransfer", example = SuspendTransferSchema.SUSPEND_TRANSFER_EXAMPLE)
    record SuspendTransferSchema(
            @Schema(name = TYPE, example = SuspendTransfer.SUSPEND_TRANSFER_TYPE)
            String ldType,
            String state
    ) {
        public static final String SUSPEND_TRANSFER_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/SuspendTransfer",
                    "reason": "a reason to suspend"
                }
                """;
    }
}
