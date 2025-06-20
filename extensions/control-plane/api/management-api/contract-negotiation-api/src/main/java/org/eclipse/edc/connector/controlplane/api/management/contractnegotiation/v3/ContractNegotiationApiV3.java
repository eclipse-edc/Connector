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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3;

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
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.NegotiationState;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;

@OpenAPIDefinition(info = @Info(version = "v3"))
@Tag(name = "Contract Negotiation V3")
public interface ContractNegotiationApiV3 {

    @Operation(description = "Returns all contract negotiations according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiations that match the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ManagementApiSchema.ContractNegotiationSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))}
    )
    JsonArray queryNegotiationsV3(JsonObject querySpecJson);

    @Operation(description = "Gets a contract negotiation with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiation",
                            content = @Content(schema = @Schema(implementation = ManagementApiSchema.ContractNegotiationSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getNegotiationV3(String id);

    @Operation(description = "Gets the state of a contract negotiation with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract negotiation's state",
                            content = @Content(schema = @Schema(implementation = NegotiationState.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getNegotiationStateV3(String id);

    @Operation(description = "Gets a contract agreement for a contract negotiation with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreement that is attached to the negotiation, or null",
                            content = @Content(schema = @Schema(implementation = ManagementApiSchema.ContractAgreementSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getAgreementForNegotiationV3(String negotiationId);

    @Operation(description = "Initiates a contract negotiation for a given offer and with the given counter part. Please note that successfully invoking this endpoint " +
            "only means that the negotiation was initiated. Clients must poll the /{id}/state endpoint to track the state",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ContractRequestSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The negotiation was successfully initiated. Returns the contract negotiation ID and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class)),
                            links = @Link(name = "poll-state", operationId = "getNegotiationStateV3", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
            })
    JsonObject initiateContractNegotiationV3(JsonObject requestDto);

    @Operation(description = "Terminates the contract negotiation.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TerminateNegotiationSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "ContractNegotiation is terminating",
                            links = @Link(name = "poll-state", operationId = "getNegotiationStateV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void terminateNegotiationV3(String id, JsonObject terminateNegotiation);

    @Operation(description = "Deletes the contract negotiation with the given ID. Only terminated negotiations without agreement will be deleted",
            responses = {
                    @ApiResponse(responseCode = "204", description = "ContractNegotiation is deleted",
                            links = @Link(name = "poll-state", operationId = "getNegotiationStateV3")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract negotiation with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "The given contract negotiation cannot be deleted due to a wrong state or has existing contract agreement",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void deleteNegotiationV3(String id);

    @Schema(name = "ContractRequest", example = ContractRequestSchema.CONTRACT_REQUEST_EXAMPLE)
    record ContractRequestSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, example = CONTRACT_REQUEST_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String protocol,
            @Schema(requiredMode = REQUIRED)
            String counterPartyAddress,
            @Schema(requiredMode = REQUIRED)
            OfferSchema policy,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses) {

        // policy example took from https://w3c.github.io/odrl/bp/
        public static final String CONTRACT_REQUEST_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/ContractRequest",
                    "counterPartyAddress": "http://provider-address",
                    "protocol": "dataspace-protocol-http",
                    "policy": {
                        "@context": "http://www.w3.org/ns/odrl.jsonld",
                        "@type": "odrl:Offer",
                        "@id": "offer-id",
                        "assigner": "providerId",
                        "permission": [],
                        "prohibition": [],
                        "obligation": [],
                        "target": "assetId"
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

    @Schema(name = "Offer", description = "ODRL offer", example = OfferSchema.OFFER_EXAMPLE)
    record OfferSchema(
            @Schema(name = TYPE, example = ODRL_POLICY_TYPE_OFFER)
            String type,
            @Schema(name = ID, requiredMode = REQUIRED)
            String id,
            @Schema(requiredMode = REQUIRED)
            String assigner,
            @Schema(requiredMode = REQUIRED)
            String target
    ) {
        public static final String OFFER_EXAMPLE = """
                {
                    "@context": "http://www.w3.org/ns/odrl.jsonld",
                    "@type": "odrl:Offer",
                    "@id": "offer-id",
                    "assigner": "providerId",
                    "target": "assetId",
                    "permission": [],
                    "prohibition": [],
                    "obligation": []
                }
                """;
    }

    @Schema(example = TerminateNegotiationSchema.TERMINATE_NEGOTIATION_EXAMPLE)
    record TerminateNegotiationSchema(
            @Schema(name = TYPE, example = TERMINATE_NEGOTIATION_TYPE)
            String ldType,
            @Schema(name = ID)
            String id,
            String reason
    ) {
        public static final String TERMINATE_NEGOTIATION_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TerminateNegotiation",
                    "@id": "negotiation-id",
                    "reason": "a reason to terminate"
                }
                """;
    }

}
