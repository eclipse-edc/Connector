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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v3.ContractDefinitionApiV3.ContractDefinitionInputSchema.CONTRACT_DEFINITION_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v3.ContractDefinitionApiV3.ContractDefinitionOutputSchema.CONTRACT_DEFINITION_OUTPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition(info = @Info(version = "v3"))
@Tag(name = "Contract Definition V3")
public interface ContractDefinitionApiV3 {

    @Operation(description = "Returns all contract definitions according to a query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract definitions matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContractDefinitionOutputSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonArray queryContractDefinitionsV3(JsonObject querySpecJson);

    @Operation(description = "Gets an contract definition with the given ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract definition",
                            content = @Content(schema = @Schema(implementation = ContractDefinitionOutputSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An contract agreement with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getContractDefinitionV3(String id);

    @Operation(description = "Creates a new contract definition",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ContractDefinitionInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "contract definition was created successfully. Returns the Contract Definition Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create contract definition, because a contract definition with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))) }
    )
    JsonObject createContractDefinitionV3(JsonObject createObject);

    @Operation(description = "Removes a contract definition with the given ID if possible. " +
            "DANGER ZONE: Note that deleting contract definitions can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contract definition was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void deleteContractDefinitionV3(String id);

    @Operation(description = "Updated a contract definition with the given ID. The supplied JSON structure must be a valid JSON-LD object",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ContractDefinitionInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contract definition was updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A contract definition with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void updateContractDefinitionV3(JsonObject updateObject);

    @Schema(name = "ContractDefinitionInput", example = CONTRACT_DEFINITION_INPUT_EXAMPLE)
    record ContractDefinitionInputSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = CONTRACT_DEFINITION_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String accessPolicyId,
            @Schema(requiredMode = REQUIRED)
            String contractPolicyId,
            @Schema(requiredMode = REQUIRED)
            List<ApiCoreSchema.CriterionSchema> assetsSelector) {

        public static final String CONTRACT_DEFINITION_INPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "definition-id",
                    "accessPolicyId": "asset-policy-id",
                    "contractPolicyId": "contract-policy-id",
                    "assetsSelector": []
                }
                """;
    }

    @Schema(name = "ContractDefinitionOutput", example = CONTRACT_DEFINITION_OUTPUT_EXAMPLE)
    record ContractDefinitionOutputSchema(
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = CONTRACT_DEFINITION_TYPE)
            String type,
            String accessPolicyId,
            String contractPolicyId,
            List<ApiCoreSchema.CriterionSchema> assetsSelector,
            long createdAt) {

        public static final String CONTRACT_DEFINITION_OUTPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "definition-id",
                    "accessPolicyId": "asset-policy-id",
                    "contractPolicyId": "contract-policy-id",
                    "assetsSelector": [],
                    "createdAt": 1688465655
                }
                """;
    }
}
