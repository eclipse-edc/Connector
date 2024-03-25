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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_KEY;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;

@OpenAPIDefinition(
        info = @Info(description = "This contains the secret management API, which allows a participant to manage the API keys associated to the API they expose.", title = "Secret API"))
@Tag(name = "Secret")
public interface SecretApi {

    @Operation(description = "Creates a new secret, as a key,value pair",
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

    @Operation(description = "Request all secrets according to a particular query",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The secrets matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SecretOutputSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    JsonArray requestSecrets(JsonObject querySpecJson);

    @Operation(description = "Gets a secret with the given ID",
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

    @Operation(description = "Removes a secret with the given ID if possible. Deleting a secret is only possible if that secret is not yet referenced " +
            "by a contract agreement, in which case an error is returned. " +
            "DANGER ZONE: Note that deleting secrets referenced by an asset may lead to unexpected behavior in the system.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Secret was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "A secret with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    // TODO: check if it makes sense to check id secret is referenced from any asset
                    @ApiResponse(responseCode = "409", description = "The secret cannot be deleted, because it is referenced by an asset",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void removeSecret(String id);

    @Operation(description = "Updates a secret with the given ID if it exists. If the secret is not found, no further action is taken. ",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = SecretInputSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Secret was updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Secret could not be updated, because it does not exist."),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
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

            @Schema(name = EDC_SECRET_KEY, requiredMode = REQUIRED)
            String key,

            @Schema(name = EDC_SECRET_VALUE, requiredMode = REQUIRED)
            String value
    ) {
        public static final String SECRET_INPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "secret-id",
                    "key": "secret-key",
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
            @Schema(name = EDC_SECRET_KEY, requiredMode = REQUIRED)
            String key,
            @Schema(name = EDC_SECRET_VALUE, requiredMode = REQUIRED)
            String value,
            long createdAt
    ) {
        // TODO: check key and value names
        public static final String SECRET_OUTPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "secret-id",
                    "@type": "https://w3id.org/edc/v0.0.1/ns/Secret",
                    "key": "secret-key",
                    "value": "secret-value",
                    "createdAt": 1688465655
                }
                """;
    }

}
