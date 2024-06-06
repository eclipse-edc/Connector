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

package org.eclipse.edc.connector.api.management.version.v1;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;

import java.util.List;
import java.util.Map;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;

@OpenAPIDefinition(
        info = @Info(description = "This contains the version API that provides information about the exact version of the management API", title = "Version API"))
@Tag(name = "Version")
public interface VersionApi {

    @Operation(description = "Gets the exact SemVer string of the Management API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The secret",
                            content = @Content(schema = @Schema(implementation = SecretOutputSchema.class)))
            }
    )
    Map<String, List<VersionRecord>> getVersion();


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
