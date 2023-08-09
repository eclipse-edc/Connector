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

package org.eclipse.edc.connector.api.management.transferprocess;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
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
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiSchema;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferState;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition
@Tag(name = "Transfer Process")
public interface TransferProcessApi {
    @Operation(description = "Returns all transfer process according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer processes matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransferProcessSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    JsonArray queryTransferProcesses(JsonObject querySpecJson);

    @Operation(description = "Gets an transfer process with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer process",
                            content = @Content(schema = @Schema(implementation = TransferProcessSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getTransferProcess(String id);

    @Operation(description = "Gets the state of a transfer process with the given ID",
            operationId = "getTransferProcessState",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The  transfer process's state",
                            content = @Content(schema = @Schema(implementation = TransferStateSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An  transfer process with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getTransferProcessState(String id);

    @Operation(description = "Initiates a data transfer with the given parameters. Please note that successfully invoking this endpoint " +
            "only means that the transfer was initiated. Clients must poll the /{id}/state endpoint to track the state",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TransferRequestSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer was successfully initiated. Returns the transfer process ID and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class)),
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    JsonObject initiateTransferProcess(JsonObject transferRequest);

    @Operation(description = "Requests the deprovisioning of resources associated with a transfer process. Due to the asynchronous nature of transfers, a successful " +
            "response only indicates that the request was successfully received. This may take a long time, so clients must poll the /{id}/state endpoint to track the state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request to deprovision the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void deprovisionTransferProcess(String id);

    @Operation(description = "Requests the termination of a transfer process. Due to the asynchronous nature of transfers, a successful " +
            "response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TerminateTransferSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request to cancel the transfer process was successfully received",
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not terminate transfer process, because it is already completed or terminated.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void terminateTransferProcess(String id, JsonObject terminateTransfer);

    @Schema(name = "TransferRequest", example = TransferRequestSchema.TRANSFER_REQUEST_EXAMPLE)
    record TransferRequestSchema(
            @Schema(name = TYPE, example = TRANSFER_REQUEST_TYPE)
            String type,
            String protocol,
            String connectorAddress,
            String connectorId,
            String contractId,
            String assetId,
            ManagementApiSchema.DataAddressSchema dataDestination,
            @Schema(deprecated = true, description = "Deprecated as this field is not used anymore, please use privateProperties instead")
            Map<String, String> properties,
            Map<String, String> privateProperties,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses) {

        public static final String TRANSFER_REQUEST_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferRequest",
                    "protocol": "dataspace-protocol-http",
                    "connectorAddress": "http://provider-address",
                    "connectorId": "provider-id",
                    "contractId": "contract-id",
                    "assetId": "asset-id",
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
            @Deprecated(since = "0.2.0")
            @Schema(deprecated = true)
            Map<String, String> properties,
            Map<String, Object> privateProperties,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses
    ) {
        public static final String TRANSFER_PROCESS_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferProcess",
                    "@id": "process-id",
                    "correlationId": "correlation-id",
                    "type": "PROVIDER",
                    "state": "STARTED",
                    "stateTimestamp": 1688465655,
                    "assetId": "asset-id",
                    "connectorId": "connectorId",
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
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferState",
                    "state": "STARTED"
                }
                """;
    }

    @Schema(name = "TerminateTransfer", example = TerminateTransferSchema.TERMINATE_TRANSFER_EXAMPLE)
    record TerminateTransferSchema(
            @Schema(name = TYPE, example = TransferState.TRANSFER_STATE_TYPE)
            String ldType,
            String state
    ) {
        public static final String TERMINATE_TRANSFER_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TerminateTransfer",
                    "reason": "a reason to terminate"
                }
                """;
    }
}
