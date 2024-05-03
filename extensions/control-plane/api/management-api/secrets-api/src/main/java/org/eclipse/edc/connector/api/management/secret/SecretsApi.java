/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;

@OpenAPIDefinition(
        info = @Info(description = "This contains the secret management API, which allows to add, remove and update secrets in the Vault.", title = "Secret API"))
@Tag(name = "Secret")
public interface SecretsApi {

    @Operation(description = "Creates a new secret.",
            operationId = "createSecret",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = SecretInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Secret was created successfully. Returns the secret Id and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Could not create secret, because a secret with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))}
    )
    JsonObject createSecret(JsonObject secret);

    @Operation(description = "Gets a secret with the given ID",
            operationId = "getSecret",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The secret",
                            content = @Content(schema = @Schema(implementation = SecretOutputSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A secret with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getSecret(String id);

    @Operation(description = "Removes a secret with the given ID if possible.",
            operationId = "removeSecret",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Secret was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A secret with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void removeSecret(String id);

    @Operation(description = "Updates a secret with the given ID if it exists. If the secret is not found, no further action is taken. ",
            operationId = "updateSecret",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = SecretInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Secret was updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "Secret could not be updated, because it does not exist.")
            })
    void updateSecret(JsonObject secret);

    @Schema(name = "SecretInput", example = SecretInputSchema.SECRET_INPUT_EXAMPLE)
    record SecretInputSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = EDC_SECRET_TYPE)
            String type,
            @Schema(name = EDC_SECRET_VALUE, requiredMode = REQUIRED)
            String value
    ) {
        public static final String SECRET_INPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "secret-id",
                    "value" : "secret-value"
                }
                """;
    }

    @ArraySchema()
    @Schema(name = "SecretOutput", example = SecretOutputSchema.SECRET_OUTPUT_EXAMPLE)
    record SecretOutputSchema(
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = EDC_SECRET_TYPE)
            String type,
            @Schema(name = EDC_SECRET_VALUE, requiredMode = REQUIRED)
            String value
    ) {
        public static final String SECRET_OUTPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "secret-id",
                    "@type": "https://w3id.org/edc/v0.0.1/ns/Secret",
                    "value": "secret-value"
                }
                """;
    }

}
