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

package org.eclipse.edc.connector.dataplane.api.controller.v1;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.api.model.DataFlowState;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_SIMPLE_TYPE;

@OpenAPIDefinition
@Tag(name = "Data Plane Signaling api API",
        description = "Api targeted by the Control Plane to delegate a data transfer " +
                "to the Data Plane after the contract has been successfully negotiated and agreed between the two participants. ")
public interface DataPlaneSignalingApi {

    @Operation(description = "Initiates a data transfer for the given start message. If the data transfer is handled by the data plane, it will be performed asynchronously. " +
            "If it's a consumer-pull scenario, a data address will be returned",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataFlowStartMessageSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "400", description = "Failed to validate request"),
                    @ApiResponse(responseCode = "200", description = "Data transfer initiated",
                            content = @Content(schema = @Schema(implementation = DataFlowResponseMessageSchema.class))),
            }
    )
    JsonObject start(JsonObject dataFlowStartMessage);

    @Operation(description = "Get the current state of a data transfer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "State of the data transfer",
                            content = @Content(schema = @Schema(implementation = DataFlowStateSchema.class))),
                    @ApiResponse(responseCode = "404", description = "Data transfer not found in the data plane")
            }
    )
    JsonObject getTransferState(String transferProcessId);

    @Operation(description = "Terminates a data transfer.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataFlowTerminateMessageSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Data transfer terminated"),
                    @ApiResponse(responseCode = "404", description = "Data transfer not handled by the data plane"),
                    @ApiResponse(responseCode = "409", description = "Cannot terminate the transfer"),
            }
    )
    void terminate(String transferProcessId, JsonObject terminationMessage);

    @Operation(description = "Suspend a data transfer.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataFlowSuspendMessageSchema.class))),
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Data transfer suspended"),
                    @ApiResponse(responseCode = "404", description = "Data transfer not handled by the data plane"),
                    @ApiResponse(responseCode = "409", description = "Cannot suspend the transfer"),
            }
    )
    @Deprecated(since = "0.15.0")
    void suspend(String transferProcessId, JsonObject suspendMessage);

    @Operation(description = "Check if data plane is available.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Data plane is available"),
            }
    )
    void checkAvailability();

    @Schema(name = "DataFlowStartMessage", example = DataFlowStartMessageSchema.DATA_FLOW_START_EXAMPLE)
    record DataFlowStartMessageSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = EDC_DATA_FLOW_START_MESSAGE_SIMPLE_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String processId,
            @Schema(requiredMode = REQUIRED)
            String datasetId,
            @Schema(requiredMode = REQUIRED)
            String participantId,
            @Schema(requiredMode = REQUIRED)
            String agreementId,
            @Schema(requiredMode = REQUIRED)
            String transferType,
            @Schema(requiredMode = REQUIRED, example = DataAddressSchema.DATA_ADDRESS_EXAMPLE)
            DataAddressSchema sourceDataAddress,
            @Schema(example = DataAddressSchema.DATA_ADDRESS_EXAMPLE)
            DataAddressSchema destinationDataAddress,
            @Schema
            String callbackAddress,
            @Schema(requiredMode = REQUIRED)
            FreeFormPropertiesSchema properties
    ) {
        public static final String DATA_FLOW_START_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "transfer-id",
                    "@type": "DataFlowStartMessage",
                    "processId": "process-id",
                    "datasetId": "dataset-id",
                    "participantId": "participant-id",
                    "agreementId": "agreement-id",
                    "transferType": "HttpData-PUSH",
                    "sourceDataAddress": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos"
                    },
                    "destinationDataAddress": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos"
                    },
                    "callbackAddress" : "http://control-plane",
                    "properties": {
                        "key": "value"
                    }
                }
                """;
    }

    @Schema(name = "Properties", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    record FreeFormPropertiesSchema() {
    }

    @Schema(name = "DataAddress", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    record DataAddressSchema(
            @Schema(name = TYPE, example = DataAddress.EDC_DATA_ADDRESS_TYPE)
            String type,
            @Schema(name = "type")
            String typeProperty
    ) {
        public static final String DATA_ADDRESS_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "type": "HttpData",
                    "baseUrl": "http://example.com"
                }
                """;
    }

    @Schema(name = "DataFlowResponseMessage", example = DataFlowResponseMessageSchema.DATA_FLOW_RESPONSE_MESSAGE_EXAMPLE)
    record DataFlowResponseMessageSchema(
            @Schema(name = CONTEXT)
            Object context,
            @Schema(name = TYPE, example = DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_SIMPLE_TYPE)
            String ldType,
            DataAddressSchema dataAddress
    ) {
        public static final String DATA_FLOW_RESPONSE_MESSAGE_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "DataFlowResponseMessage",
                    "dataAddress": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos"
                    }
                }
                """;
    }

    @Schema(name = "DataFlowState", example = DataFlowStateSchema.DATA_FLOW_STATE_EXAMPLE)
    record DataFlowStateSchema(
            @Schema(name = CONTEXT)
            Object context,
            @Schema(name = TYPE, example = DataFlowState.DATA_FLOW_STATE_SIMPLE_TYPE)
            String ldType,
            String state
    ) {
        public static final String DATA_FLOW_STATE_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "DataFlowState",
                    "state": "STARTED"
                }
                """;
    }

    @Schema(name = "DataFlowTerminateMessage", example = DataFlowTerminateMessageSchema.TERMINATE_DATA_FLOW_EXAMPLE)
    record DataFlowTerminateMessageSchema(
            @Schema(name = TYPE, example = DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_SIMPLE_TYPE)
            String ldType,
            String state
    ) {
        public static final String TERMINATE_DATA_FLOW_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "DataFlowTerminateMessage",
                    "reason": "reason"
                }
                """;
    }

    @Schema(name = "DataFlowSuspendMessage", example = DataFlowSuspendMessageSchema.TERMINATE_DATA_FLOW_EXAMPLE)
    record DataFlowSuspendMessageSchema(
            @Schema(name = TYPE, example = DataFlowSuspendMessage.DATA_FLOW_SUSPEND_MESSAGE_SIMPLE_TYPE)
            String ldType,
            String state
    ) {
        public static final String TERMINATE_DATA_FLOW_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "DataFlowSuspendMessage",
                    "reason": "reason"
                }
                """;
    }
}
