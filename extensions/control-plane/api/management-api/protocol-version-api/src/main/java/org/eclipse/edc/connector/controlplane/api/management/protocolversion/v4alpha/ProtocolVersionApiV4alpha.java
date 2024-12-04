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

package org.eclipse.edc.connector.controlplane.api.management.protocolversion.v4alpha;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition(info = @Info(version = "v4alpha"))
@Tag(name = "Protocol Version v4alpha")
public interface ProtocolVersionApiV4alpha {

    @Operation(
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ProtocolVersionRequestSchema.class))),
            responses = { @ApiResponse(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProtocolVersionSchema.class)
                    ),
                    description = "Gets supported protocol versions of a single connector") }
    )
    void requestProtocolVersionV4alpha(JsonObject request, @Suspended AsyncResponse response);

    @Schema(name = "CatalogRequest", example = ProtocolVersionRequestSchema.PROTOCOL_VERSION_REQUEST_EXAMPLE)
    record ProtocolVersionRequestSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, example = CATALOG_REQUEST_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String counterPartyAddress,
            @Schema(requiredMode = REQUIRED)
            String counterPartyId,
            @Schema(requiredMode = REQUIRED)
            String protocol) {

        public static final String PROTOCOL_VERSION_REQUEST_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "ProtocolVersionRequest",
                    "counterPartyAddress": "http://provider-address",
                    "counterPartyId": "providerId",
                    "protocol": "dataspace-protocol-http"
                }
                """;
    }

    @Schema(name = "Protocol Version", description = "Protocol Version", example = ProtocolVersionSchema.PROTOCOL_VERSION_EXAMPLE)
    record ProtocolVersionSchema(
    ) {
        public static final String PROTOCOL_VERSION_EXAMPLE = """
                {
                     "protocolVersions": [
                         {
                             "version": "2024/1",
                             "path": "/2024/1"
                         },
                         {
                             "version": "v0.8",
                             "path": "/"
                         }
                     ]
                 }
                """;
    }

}
