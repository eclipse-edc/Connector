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

package org.eclipse.edc.connector.api.management.contractnegotiation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationState;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Negotiation")
public interface ContractNegotiationNewApi {

    @Operation(description = "Returns all contract negotiations according to a query",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContractNegotiationDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    List<JsonObject> queryNegotiations(JsonObject querySpecDto);

    @Operation(description = "Gets a contract negotiation with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiation",
                            content = @Content(schema = @Schema(implementation = ContractNegotiationDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getNegotiation(String id);

    @Operation(description = "Gets the state of a contract negotiation with the given ID",
            operationId = "getNegotiationState",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiation's state",
                            content = @Content(schema = @Schema(implementation = NegotiationState.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getNegotiationState(String id);

    @Operation(description = "Gets a contract agreement for a contract negotiation with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreement that is attached to the negotiation, or null",
                            content = @Content(schema = @Schema(implementation = ContractAgreementDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getAgreementForNegotiation(String negotiationId);

    @Operation(description = "Initiates a contract negotiation for a given offer and with the given counter part. Please note that successfully invoking this endpoint " +
            "only means that the negotiation was initiated. Clients must poll the /{id}/state endpoint to track the state",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The negotiation was successfully initiated. Returns the contract negotiation ID and created timestamp",
                            content = @Content(schema = @Schema(implementation = IdResponseDto.class)),
                            links = @Link(name = "poll-state", operationId = "getNegotiationState", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    JsonObject initiateContractNegotiation(@Schema(implementation = NegotiationInitiateRequestDto.class) JsonObject requestDto);

    @Operation(description = "Requests aborting the contract negotiation. Due to the asynchronous nature of contract negotiations, a successful " +
            "response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request to cancel the Contract negotiation was successfully received",
                            links = @Link(name = "poll-state", operationId = "getNegotiationState")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void cancelNegotiation(String id);

    @Operation(description = "Requests cancelling the contract negotiation. Due to the asynchronous nature of contract negotiations, a successful " +
            "response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request to decline the Contract negotiation was successfully received",
                            links = @Link(name = "poll-state", operationId = "getNegotiationState")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    void declineNegotiation(String id);
}
